/*
 * Copyright (C) 2026 AnswerDev
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by AnswerDev
 */
package dev.answer.altercore;

import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.unsafe.Reflection.getArtMethod;
import static dev.answer.altercore.AlterConfig.isDebug;
import static dev.answer.altercore.utils._Utils.rawMethodTypeOf;

import android.util.Log;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.MethodId;
import com.v7878.dex.immutable.ProtoId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.Reflection;
import com.v7878.unsafe.invoke.EmulatedStackFrame;

import dev.answer.altercore.callback.AfterCallback;
import dev.answer.altercore.callback.BeforeCallback;
import dev.answer.altercore.callback.MethodReplacement;
import dev.answer.altercore.callback.ReplaceCallback;
import dev.answer.altercore.core.HookTransformer;
import dev.answer.altercore.core.Hooks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.answer.altercore.callback.MethodHook;
import dev.answer.altercore.core.HookParams;
import dev.answer.altercore.core.HookRecord;

public class AlterCore {
    private static final String TAG = "AlterCore";
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private static final Map<Long, HookRecord> sHookRecords = new ConcurrentHashMap<>();
    private static final Object sHookLock = new Object();

    // Maps to store method information, hooks, and replacements
    private static final Map<Member, HookData> methodMaps = new HashMap<>();
    private static final Map<Member, HookCallback> hookMaps = new HashMap<>();
    private static final Map<Member, ReplacementCallback> replaceMaps = new HashMap<>();
    private static final Map<Member, MethodHook.Unhook> unhookMaps = new HashMap<>();
    public static final ReplaceCallback DO_NOTHING = params -> null;


    /**
     * Adds an after hook to the specified method.
     *
     * @param member   The method to hook.
     * @param callback The callback to execute after the method is called.
     */
    public static void after(Member member, AfterCallback callback) {
        if (member == null) {
            Log.e(TAG, "member can't be null");
            return;
        }

        HookData info = methodMaps.computeIfAbsent(member, k -> new HookData());
        if (info.replace == null) {
            info.setAfter(callback);
            if (!hookMaps.containsKey(member)) {
                HookCallback hook = new HookCallback(member);
                hookMaps.put(member, hook);
                unhookMaps.put(member, hook((Executable) member, hook));
            }
        }
    }

    /**
     * Adds a before hook to the specified method.
     *
     * @param member   The method to hook.
     * @param callback The callback to execute before the method is called.
     */
    public static void before(Member member, BeforeCallback callback) {
        if (member == null) {
            Log.e(TAG, "member can't be null");
            return;
        }

        HookData info = methodMaps.computeIfAbsent(member, k -> new HookData());
        if (info.replace == null) {
            info.setBefore(callback);
            if (!hookMaps.containsKey(member)) {
                HookCallback hook = new HookCallback(member);
                hookMaps.put(member, hook);
                unhookMaps.put(member, hook((Executable) member, hook));
            }
        }
    }

    /**
     * Replaces the specified method with the provided callback.
     *
     * @param member   The method to replace.
     * @param callback The callback that replaces the method.
     */
    public static void replace(Member member, ReplaceCallback callback) {
        if (member == null) {
            Log.e(TAG, "member can't be null");
            return;
        }

        HookData info = new HookData().setReplace(callback);
        methodMaps.put(member, info);
        if (!replaceMaps.containsKey(member)) {
            ReplacementCallback replace = new ReplacementCallback(member);
            replaceMaps.put(member, replace);
            unhookMaps.put(member, hook((Executable) member, replace));
        }
    }

    /**
     * Replaces the implementation of the provided `member` with a "do nothing" method.
     *
     * <p>This method is primarily used when you want to neutralize a method call by replacing it with
     * a no-op (a method that does nothing). If the `member` is not already in `replaceMaps`, a new
     * `ReplacementCallback` is created and hooked. This callback is stored in `replaceMaps` to ensure
     * the replacement only happens once for each member.
     *
     * <p>The reason for creating a `ReplacementCallback` instead of using a simple placeholder is to
     * facilitate the ability to unhook the replacement later. This allows for restoring the original
     * method implementation if needed.
     *
     * @param member The method or constructor to be replaced with a "do nothing" implementation.
     */
    public static void doNothing(Member member) {
        if (member == null) {
            Log.e(TAG, "member can't be null");
            return;
        }

        HookData info = new HookData().setReplace(DO_NOTHING);
        methodMaps.put(member, info);
        if (!replaceMaps.containsKey(member)) {
            ReplacementCallback replace = new ReplacementCallback(member);
            replaceMaps.put(member, replace);
            unhookMaps.put(member, hook((Executable) member, replace));
        }
    }

