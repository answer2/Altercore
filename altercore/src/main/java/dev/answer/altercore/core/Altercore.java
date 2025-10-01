package dev.answer.altercore.core;

import android.annotation.SuppressLint;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dev.answer.altercore.utils.AlterLog;
import dev.answer.altercore.utils.UnsafeWrapper;
import dev.tmpfs.libcoresyscall.core.MemoryAllocator;
import dev.tmpfs.libcoresyscall.core.NativeAccess;
import dev.tmpfs.libcoresyscall.elfloader.SymbolResolver;
import sun.misc.Unsafe;

public class Altercore {
    private static Unsafe theUnsafe = UnsafeWrapper.getUnsafe();
    private static Class objectArrayClass = Object[].class;
    public static final long TRUE = 1;
    public static final long FALSE = 0;

    public static long nativePeer;

    private static long jit_compile_method;

    private static long jit_load;

    private static long addWeakGloablReference;

    private static long jit_compiler_handle_;

    private static SymbolResolver art_module;

    private static SymbolResolver art_compiler_module;


    static {

        try {
            Field field =Thread.class.getDeclaredField("nativePeer");
            field.setAccessible(true);
            nativePeer = field.getLong(Thread.currentThread());

            art_module = SymbolResolver.getModule("libart.so");
            art_compiler_module = SymbolResolver.getModule("libart-compiler.so");

            jit_compile_method = art_compiler_module.getSymbolAddress("_ZN3art3jit11JitCompiler13CompileMethodEPNS_6ThreadEPNS0_15JitMemoryRegionEPNS_9ArtMethodEbb");
            jit_load = art_compiler_module.getSymbolAddress("jit_load");
            //
            addWeakGloablReference = art_module.getSymbolAddress("_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadENS_6ObjPtrINS_6mirror6ObjectEEE");

            jit_compiler_handle_ = NativeAccess.callPointerFunction(jit_load, FALSE);

            System.out.println("This is addWeakGloablReference : " + addWeakGloablReference);
            System.out.println("This is jit_compile_method : " + jit_compile_method);
            System.out.println("This is jit_load :" + jit_load);
            System.out.println("This is jit_compiler_handle_ :" + jit_compiler_handle_);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void compile_Method(Method method, long nativePeer){

    }


    /**
     * Retrieves the raw memory address of a Java object using Unsafe.
     * <p>
     * - This method is non-standard and depends on JVM internals.
     * - The result is only meaningful within the current JVM instance and memory layout.
     * - Should not be used for object dereferencing in native code unless you know exactly what you're doing.
     *
     * @param obj The Java object whose memory address is to be retrieved.
     * @return The memory address of the object as a long, or -1 if failed.
     */
    public static long getObjectAddress(Object obj) {
        if (obj == null) return 0L;

        try {
            Object[] array = new Object[]{obj};
            long baseOffset = theUnsafe.arrayBaseOffset(Object[].class);
            int scale = theUnsafe.arrayIndexScale(Object[].class);

            AlterLog.d(scale+"");

            if (scale == 8) {
                // 64-bit JVM
                return theUnsafe.getLong(array, baseOffset);
            } else if (scale == 4) {
                // 32-bit JVM
                return 0xFFFFFFFFL & theUnsafe.getInt(array, baseOffset);
            } else {
                // Unexpected scale size
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Converts a raw memory address back into a Java object reference.
     * <p>
     * - This only works if the address is a valid object pointer and has not been GC'ed.
     * - Misuse can crash the JVM or cause undefined behavior.
     * - Do not use with arbitrary native pointers.
     *
     * @param address A long value representing a supposed Java object address.
     * @return The Java object at the given address, or null/undefined if invalid.
     */
    public static Object getObjectByAddress(long address) {
        try {
            Object[] array = new Object[]{null};
            long baseOffset = theUnsafe.arrayBaseOffset(Object[].class);
            int scale = theUnsafe.arrayIndexScale(Object[].class);

            if (scale == 8) {
                // 64-bit JVM without CompressedOops
                theUnsafe.putLong(array, baseOffset, address);
            } else if (scale == 4) {
                // 32-bit JVM or 64-bit with CompressedOops
                theUnsafe.putInt(array, baseOffset, (int) address);
            } else {
                throw new UnsupportedOperationException("Unsupported reference size: " + scale);
            }

            return array[0];
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static long callPointerFunction(long function, Object... args) {
        return NativeAccess.callPointerFunction(function, exchangeType(args));
    }

    public static long[] exchangeType(Object... objects) {
        if (objects == null) return new long[]{0};

        long[] args = new long[objects.length];
        for (int i = 0; i < objects.length; i++) {
            Object object = objects[i];

            if (object == null) {
                args[i] = 0L;
                continue;
            }

            Class<?> clazz = object.getClass();
            if (clazz == String.class) {
                args[i] = MemoryAllocator.copyCString((String) object).getAddress();

            } else if (clazz == Integer.class) {
                args[i] = ((Integer) object).longValue();

            } else if (clazz == Long.class) {
                args[i] = (Long) object;

            } else if (clazz == Short.class) {
                args[i] = ((Short) object).longValue();

            } else if (clazz == Byte.class) {
                args[i] = ((Byte) object).longValue();

            } else if (clazz == Character.class) {
                args[i] = (long) ((Character) object).charValue();
            } else if (clazz == Boolean.class) {
                args[i] = (Boolean) object ? 1L : 0L;

            } else if (clazz == Float.class) {
                args[i] = Float.floatToRawIntBits((Float) object);

            } else if (clazz == Double.class) {
                args[i] = Double.doubleToRawLongBits((Double) object);
            } else {
                args[i] = getObjectAddress(object);
            }
        }

        return args;
    }


    public static Object getObject(long nativePeer, long address) {
        AlterLog.d(address+"");
        long weakRef = NativeAccess.callPointerFunction(
                addWeakGloablReference,
                NativeAccess.getJavaVM(),
                (nativePeer),
                (address)
        );

        long NewLocalRef = art_module.getSymbolAddress("_ZN3art3JNIILb1EE11NewLocalRefEP7_JNIEnvP8_jobject");
        long DeleteWeakGlobalRef = art_module.getSymbolAddress("_ZN3art3JNIILb1EE19DeleteWeakGlobalRefEP7_JNIEnvP8_jobject");

        long newLocalObj = NativeAccess.callPointerFunction(NewLocalRef, NativeAccess.getJavaVM(), weakRef);
        NativeAccess.callPointerFunction(DeleteWeakGlobalRef, NativeAccess.getJavaVM(), weakRef);

        return getObjectAddress(newLocalObj); // 正确转换回 Java 对象
    }


}
