/*
 * Copyright (C) 2025 AnswerDev. All rights reserved.
 *
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

import static dev.answer.altercore.utils.UnsafeWrapper.getUnsafe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;


import dev.answer.altercore.core.NativeObject;
import dev.tmpfs.libcoresyscall.core.MemoryAllocator;
import dev.tmpfs.libcoresyscall.core.NativeAccess;
import dev.tmpfs.libcoresyscall.core.NativeHelper;
import dev.tmpfs.libcoresyscall.elfloader.SymbolResolver;

public class NativeImpl {
    private static final int length = getUnsafe().addressSize();
    public static void memcpy(long src, long dest, int length) {
        getUnsafe().copyMemory(src, dest, length);
    }

    public static synchronized byte[] get(long address, int size){
        byte[] bytes = new byte[size];
        getUnsafe().copyMemoryToPrimitiveArray(address, bytes, 0, size);
        return bytes;
    }

    public static synchronized void put(byte[] bytes, long address){
        getUnsafe().copyMemoryFromPrimitiveArray(bytes, 0,address, bytes.length);
    }

    public static synchronized long read(long base, long offset) {
        long address = base + offset;
        byte[] bytes = get(address, length);
        if (length == 4) {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
        } else {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
        }
    }

    public static synchronized void write(long base, long offset, long value) {
        long address = base + offset;
        byte[] bytes;
        if (length == 4) {
            if (value > 0xFFFFFFFFL) {
                throw new IllegalStateException("overflow may occur");
            } else {
                bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array();
            }
        } else {
            bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
        }
        put(bytes, address);
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
            long baseOffset = getUnsafe().arrayBaseOffset(Object[].class);

            if (NativeHelper.isCurrentRuntime64Bit()) {
                // 64-bit JVM
                return getUnsafe().getLong(array, baseOffset);
            } else {
                // 32-bit JVM
                return 0xFFFFFFFFL & getUnsafe().getInt(array, baseOffset);
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
            long baseOffset = getUnsafe().arrayBaseOffset(Object[].class);

            if (NativeHelper.isCurrentRuntime64Bit()) {
                // 64-bit JVM without CompressedOops
                getUnsafe().putLong(array, baseOffset, address);
            } else {
                // 32-bit JVM or 64-bit with CompressedOops
                getUnsafe().putInt(array, baseOffset, (int) address);
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

    public static NativeObject getSymbolObject(SymbolResolver symbolResolver, String symbol){
        return new NativeObject(symbolResolver.getSymbolAddress(symbol));
    }

}