    /**
     * Unhooks a method by removing its associated hook from the map and calling the unhook method.
     *
     * @param member The method or constructor to unhook.
     */
    public static void unHook(Member member) {
        if (unhookMaps.containsKey(member)) {
            unhookMaps.get(member).unhook();
            unhookMaps.remove(member);
        }
    }


    /**
     * Installs a hook on the given executable (method or constructor).
     *
     * @param executable the target method or constructor to hook
     * @param callback   the callback that will be invoked before and after the original call
     * @return an {@link MethodHook.Unhook} object that can be used to remove the hook,
     * or {@code null} if an error occurred
     */
    public static MethodHook.Unhook hook(Executable executable, MethodHook callback) {
        try {
            if (isDebug) {
                Log.d(TAG, "Hooking method " + executable + " with callback " + callback);
            }

            if (executable == null) {
                throw new NullPointerException("method == null");
            }
            if (callback == null) {
                throw new NullPointerException("callback == null");
            }

            int modifiers = executable.getModifiers();
            boolean isStatic = Modifier.isStatic(modifiers);
            boolean isConstructor = executable instanceof Constructor;

            // Return type: always void for constructors, the real return type for methods.
            Class<?> returnType = isConstructor ? void.class : ((Method) executable).getReturnType();

            if (executable instanceof Method) {
                if (Modifier.isAbstract(modifiers)) {
                    throw new IllegalArgumentException("Cannot hook abstract methods: " + executable);
                }
                ((Method) executable).setAccessible(true);
            } else if (executable instanceof Constructor) {
                if (isStatic) {
                    throw new IllegalArgumentException("Cannot hook class initializer: " + executable);
                }
                ((Constructor<?>) executable).setAccessible(true);
            } else {
                throw new IllegalArgumentException("Only methods and constructors can be hooked: " + executable);
            }

            Method backupMethod = buildBackup(executable);
            Hooks.hook(backupMethod, executable, Hooks.EntryPointType.DIRECT);

            long artMethod = getArtMethod(executable);
            HookRecord hookRecord;
            boolean newMethod = false;

            // Set isStatic / backup while still holding the lock, so no thread can ever
            // observe a partially-initialized HookRecord through sHookRecords.
            synchronized (sHookLock) {
                hookRecord = sHookRecords.get(artMethod);
                if (hookRecord == null) {
                    newMethod = true;
                    hookRecord = new HookRecord(executable, artMethod);
                    hookRecord.isStatic = isStatic;
                    hookRecord.backup = backupMethod;
                    sHookRecords.put(artMethod, hookRecord);
                } else {
                    // Update the existing record inside the lock too, to avoid racing
                    // with concurrent readers of sHookRecords.
                    hookRecord.isStatic = isStatic;
                    hookRecord.backup = backupMethod;
                }
            }

            HookRecord finalHookRecord = hookRecord;
            HookTransformer hookTransformer = new HookTransformer() {
                @Override
                public void transform(MethodHandle original, EmulatedStackFrame frame) throws Throwable {
                    EmulatedStackFrame.StackFrameAccessor accessor = frame.accessor();

                    // Walk original.type()'s parameters and read each one with the accessor method
                    // matching its actual type, instead of slicing frame.references() (which silently
                    // drops primitive parameters — see readArgumentAt()).
                    Class<?>[] paramTypes = original.type().parameterArray();
                    int paramCount = paramTypes.length;
                    int argsStart = isStatic ? 0 : 1;

                    Object thisObject = isStatic ? null : readArgumentAt(accessor, paramTypes[0], 0);

                    Object[] argsData = paramCount == argsStart
                            ? EMPTY_OBJECT_ARRAY
                            : new Object[paramCount - argsStart];
                    for (int i = argsStart; i < paramCount; i++) {
                        argsData[i - argsStart] = readArgumentAt(accessor, paramTypes[i], i);
                    }

                    HookParams params = new HookParams(finalHookRecord, thisObject, argsData);

                    try {
                        callback.before(params);
                    } catch (Throwable e) {
                        Log.e(TAG, "Unexpected exception occurred when calling "
                                + callback.getClass().getName() + ".beforeCall()", e);
                        params.resetResult();
                        params.setThrowable(e);
                    }

                    if (isConstructor && params.returnEarly) {
                        Log.w(TAG, "returnEarly is not supported for constructors, ignoring: " + executable);
                        params.returnEarly = false;
                    }

                    Object[] methodArgs = isStatic ? params.args : addFirst(params.args, params.thisObject);

                    Object originalResult = null;
                    if (!params.returnEarly) {
                        try {
                            originalResult = original.invokeWithArguments(methodArgs);
                            params.setResult(originalResult);
                        } catch (Throwable e) {
                            params.setThrowable(e);
                        }
                    }

                    try {
                        callback.after(params);
                    } catch (Throwable e) {
                        Log.e(TAG, "Unexpected exception occurred when calling "
                                + callback.getClass().getName() + ".after()", e);
                        params.setThrowable(e);
                    }

                    if (!isConstructor && params.getResult() != originalResult) {
                        writeReturnValue(accessor, returnType, params.getResult());
                    }
                }
            };

            Hooks.hook(executable, Hooks.EntryPointType.DIRECT,
                    hookTransformer, Hooks.EntryPointType.DIRECT);

            return callback.new Unhook(hookRecord);
        } catch (Throwable e) {
            // Previously this only caught Exception and silently printed the stack trace.
            // Log the full failure here so callers can tell whether buildBackup, Hooks.hook,
            // or something else failed.
            Log.e(TAG, "Failed to hook " + executable, e);
        }
        return null;
    }

