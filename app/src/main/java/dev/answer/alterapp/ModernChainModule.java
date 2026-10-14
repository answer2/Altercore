package dev.answer.alterapp;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * A libxposed API 101 module that lives inside this app, so {@link TestModernXposed} can drive
 * AlterCore's modern loading path end to end without needing a second APK.
 *
 * <p>The hookers installed here are what the chain tests assert against; see
 * {@link ChainTarget} for the targets.</p>
 */
public class ModernChainModule extends XposedModule {

    private static final String TAG = "ModernChainModule";
    private static final String TARGET_CLASS = "dev.answer.alterapp.ChainTarget";

    /** Set from {@code onModuleLoaded}, which is the first callback the framework delivers. */
    public static volatile String processName;

    /** Every framework call the module wants the test to look at, in order. */
    public static final List<String> trace = new ArrayList<>();

    /** Makes the {@code PROTECTIVE} hooker fault before it proceeds. */
    public static boolean throwInProtective;

    /** Makes the {@code PASSTHROUGH} hooker fault before it proceeds. */
    public static boolean throwInPassthrough;

    private static volatile ModernChainModule self;
    private static final List<XposedInterface.HookHandle> handles = new ArrayList<>();
    private static int replaceSlot = -1;
    private static volatile ClassLoader lastClassLoader;

    /** Reload observations, read by the test suite. */
    public static volatile int reloadsSeen;
    public static volatile String carriedState;
    public static volatile int oldHandlesAtReload;
    public static volatile String frozenProbe;
    public static volatile String extrasSeen;

    public ModernChainModule() {
    }

    public static ModernChainModule getInstance() {
        return self;
    }

    public static void clearTrace() {
        synchronized (trace) {
            trace.clear();
        }
    }

    public static void record(String entry) {
        synchronized (trace) {
            trace.add(entry);
        }
    }

    static String describe() {
        synchronized (trace) {
            return String.join(",", new ArrayList<>(trace));
        }
    }

    static int countTrace(String prefix) {
        synchronized (trace) {
            int count = 0;
            for (String entry : trace) {
                if (entry.startsWith(prefix)) {
                    count++;
                }
            }
            return count;
        }
    }

    /** Removes every hook this module installed; the next {@code onPackageReady} re-installs them. */
    public static synchronized void uninstallAll() {
        for (XposedInterface.HookHandle handle : handles) {
            handle.unhook();
        }
        handles.clear();
    }

