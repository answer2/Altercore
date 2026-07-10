package dev.answer.altercore;

import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.vmtools._Utils.rawMethodTypeOf;

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
import com.v7878.unsafe.VM;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.vmtools.HookTransformer;
import com.v7878.vmtools.Hooks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.InMemoryDexClassLoader;
import dalvik.system.PathClassLoader;
import dev.answer.altercore.callback.MethodHook;
import dev.answer.altercore.core.HookParams;
import dev.answer.altercore.core.HookRecord;

public class AlterCore {

    private static final String TAG = "AlterCore";
    private static final Map<Long, HookRecord> sHookRecords = new ConcurrentHashMap<>();
    private static final Object sHookLock = new Object();

    public String a(String aaa, String aab) {
        return "a" + aaa + aab;
    }

    public static String b() {
        return "bbbbbyuan";
    }

    public static void init() {
        try {
            Method a_method = AlterCore.class.getMethod("a", String.class, String.class);
            Method b_method = AlterCore.class.getMethod("b");

            MethodHook.Unhook un = hook(a_method, new MethodHook() {
                @Override
                public void after(HookParams params) throws Throwable {
                    super.after(params);
                    params.setResult("你好哇");
                }

                @Override
                public void before(HookParams params) throws Throwable {
                    super.before(params);

                }
            });

            MethodHook.Unhook un_ = hook(b_method, new MethodHook() {
                @Override
                public void after(HookParams params) throws Throwable {
                    super.after(params);
                    params.setResult("你好哇bbbb");
                }

                @Override
                public void before(HookParams params) throws Throwable {
                    super.before(params);

                }
            });

            var main = new AlterCore();

             Log.d(TAG,"Hook : " +  main.a(" Hello", "114514"));

             un.unhook();

             Log.d(TAG,"Origin : " +  main.a(" Hello", "114514"));


             Log.d(TAG,"Hook : " +  b());

             un_.unhook();

             Log.d(TAG,"Origin : " +  b());

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    public static MethodHook.Unhook hook(Executable executable, MethodHook callback) {
        try {
            boolean isStatic = Modifier.isStatic(executable.getModifiers());

            Method method = buildBackup(executable);
            Hooks.hook(method, executable, Hooks.EntryPointType.DIRECT);

            long artMethod = Reflection.getArtMethod(method);
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
            hookRecord.backup = method;

            HookRecord finalHookRecord = hookRecord;
            HookTransformer hookTransformer = new HookTransformer() {
                @Override
                public void transform(MethodHandle original, EmulatedStackFrame frame) throws Throwable {
                    // 1. 获取访问器
                    EmulatedStackFrame.StackFrameAccessor accessor = frame.accessor();
                    Object[] args = frame.references();

                    Object returnObj = args[args.length - 1];
                    Object instant = isStatic ? null : args[0];
                    Object[] args_data = Arrays.copyOfRange(args, isStatic ? 0 : 1, args.length - 1);
                    // 包含实例对象 和 参数
                    // Object[] args_pass = Arrays.copyOfRange(args, 0, args.length-1);

                    HookParams params = new HookParams(finalHookRecord, instant, args_data);
                    try {
                        callback.before(params);
                    } catch (Throwable e) {
                        Log.e(TAG, "Unexpected exception occurred when calling " + callback.getClass().getName() + ".beforeCall()", e);
                        // reset result (ignoring what the unexpectedly exiting callback did)
                        params.resetResult();
                        params.setThrowable(e);
                    }

                    Object[] args_pass = isStatic ? params.args : addFirst(params.args, params.thisObject);

                    Object originalResult = null;
                    if (!params.returnEarly) {
                        try {
                            originalResult = original.invokeWithArguments(args_pass);
                            params.setResult(originalResult);
                        } catch (Exception e) {
                            params.setThrowable(e);
                        }
                    }


                    try {
                        callback.after(params);
                    } catch (Throwable e) {
                        Log.e(TAG, "Unexpected exception occurred when calling " + callback.getClass().getName() + ".after()", e);
                        // reset to last result (ignoring what the unexpectedly exiting callback did)
                        params.setThrowable(e);
                    }


                    // 4. 修改返回值并写回
                    if (params.getResult() != originalResult)
                        accessor.setReference(EmulatedStackFrame.RETURN_VALUE_IDX, params.getResult());


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
     * 向 Object 数组首位添加元素
     * 例如：addFirst(new Object[]{"B", 2}, "A") -> ["A", "B", 2]
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

    public static Method buildBackup(Executable executable) throws NoSuchMethodException {
        boolean isStatic = Modifier.isStatic(executable.getModifiers());
        MethodType methodType = rawMethodTypeOf(executable);

        ProtoId protoId = ProtoId.of(methodType);

        String origin_name = executable.getDeclaringClass().getName();
        String backup_name = origin_name + "$Backup";
        TypeId backup_id = TypeId.ofName(backup_name);
        TypeId backup_id_origin = TypeId.ofName(origin_name);
        var backup_method_id = MethodId.of(backup_id, executable.getName(), protoId);

        ClassDef backup_def = ClassBuilder.build(backup_id, cb -> cb
                .withSuperClass(TypeId.OBJECT)
                .withFlags(ACC_PUBLIC)
                .withMethod(mb -> mb
                        .of(backup_method_id)
                        .withFlags(ACC_PUBLIC)
                        .withCode(2, ib -> ib
                                .const_(ib.v(0), 0) // 将 null 加载到寄存器 v0
                                .return_object(ib.v(0))  // 返回 null
                        )
                )
        );

        ClassLoader loader = ClassUtils.newLoader(AlterCore.class.getClassLoader(),
                DexIO.write(Dex.of(backup_def)));

        Class<?> backup_class = ClassUtils.forName(backup_name, loader);

        Method member = backup_class.getDeclaredMethod(executable.getName(), methodType.parameterArray());
        return member;
    }

}