    /**
     * Writes the hook result back into the stack frame according to the executable's
     * actual return type. Using {@code setReference} unconditionally is wrong for
     * primitive return types (int/long/boolean/float/double/...), as it can corrupt
     * the emulated stack frame.
     *
     * @param accessor   the stack frame accessor to write into
     * @param returnType the executable's declared return type
     * @param result     the value to write back
     */
    private static void writeReturnValue(EmulatedStackFrame.StackFrameAccessor accessor,
                                         Class<?> returnType, Object result) {
        if (returnType == void.class) {
            return;
        } else if (returnType == boolean.class) {
            accessor.setBoolean(EmulatedStackFrame.RETURN_VALUE_IDX, (Boolean) result);
        } else if (returnType == byte.class) {
            accessor.setByte(EmulatedStackFrame.RETURN_VALUE_IDX, (Byte) result);
        } else if (returnType == char.class) {
            accessor.setChar(EmulatedStackFrame.RETURN_VALUE_IDX, (Character) result);
        } else if (returnType == short.class) {
            accessor.setShort(EmulatedStackFrame.RETURN_VALUE_IDX, (Short) result);
        } else if (returnType == int.class) {
            accessor.setInt(EmulatedStackFrame.RETURN_VALUE_IDX, (Integer) result);
        } else if (returnType == long.class) {
            accessor.setLong(EmulatedStackFrame.RETURN_VALUE_IDX, (Long) result);
        } else if (returnType == float.class) {
            accessor.setFloat(EmulatedStackFrame.RETURN_VALUE_IDX, (Float) result);
        } else if (returnType == double.class) {
            accessor.setDouble(EmulatedStackFrame.RETURN_VALUE_IDX, (Double) result);
        } else {
            accessor.setReference(EmulatedStackFrame.RETURN_VALUE_IDX, result);
        }
    }

