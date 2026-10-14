package dev.answer.altercore.xposed.modern;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XCallback;

import dev.answer.altercore.AlterCore;
import dev.answer.altercore.xposed.AlterXposed;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.error.HookFailedError;

/**
 * Interceptor-chain dispatcher backing the modern libxposed {@code hook()} API.
 *
 * <p>All hookers for one {@link Executable} live in a single {@link ChainState} driven by one
 * internal {@link XC_MethodHook} carrier installed through {@link XposedBridge#hookMethod}, so a
 * member never gets two engine hooks when legacy and modern modules coexist; the whole chain sits
 * in the lowest legacy priority slot and therefore runs after legacy callbacks. Constructors behave
 * like methods: the chain calls {@code <init>} through the target's backup, so a hooker that does
 * not proceed leaves the object uninitialized (see {@link Carrier#allowsConstructorSkip()}).
 */
public final class HookRegistry {
    private static final String TAG = "AlterCore/Chain";
    private static final Link[] EMPTY_LINKS = new Link[0];

    private static final Map<Executable, ChainState> sChains = new HashMap<>();

    private HookRegistry() {
    }

    static XposedInterface.HookHandle hook(ModernModule owner, Executable origin, int priority,
                                            XposedInterface.ExceptionMode mode, String id,
                                            int generation, XposedInterface.Hooker hooker) {
        if (origin == null) {
            throw new IllegalArgumentException("origin == null");
        }
        if (hooker == null) {
            throw new IllegalArgumentException("hooker == null");
        }
        if (owner.rejectsRegistration(generation)) {
            throw new IllegalStateException("Module generation " + generation
                    + " can no longer register hooks");
        }
        if (!(origin instanceof Method) && !(origin instanceof Constructor)) {
            throw new IllegalArgumentException("Only methods and constructors can be hooked: " + origin);
        }
        if (Modifier.isAbstract(origin.getModifiers())) {
            throw new IllegalArgumentException("Cannot hook abstract methods: " + origin);
        }

        ChainState state;
        synchronized (sChains) {
            state = sChains.get(origin);
            if (state == null) {
                state = new ChainState(origin);
                sChains.put(origin, state);
            }
        }

        try {
            origin.setAccessible(true);
            XposedBridge.hookMethod(origin, state.carrier);
        } catch (Throwable t) {
            synchronized (sChains) {
                if (state.links.isEmpty()) {
                    sChains.remove(origin);
                }
            }
            throw new HookFailedError("Failed to hook " + origin, t);
        }

        return state.insert(owner, priority, mode, id, generation, hooker);
    }

    /**
     * The handles a module's given generation currently holds, used to hand the outgoing
     * generation's hooks to the new one during a hot reload.
     */
    static List<XposedInterface.HookHandle> handlesOf(ModernModule owner, int generation) {
        List<XposedInterface.HookHandle> handles = new ArrayList<>();
        synchronized (sChains) {
            for (ChainState state : sChains.values()) {
                for (Link link : state.links) {
                    if (link.owner == owner && link.generation == generation && link.valid) {
                        handles.add(link);
                    }
                }
            }
        }
        return handles;
    }

    static void unhook(Executable origin, Link link) {
        ChainState state;
        boolean gone;
        synchronized (sChains) {
            state = sChains.get(origin);
            if (state == null) {
                link.valid = false;
                return;
            }
            if (link.valid) {
                state.removeLink(link);
            }
            link.valid = false;
            gone = state.links.isEmpty();
            if (gone) {
                sChains.remove(origin);
            }
        }
        if (gone) {
            XposedBridge.unhookMethod(origin, state.carrier);
        }
    }

    /**
     * Invokes {@code origin} honouring an {@link XposedInterface.Invoker.Type}: {@code ORIGIN}
     * calls the un-hooked implementation, {@code Chain} runs the registered hookers whose
     * priority does not exceed {@code maxPriority}.
     */
    static Object callByInvokerType(Executable origin, XposedInterface.Invoker.Type type,
                                    Object thisObject, Object[] args) throws Throwable {
        if (type instanceof XposedInterface.Invoker.Type.Origin) {
            return AlterCore.invokeOriginalMethod(origin, thisObject, args);
        }
        int maxPriority = ((XposedInterface.Invoker.Type.Chain) type).maxPriority();
        return invokeThroughChain(origin, maxPriority, thisObject, args);
    }

    /**
     * Runs {@code origin} through the registered hookers whose priority does not exceed
     * {@code maxPriority}, which is what {@link XposedInterface.Invoker.Type.Chain} means.
     */
    static Object invokeThroughChain(Executable origin, int maxPriority, Object thisObject,
                                     Object[] args) throws Throwable {
        ChainState state;
        synchronized (sChains) {
            state = sChains.get(origin);
        }
        Object[] callArgs = args == null ? AlterCore.EMPTY_OBJECT_ARRAY : args;
        if (state == null) {
            return invokeOrigin(origin, new CallFrame(thisObject, callArgs));
        }
        return runLinks(state, state.linksUpTo(maxPriority), 0, new CallFrame(thisObject, callArgs));
    }