    public static synchronized boolean isInstalled() {
        return !handles.isEmpty();
    }

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        self = this;
        processName = param.getProcessName();
        log("onModuleLoaded process=" + param.getProcessName()
                + " framework=" + getFrameworkName() + "/" + getFrameworkVersion()
                + " api=" + getApiVersion());
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!"dev.answer.alterapp".equals(param.getPackageName())) {
            return;
        }
        // Saved because package callbacks are not replayed after a hot reload: the new generation
        // reinstalls its hooks from onHotReloaded instead.
        lastClassLoader = param.getClassLoader();
        installHooks(param.getClassLoader());
    }

    @Override
    public boolean onHotReloading(XposedModuleInterface.HotReloadingParam param) {
        // This callback runs in the outgoing code, which the framework has already frozen: a hook
        // registration attempt here must fail.
        try {
            hook(ChainTarget.class.getDeclaredMethod("identified", String.class))
                    .intercept(chain -> "must-be-refused");
            frozenProbe = "accepted";
        } catch (IllegalStateException expected) {
            frozenProbe = "IllegalStateException";
        } catch (NoSuchMethodException e) {
            frozenProbe = "NoSuchMethodException";
        }
        param.setSavedInstanceState("carried");
        return true;
    }

    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        reloadsSeen++;
        carriedState = String.valueOf(param.getSavedInstanceState());
        extrasSeen = param.getExtras() == null ? "null" : String.valueOf(param.getExtras().get("token"));
        oldHandlesAtReload = param.getOldHookHandles().size();
        // The interface default unhooks all of them; doing it explicitly keeps the intent visible.
        for (XposedInterface.HookHandle old : param.getOldHookHandles()) {
            old.unhook();
        }
        handles.clear();
        replaceSlot = -1;
        if (lastClassLoader != null) {
            installHooks(lastClassLoader);
        }
    }

    private synchronized void installHooks(ClassLoader classLoader) {
        if (!handles.isEmpty()) {
            return;
        }
        try {
            Class<?> target = classLoader.loadClass(TARGET_CLASS);

            Method shout = target.getDeclaredMethod("shout", String.class);
            // Outer link wraps the result, inner link rewrites the arguments. Running both proves
            // the nesting order, the priority order and proceed(Object[]) in a single call.
            handles.add(hook(shout).setPriority(XposedInterface.PRIORITY_HIGHEST)
                    .intercept(chain -> {
                        record("outer:enter");
                        Object result = chain.proceed();
                        record("outer:exit");
                        return result + "[outer]";
                    }));
            handles.add(hook(shout).setPriority(XposedInterface.PRIORITY_LOWEST)
                    .intercept(chain -> {
                        record("inner:enter");
                        Object result = chain.proceed(
                                new Object[] { chain.getArg(0) + "!" });
                        record("inner:exit");
                        return result;
                    }));

            Method add = target.getDeclaredMethod("add", int.class, int.class);
            handles.add(hook(add).intercept(chain -> {
                Object result = chain.proceed();
                return (Integer) result * 2;
            }));

            Method blocked = target.getDeclaredMethod("blocked");
            handles.add(hook(blocked).intercept(chain -> "replaced"));

            Method mutate = target.getDeclaredMethod("mutate", StringBuilder.class);
            handles.add(hook(mutate).intercept(chain -> {
                chain.proceed();
                ((StringBuilder) chain.getArg(0)).append("[after]");
                return null;
            }));

            Method boom = target.getDeclaredMethod("boom");
            handles.add(hook(boom).intercept(chain -> {
                try {
                    return chain.proceed();
                } catch (Throwable t) {
                    record("boom:caught:" + t.getClass().getSimpleName() + ":" + t.getMessage());
                    throw t;
                }
            }));

            Method protective = target.getDeclaredMethod("protective", String.class);
            handles.add(hook(protective).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (throwInProtective) {
                            throw new IllegalStateException("hooker-fault");
                        }
                        return chain.proceed();
                    }));

            Method passthrough = target.getDeclaredMethod("passthrough", String.class);
            handles.add(hook(passthrough)
                    .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                    .intercept(chain -> {
                        if (throwInPassthrough) {
                            throw new IllegalStateException("hooker-fault");
                        }
                        return chain.proceed();
                    }));

            // A constructor hooker that proceeds: the body must have run before its continuation.
            Constructor<?> tagged = target.getDeclaredConstructor(String.class);
            handles.add(hook(tagged).intercept(chain -> {
                record("ctor:pre");
                chain.proceed();
                ((ChainTarget) chain.getThisObject()).label += "[inited]";
                return null;
            }));

            // A constructor hooker that does not proceed: the object stays uninitialized.
            Constructor<?> marker = target.getDeclaredConstructor(int.class);
            handles.add(hook(marker).intercept(chain -> null));

            // API 102: a second hook with the same id replaces the first one atomically.
            Method identified = target.getDeclaredMethod("identified", String.class);
            handles.add(hook(identified).setId("shared-id").intercept(chain -> "first"));
            handles.add(hook(identified).setId("shared-id").intercept(chain -> "second"));

            // API 102: replaceHook keeps this hook's configuration and invalidates the old handle.
            Method replaced = target.getDeclaredMethod("replaced", String.class);
            replaceSlot = handles.size();
            handles.add(hook(replaced).intercept(chain -> "before-replace"));

            log("installed " + handles.size() + " chain hooks on " + TARGET_CLASS);
        } catch (Throwable t) {
            log("failed to install chain hooks: " + t);
        }
    }

    private void log(String message) {
        Log.i(TAG, message);
    }

    /** {@code shout} through the given invoker type, used by the call-original test. */
    public Object callShout(String message) throws Throwable {
        Class<?> target = ChainTarget.class;
        Method shout = target.getDeclaredMethod("shout", String.class);
        return getInvoker(shout).invoke(null, message);
    }

    public Object callShoutOrigin(String message) throws Throwable {
        Method shout = ChainTarget.class.getDeclaredMethod("shout", String.class);
        return getInvoker(shout).setType(XposedInterface.Invoker.Type.ORIGIN)
                .invoke(null, message);
    }

    /**
     * Runs the {@code setId} scenario: two builders shared one id, so only the second hooker may
     * be live; without any hook installed the original returns.
     */
    public static String exerciseHookId() {
        return ChainTarget.identified("x");
    }

    /**
     * Runs the {@code replaceHook} scenario and returns
     * {@code before|afterReplacement|oldHandleReuse|stillLive|newId}.
     */
    public static synchronized String exerciseReplaceHook() {
        if (!isInstalled() || replaceSlot < 0 || replaceSlot >= handles.size()) {
            return ChainTarget.replaced("x");
        }
        String before = ChainTarget.replaced("x");
        XposedInterface.HookHandle old = handles.get(replaceSlot);
        XposedInterface.HookHandle fresh = old.replaceHook(chain -> "after-replace");
        handles.set(replaceSlot, fresh);
        String after = ChainTarget.replaced("x");

        String oldReuse;
        try {
            old.replaceHook(chain -> "must-not-happen");
            oldReuse = "accepted";
        } catch (IllegalStateException expected) {
            oldReuse = "IllegalStateException";
        }
        old.unhook(); // idempotent: the replaced handle must not tear down the live hook
        String stillLive = ChainTarget.replaced("x");
        return before + "|" + after + "|" + oldReuse + "|" + stillLive + "|" + fresh.getId();
    }
}