    /**
     * Reads a single argument out of the stack frame using the accessor method that
     * matches its declared type.
     * <p>
     * {@code frame.references()} only contains the frame's reference-type slots
     * (receiver, reference parameters, and a reference return slot if applicable) —
     * primitive parameters (int/long/boolean/...) are stored elsewhere and never show
     * up in it. Building args by slicing {@code references()} silently drops every
     * primitive argument, which is why reads must be dispatched by the parameter's
     * actual type instead.
     *
     * @param accessor  the stack frame accessor to read from
     * @param paramType the declared type of the parameter at {@code index}
     * @param index     the parameter's position within the frame's MethodType
     * @return the (boxed, if primitive) argument value
     */
    private static Object readArgumentAt(EmulatedStackFrame.StackFrameAccessor accessor,
                                         Class<?> paramType, int index) {
        if (!paramType.isPrimitive()) {
            return accessor.getReference(index);
        } else if (paramType == boolean.class) {
            return accessor.getBoolean(index);
        } else if (paramType == byte.class) {
            return accessor.getByte(index);
        } else if (paramType == char.class) {
            return accessor.getChar(index);
        } else if (paramType == short.class) {
            return accessor.getShort(index);
        } else if (paramType == int.class) {
            return accessor.getInt(index);
        } else if (paramType == long.class) {
            return accessor.getLong(index);
        } else if (paramType == float.class) {
            return accessor.getFloat(index);
        } else if (paramType == double.class) {
            return accessor.getDouble(index);
        } else {
            throw new IllegalArgumentException("Unsupported parameter type: " + paramType);
        }
    }

    /**
     * Inserts an element at the front of an object array.
     * <p>
     * Example: {@code addFirst(new Object[]{"B", 2}, "A")} returns {@code ["A", "B", 2]}.
     *
     * @param arr     the original array, may be {@code null}
     * @param element the element to prepend
     * @return a new array with the element inserted at index 0
     */
    private static Object[] addFirst(Object[] arr, Object element) {
        if (arr == null) {
            return new Object[]{element};
        }
        Object[] result = new Object[arr.length + 1];
        result[0] = element;
        System.arraycopy(arr, 0, result, 1, arr.length);
        return result;
    }

    /**
     * Builds a backup method that will be used as the replacement target when hooking.
     * The backup class is dynamically generated with a minimal method that returns null/0/void,
     * and is loaded via a new {@link ClassLoader}.
     * <p>
     * Supports both {@link Method} and {@link Constructor}. Since a constructor's
     * {@link Executable#getName()} returns the fully-qualified class name (which is not a
     * valid, non-constructor dex method name), a synthetic name is used instead. The generated
     * backup for a constructor is an ordinary instance method (not flagged {@code ACC_CONSTRUCTOR})
     * with a {@code void} return, matching the constructor's calling shape.
     *
     * @param executable the original method or constructor
     * @return the backup {@link Method} instance
     * @throws NoSuchMethodException if the backup method cannot be found in the generated class
     */
    private static Method buildBackup(Executable executable) throws NoSuchMethodException {
        boolean isConstructor = executable instanceof Constructor;
        boolean isStatic = Modifier.isStatic(executable.getModifiers());
        MethodType methodType = rawMethodTypeOf(executable); // constructor -> return type is void

        ProtoId protoId = ProtoId.of(methodType);

        String originName = executable.getDeclaringClass().getName();
        String backupName = originName + "$Backup";

        // Constructor#getName() returns the FQCN, which is not usable as a plain
        // (non-<init>) dex method name, so constructors get a distinct synthetic name.
        String backupMethodName = isConstructor ? "backupInit" : executable.getName();

        TypeId backupId = TypeId.ofName(backupName);
        TypeId backupOriginId = TypeId.ofName(originName);
        MethodId backupMethodId = MethodId.of(backupId, backupMethodName, protoId);

        ClassDef backupDef = ClassBuilder.build(backupId, cb -> cb
                .withSuperClass(TypeId.OBJECT)
                .withFlags(ACC_PUBLIC)
                .withMethod(mb -> mb
                        .of(backupMethodId)
                        .withFlags((ACC_PUBLIC | ACC_STATIC))
                        .withCode(2, ib -> {
                            if (isConstructor || methodType.returnType() == void.class) {
                                // Constructors (and void methods) just return.
                                ib.return_void();
                            } else {
                                ib.const_(ib.v(0), 0)
                                        .return_object(ib.v(0));
                            }
                        })
                )
        );

        ClassLoader loader = ClassUtils.newLoader(
                AlterCore.class.getClassLoader(),
                DexIO.write(Dex.of(backupDef))
        );

        Class<?> backupClass = ClassUtils.forName(backupName, loader);
        return backupClass.getDeclaredMethod(backupMethodName, methodType.parameterArray());
    }

