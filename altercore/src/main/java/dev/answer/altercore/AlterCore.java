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

import dev.answer.altercore.core.HookTransformer;
import dev.answer.altercore.core.Hooks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.answer.altercore.callback.MethodHook;
import dev.answer.altercore.core.HookParams;
import dev.answer.altercore.core.HookRecord;

/**
 * Core hooking engine that manages method interception and backup generation.
 */
public class AlterCore {

    private static final String TAG = "AlterCore";
    private static final Map<Long, HookRecord> sHookRecords = new ConcurrentHashMap<>();
    private static final Object sHookLock = new Object();

    /**
     * Installs a hook on the given executable (method or constructor).
     *
     * @param executable the target method or constructor to hook
     * @param callback   the callback that will be invoked before and after the original call
     * @return an {@link MethodHook.Unhook} object that can be used to remove the hook,
     *         or {@code null} if an error occurred
     */
    public static MethodHook.Unhook hook(Executable executable, MethodHook callback) {
        try {
            if (AlterConfig.isDebug) {
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

            long artMethod = Reflection.getArtMethod(backupMethod);
            HookRecord hookRecord;
            boolean newMethod = false;

            synchronized (sHookLock) {
                hookRecord = sHookRecords.get(artMethod);
                if (hookRecord == null) {
                    newMethod = true;
                    hookRecord = new HookRecord(executable, artMethod);
                    sHookRecords.put(artMethod, hookRecord);
                }
            }

            hookRecord.isStatic = isStatic;
            hookRecord.backup = backupMethod;

            HookRecord finalHookRecord = hookRecord;
            HookTransformer hookTransformer = new HookTransformer() {
                @Override
                public void transform(MethodHandle original, EmulatedStackFrame frame) throws Throwable {
                    EmulatedStackFrame.StackFrameAccessor accessor = frame.accessor();
                    Object[] frameRefs = frame.references();

                    Object thisObject = isStatic ? null : frameRefs[0];
                    Object[] argsData = Arrays.copyOfRange(frameRefs, isStatic ? 0 : 1, frameRefs.length - 1);

                    HookParams params = new HookParams(finalHookRecord, thisObject, argsData);

                    try {
                        callback.before(params);
                    } catch (Throwable e) {
                        Log.e(TAG, "Unexpected exception occurred when calling "
                                + callback.getClass().getName() + ".beforeCall()", e);
                        params.resetResult();
                        params.setThrowable(e);
                    }

                    Object[] methodArgs = isStatic ? params.args : addFirst(params.args, params.thisObject);

                    Object originalResult = null;
                    if (!params.returnEarly) {
                        try {
                            originalResult = original.invokeWithArguments(methodArgs);
                            params.setResult(originalResult);
                        } catch (Exception e) {
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

                    if (params.getResult() != originalResult) {
                        accessor.setReference(EmulatedStackFrame.RETURN_VALUE_IDX, params.getResult());
                    }
                }
            };

            Hooks.hook(executable, Hooks.EntryPointType.DIRECT,
                    hookTransformer, Hooks.EntryPointType.DIRECT);

            return callback.new Unhook(hookRecord);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
    public static Object[] addFirst(Object[] arr, Object element) {
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
     * The backup class is dynamically generated with a minimal method that returns null/0,
     * and is loaded via a new {@link ClassLoader}.
     *
     * @param executable the original method or constructor
     * @return the backup {@link Method} instance
     * @throws NoSuchMethodException if the backup method cannot be found in the generated class
     */
    public static Method buildBackup(Executable executable) throws NoSuchMethodException {
        boolean isStatic = Modifier.isStatic(executable.getModifiers());
        MethodType methodType = rawMethodTypeOf(executable);

        ProtoId protoId = ProtoId.of(methodType);

        String originName = executable.getDeclaringClass().getName();
        String backupName = originName + "$Backup";
        TypeId backupId = TypeId.ofName(backupName);
        TypeId backupOriginId = TypeId.ofName(originName);
        MethodId backupMethodId = MethodId.of(backupId, executable.getName(), protoId);

        ClassDef backupDef = ClassBuilder.build(backupId, cb -> cb
                .withSuperClass(TypeId.OBJECT)
                .withFlags(ACC_PUBLIC)
                .withMethod(mb -> mb
                        .of(backupMethodId)
                        .withFlags(ACC_PUBLIC)
                        .withCode(2, ib -> ib
                                .const_(ib.v(0), 0)      // load null into register v0
                                .return_object(ib.v(0))   // return null
                        )
                )
        );

        ClassLoader loader = ClassUtils.newLoader(
                AlterCore.class.getClassLoader(),
                DexIO.write(Dex.of(backupDef))
        );

        Class<?> backupClass = ClassUtils.forName(backupName, loader);
        return backupClass.getDeclaredMethod(executable.getName(), methodType.parameterArray());
    }

    /**
     * Logs a formatted message when debug mode is enabled.
     *
     * @param fmt  the format string (as in {@link String#format})
     * @param args the arguments referenced by the format specifiers
     */
    public static void print(String fmt, Object... args) {
        if (AlterConfig.isDebug) {
            Log.i(TAG, String.format(fmt, args));
        }
    }
}