    private static Object invokeOrigin(Executable origin, CallFrame frame) throws Throwable {
        try {
            return AlterCore.invokeOriginalMethod(origin, frame.thisObject, frame.args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw cause != null ? cause : e;
        }
    }

    static Object runLinks(ChainState state, Link[] links, int index, CallFrame frame)
            throws Throwable {
        if (index >= links.length) {
            return invokeOrigin(state.origin, frame);
        }

        Link link = links[index];
        ChainImpl chain = new ChainImpl(state, links, index, frame);
        try {
            return link.hooker.intercept(chain);
        } catch (Throwable thrown) {
            if (link.exceptionMode == XposedInterface.ExceptionMode.PASSTHROUGH) {
                throw thrown;
            }
            if (chain.proceeded) {
                // The hooker already saw the rest of the chain run: report what the call
                // actually produced instead of whatever the hooker did on the way out.
                if (chain.proceedThrowable != null) {
                    throw chain.proceedThrowable;
                }
                return chain.proceedResult;
            }
            Log.e(TAG, "Hooker " + link.hooker + " threw before proceeding, skipping it", thrown);
            return runLinks(state, links, index + 1, frame);
        }
    }

    /** One registered hooker plus its configuration; also the handle modules hold. */
    static final class Link implements XposedInterface.HookHandle {
        final ChainState state;
        final ModernModule owner;
        final int generation;
        final int priority;
        final XposedInterface.ExceptionMode exceptionMode;
        final String id;
        final XposedInterface.Hooker hooker;
        volatile boolean valid = true;

        Link(ChainState state, ModernModule owner, int generation, int priority,
             XposedInterface.ExceptionMode exceptionMode, String id,
             XposedInterface.Hooker hooker) {
            this.state = state;
            this.owner = owner;
            this.generation = generation;
            this.priority = priority;
            this.exceptionMode = exceptionMode;
            this.id = id;
            this.hooker = hooker;
        }

        @Override
        public Executable getExecutable() {
            return state.origin;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void unhook() {
            if (!valid) {
                return;
            }
            HookRegistry.unhook(state.origin, this);
        }

        @Override
        public XposedInterface.HookHandle replaceHook(XposedInterface.Hooker hooker) {
            if (hooker == null) {
                throw new IllegalArgumentException("hooker == null");
            }
            if (!valid) {
                throw new IllegalStateException("Hook handle is no longer valid");
            }
            return state.replace(this, hooker);
        }
    }

    static final class ChainState {
        final Executable origin;
        final Carrier carrier;
        final List<Link> links = new ArrayList<>();

        private volatile Link[] snapshot = EMPTY_LINKS;

        ChainState(Executable origin) {
            this.origin = origin;
            this.carrier = new Carrier(this);
        }

        boolean isStatic() {
            return Modifier.isStatic(origin.getModifiers());
        }

        /**
         * Registers a hooker. A non-null {@code id} replaces, in place, the module's previous hook
         * with the same id on this executable; hook ids never cross module boundaries.
         */
        Link insert(ModernModule owner, int priority, XposedInterface.ExceptionMode mode,
                    String id, int generation, XposedInterface.Hooker hooker) {
            synchronized (sChains) {
                Link link = new Link(this, owner, generation, priority, mode, id, hooker);
                Link previous = id == null ? null : findById(owner, id);
                if (previous != null) {
                    links.set(links.indexOf(previous), link);
                    previous.valid = false;
                } else {
                    links.add(positionFor(priority), link);
                }
                publish();
                return link;
            }
        }

        /** Swaps a hooker in place, keeping this hook's configuration and id. */
        Link replace(Link old, XposedInterface.Hooker hooker) {
            synchronized (sChains) {
                int at = links.indexOf(old);
                if (at < 0) {
                    throw new IllegalStateException("Hook handle is no longer valid");
                }
                // Replacement is performed by live code, so the new link belongs to the module's
                // current generation rather than to the generation that installed it first;
                // otherwise it would never be handed to a later reload.
                Link link = new Link(this, old.owner, old.owner.currentGeneration, old.priority,
                        old.exceptionMode, old.id, hooker);
                links.set(at, link);
                old.valid = false;
                publish();
                return link;
            }
        }

        /** Where a link of this priority belongs: highest first, ties keep registration order. */
        private int positionFor(int priority) {
            for (int i = 0; i < links.size(); i++) {
                if (links.get(i).priority < priority) {
                    return i;
                }
            }
            return links.size();
        }

        private Link findById(ModernModule owner, String id) {
            for (Link link : links) {
                if (link.owner == owner && id.equals(link.id)) {
                    return link;
                }
            }
            return null;
        }

        void removeLink(Link link) {
            synchronized (sChains) {
                if (links.remove(link)) {
                    publish();
                }
            }
        }

        private void publish() {
            snapshot = links.toArray(EMPTY_LINKS);
        }

        Link[] linksUpTo(int maxPriority) {
            List<Link> kept = new ArrayList<>();
            for (Link link : snapshot) {
                if (link.priority <= maxPriority) {
                    kept.add(link);
                }
            }
            return kept.toArray(EMPTY_LINKS);
        }
    }

    /** Bridges the AlterCore before/after callback into a full interceptor chain run. */
    private static final class Carrier extends XC_MethodHook {
        private final ChainState state;

        Carrier(ChainState state) {
            super(XCallback.PRIORITY_LOWEST);
            this.state = state;
        }

        /**
         * The chain calls the original itself, so a hooker that reached the end of the chain, or
         * deliberately skipped it, must be the one that decides whether the target runs.
         */
        @Override
        public boolean allowsConstructorSkip() {
            return true;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (AlterXposed.disableHooks) {
                return;
            }
            Link[] links = state.snapshot;
            if (links.length == 0) {
                return;
            }

            CallFrame frame = new CallFrame(param.thisObject, param.args);
            Object result;
            try {
                result = runLinks(state, links, 0, frame);
            } catch (Throwable t) {
                param.thisObject = frame.thisObject;
                param.args = frame.args;
                param.setThrowable(t);
                return;
            }
            param.thisObject = frame.thisObject;
            param.args = frame.args;
            // The chain already invoked the original, so the engine must not invoke it again.
            param.setResult(result);
        }
    }

    /** The mutable call state shared by every link of one invocation. */
    static final class CallFrame {
        Object thisObject;
        Object[] args;

        CallFrame(Object thisObject, Object[] args) {
            this.thisObject = thisObject;
            this.args = args == null ? AlterCore.EMPTY_OBJECT_ARRAY : args;
        }
    }

    static final class ChainImpl implements XposedInterface.Chain {
        private final ChainState state;
        private final Link[] links;
        private final int index;
        private final CallFrame frame;

        boolean proceeded;
        Object proceedResult;
        Throwable proceedThrowable;

        ChainImpl(ChainState state, Link[] links, int index, CallFrame frame) {
            this.state = state;
            this.links = links;
            this.index = index;
            this.frame = frame;
        }

        @Override
        public Executable getExecutable() {
            return state.origin;
        }

        @Override
        public Object getThisObject() {
            return frame.thisObject;
        }

        @Override
        public List<Object> getArgs() {
            return Collections.unmodifiableList(Arrays.asList(frame.args));
        }

        @Override
        public Object getArg(int argIndex) {
            if (argIndex < 0 || argIndex >= frame.args.length) {
                throw new IndexOutOfBoundsException("Index " + argIndex
                        + " out of bounds for length " + frame.args.length);
            }
            return frame.args[argIndex];
        }

        @Override
        public Object proceed() throws Throwable {
            return proceed(frame.args);
        }

        @Override
        public Object proceed(Object[] args) throws Throwable {
            Objects.requireNonNull(args, "args");
            checkArgs(args);
            return doProceed(frame.thisObject, args);
        }

        @Override
        public Object proceedWith(Object thisObject) throws Throwable {
            return proceedWith(thisObject, frame.args);
        }

        @Override
        public Object proceedWith(Object thisObject, Object[] args) throws Throwable {
            Objects.requireNonNull(thisObject, "thisObject");
            Objects.requireNonNull(args, "args");
            checkArgs(args);
            if (state.isStatic()) {
                throw new IllegalArgumentException("Cannot replace the this object of a static method");
            }
            if (!state.origin.getDeclaringClass().isInstance(thisObject)) {
                throw new IllegalArgumentException(thisObject.getClass().getName()
                        + " is not an instance of " + state.origin.getDeclaringClass().getName());
            }
            return doProceed(thisObject, args);
        }

        private void checkArgs(Object[] args) {
            int expected = state.origin.getParameterCount();
            if (args.length != expected) {
                throw new IllegalArgumentException("Expected " + expected
                        + " arguments but got " + args.length + " for " + state.origin);
            }
        }

        private Object doProceed(Object newThis, Object[] newArgs) throws Throwable {
            proceeded = true;
            frame.thisObject = newThis;
            frame.args = newArgs;
            try {
                proceedResult = runLinks(state, links, index + 1, frame);
            } catch (Throwable t) {
                proceedThrowable = t;
                throw t;
            }
            return proceedResult;
        }
    }
}