    /**
     * Logs a formatted message when debug mode is enabled.
     *
     * @param fmt  the format string (as in {@link String#format})
     * @param args the arguments referenced by the format specifiers
     */
    private static void print(String fmt, Object... args) {
        if (isDebug) {
            Log.i(TAG, String.format(fmt, args));
        }
    }

    public static void deoptimize(Executable ex) {
        Hooks.deoptimize(ex);
    }

    /**
     * Invokes the original implementation of the given method or constructor, bypassing
     * any active hook so the un-hooked behavior can still be observed from within a
     * callback (e.g. to call through to the real implementation).
     * <p>
     * If the target is not currently hooked, this falls back to a direct
     * {@link Method#invoke} / {@link Constructor#newInstance} call. That fallback has a
     * known race: if another thread installs a hook on the same target between the
     * lookup above and the actual invocation, the call will go through the newly
     * installed hook instead of the original implementation. Do not rely on this
     * fallback behavior being un-hooked.
     *
     * @param method     the method or constructor whose original implementation should run
     * @param thisObject the receiver, or {@code null} for static methods / constructors
     * @param args       the arguments for the call
     * @return the result of the original call ({@code null} for constructors)
     * @throws NullPointerException      if {@code method} is {@code null}
     * @throws IllegalAccessException    should never happen, since accessibility is forced
     * @throws InvocationTargetException if the underlying call throws
     * @throws IllegalArgumentException  if the arguments don't match the target's signature,
     *                                   or a non-null receiver is given for an un-hooked constructor
     */
    public static Object invokeOriginalMethod(Member method, Object thisObject, Object... args)
            throws IllegalAccessException, InvocationTargetException {

        if (method == null) throw new NullPointerException("method == null");
        if (method instanceof Method) {
            ((Method) method).setAccessible(true);
        } else if (method instanceof Constructor) {
            ((Constructor<?>) method).setAccessible(true);
        } else {
            throw new IllegalArgumentException("method must be of type Method or Constructor");
        }

        HookRecord hookRecord = sHookRecords.get(Reflection.getArtMethod((Executable) method));
        if (hookRecord == null) {
            if (isDebug) {
                Log.w(TAG, "Attempting to invoke the original implementation of a method that is "
                                + "not hooked: " + method + ". This falls back to a direct call; if another "
                                + "thread hooks this method concurrently, the call may go through the "
                                + "newly installed hook instead of the real implementation.",
                        new Throwable("here"));
            }
            if (method instanceof Constructor) {
                if (thisObject != null) {
                    throw new IllegalArgumentException(
                            "Cannot invoke a not-hooked Constructor with a non-null receiver");
                }
                try {
                    return ((Constructor<?>) method).newInstance(args);
                } catch (InstantiationException e) {
                    throw new IllegalArgumentException("Invalid constructor: " + method, e);
                }
            } else {
                return ((Method) method).invoke(thisObject, args);
            }
        }

        // Unlike Pine, AlterCore builds the backup synchronously inside hook() and
        // publishes the HookRecord to sHookRecords only after `backup` is assigned
        // (see AlterCore.hook()). This branch should therefore be unreachable; it is
        // kept only as an explicit guard so a broken invariant fails loudly instead
        // of silently misbehaving.
        if (hookRecord.backup == null) {
            throw new IllegalStateException(
                    "hookRecord.backup is null for " + method + ". A HookRecord must never be "
                            + "published before its backup method is assigned.");
        }

        return callBackupMethod(hookRecord, thisObject, args);
    }

