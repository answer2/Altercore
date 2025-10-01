package dev.answer.altercore.utils;

import java.lang.reflect.Field;

import dev.answer.altercore.AlterConfig;
import sun.misc.Unsafe;

public class UnsafeWrapper {

    private static Unsafe unsafe;

    public static Unsafe getUnsafe() {
        try {
            if (unsafe == null) {
                unsafe = (Unsafe) Unsafe.class.getMethod("getUnsafe").invoke(null);

                if (unsafe == null) {
                    Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                    try {
                        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
                        theUnsafe.setAccessible(true);
                        unsafe =  (Unsafe) theUnsafe.get(null);
                    } catch (Exception e) {
                        try {
                            final Field theUnsafe = unsafeClass.getDeclaredField("THE_ONE");
                            theUnsafe.setAccessible(true);
                            unsafe = (Unsafe) theUnsafe.get(null);
                        } catch (Exception e2) {
                            AlterConfig.supportUnsafe = false;
                            AlterLog.w("Unsafe not found, Sorry, your device will not be able to use this framework");
                            throw new AlterException("Unsafe couldn't ues, please retry");
                        }
                    }
                }
            }
        } catch (Exception e) {
            AlterLog.e("UnsafeWrapper", e);
        }

        return unsafe;
    }

}