    /**
     * Invokes the generated backup method that carries the original implementation of
     * {@code hookRecord.target}.
     * <p>
     * The backup method is always a static trampoline (see {@link #buildBackup}): for
     * non-static targets the receiver is passed as the first element of the argument
     * array rather than as the reflective receiver, since the backup's declaring class
     * (a synthetic "$Backup" class) is unrelated to the original's declaring class and
     * would fail ART's receiver-type check otherwise.
     *
     * @param hookRecord the hook record describing the target and its backup
     * @param thisObject the receiver for non-static targets, ignored for static targets
     * @param args       the arguments for the call
     * @return the result of the original call
     */
    public static Object callBackupMethod(HookRecord hookRecord, Object thisObject, Object[] args)
            throws InvocationTargetException, IllegalAccessException {
        // java.lang.Class objects are movable and may cause crashes when invoking the backup
        // method; native JNI entry points can also be changed by RegisterNatives/
        // UnregisterNatives, so keep a reference alive across the call.
        Member origin = hookRecord.target;
        Method backup = hookRecord.backup;
        Class<?> declaring = origin.getDeclaringClass();

        // FIXME: a args error
        Object[] backupArgs = hookRecord.isStatic ? args : addFirst(args, thisObject);

        // FIXME: a GC happening exactly here (try Runtime.getRuntime().gc() to reproduce)
        // can crash the backup call; see the FIXME on the original Pine implementation.
        Object result = backup.invoke(null, backupArgs);

        // Explicit use of declaring_class to keep a reference on the stack and avoid it
        // being moved/collected by a concurrent GC while `backup` is executing.
        declaring.getClass();
        return result;
    }

    /**
     * Stores information about the hooks applied to a method.
     */
    public static class HookData {
        protected AfterCallback after;
        protected BeforeCallback before;
        protected ReplaceCallback replace;

        /**
         * Sets the callback to be executed after the method is called.
         *
         * @param after The AfterCallback instance.
         * @return The updated MethodInfo object.
         */
        public HookData setAfter(AfterCallback after) {
            this.after = after;
            return this;
        }

        /**
         * Sets the callback to be executed before the method is called.
         *
         * @param before The BeforeCallback instance.
         * @return The updated MethodInfo object.
         */
        public HookData setBefore(BeforeCallback before) {
            this.before = before;
            return this;
        }

        /**
         * Sets the callback to replace the method implementation.
         *
         * @param replace The ReplaceCallback instance.
         * @return The updated MethodInfo object.
         */
        public HookData setReplace(ReplaceCallback replace) {
            this.replace = replace;
            return this;
        }
    }

    /**
     * Handles before and after hooks.
     */
    private static class HookCallback extends MethodHook {
        private final HookData data;

        /**
         * Initializes HookCallback with the corresponding method's information.
         *
         * @param member The method or constructor to hook.
         */
        public HookCallback(Member member) {
            this.data = methodMaps.get(member);
        }

        /**
         * Executes the after callback if defined.
         *
         * @param params The call frame context.
         */
        @Override
        public void after(HookParams params) {
            try {
                super.after(params);
                if (data != null && data.after != null)
                    data.after.afterHook(params);

            } catch (Throwable err) {
                if (isDebug) err.printStackTrace();
            }
        }

        /**
         * Executes the before callback if defined.
         *
         * @param params The call frame context.
         */
        @Override
        public void before(HookParams params) {
            try {
                super.before(params);

                if (data != null && data.before != null)
                    data.before.beforeHook(params);

            } catch (Throwable err) {
                if (isDebug) err.printStackTrace();
            }
        }
    }

    /**
     * Handles method replacement.
     */
    private static class ReplacementCallback extends MethodReplacement {
        private final HookData data;

        /**
         * Initializes ReplacementCallback with the corresponding method's information.
         *
         * @param member The method or constructor to replace.
         */
        public ReplacementCallback(Member member) {
            this.data = methodMaps.get(member);
        }

        /**
         * Replaces the method implementation if a replacement callback is defined,
         * otherwise invokes the original method.
         *
         * @param params The call frame context.
         * @return The result of the method call.
         * @throws Throwable if an error occurs during method execution.
         */
        @Override
        public Object replace(HookParams params) throws Throwable {
            try {
                if (data != null && data.replace != null)
                    return data.replace.replaceHook(params);

            } catch (Throwable err) {
                if (isDebug) err.printStackTrace();
            }
            return params.invokeOriginalMethod();
        }
    }

}