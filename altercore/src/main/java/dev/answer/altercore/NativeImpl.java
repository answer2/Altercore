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

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;


import dev.answer.altercore.core.NativeObject;
import dev.answer.altercore.utils.AlterLog;
import dev.tmpfs.libcoresyscall.core.MemoryAccess;
import dev.tmpfs.libcoresyscall.core.MemoryAllocator;
import dev.tmpfs.libcoresyscall.core.NativeAccess;
import dev.tmpfs.libcoresyscall.core.NativeHelper;
import dev.tmpfs.libcoresyscall.core.Syscall;
import dev.tmpfs.libcoresyscall.elfloader.SymbolResolver;
import sun.misc.Unsafe;

public class NativeImpl {
    private static final int length = getUnsafe().addressSize();
    private static final Unsafe theUnsafe = getUnsafe();


    /** The value of {@code arrayBaseOffset(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(boolean[].class);

    /** The value of {@code arrayBaseOffset(byte[].class)} */
    public static final int ARRAY_BYTE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(byte[].class);

    /** The value of {@code arrayBaseOffset(short[].class)} */
    public static final int ARRAY_SHORT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(short[].class);

    /** The value of {@code arrayBaseOffset(char[].class)} */
    public static final int ARRAY_CHAR_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(char[].class);

    /** The value of {@code arrayBaseOffset(int[].class)} */
    public static final int ARRAY_INT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(int[].class);

    /** The value of {@code arrayBaseOffset(long[].class)} */
    public static final int ARRAY_LONG_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(long[].class);

    /** The value of {@code arrayBaseOffset(float[].class)} */
    public static final int ARRAY_FLOAT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(float[].class);

    /** The value of {@code arrayBaseOffset(double[].class)} */
    public static final int ARRAY_DOUBLE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(double[].class);

    /** The value of {@code arrayBaseOffset(Object[].class)} */
    public static final int ARRAY_OBJECT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(Object[].class);

    /** The value of {@code arrayIndexScale(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE
            = theUnsafe.arrayIndexScale(boolean[].class);

    /** The value of {@code arrayIndexScale(byte[].class)} */
    public static final int ARRAY_BYTE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(byte[].class);

    /** The value of {@code arrayIndexScale(short[].class)} */
    public static final int ARRAY_SHORT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(short[].class);

    /** The value of {@code arrayIndexScale(char[].class)} */
    public static final int ARRAY_CHAR_INDEX_SCALE
            = theUnsafe.arrayIndexScale(char[].class);

    /** The value of {@code arrayIndexScale(int[].class)} */
    public static final int ARRAY_INT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(int[].class);

    /** The value of {@code arrayIndexScale(long[].class)} */
    public static final int ARRAY_LONG_INDEX_SCALE
            = theUnsafe.arrayIndexScale(long[].class);

    /** The value of {@code arrayIndexScale(float[].class)} */
    public static final int ARRAY_FLOAT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(float[].class);

    /** The value of {@code arrayIndexScale(double[].class)} */
    public static final int ARRAY_DOUBLE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(double[].class);

    /** The value of {@code arrayIndexScale(Object[].class)} */
    public static final int ARRAY_OBJECT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(Object[].class);

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

    public static long mmap(int length) {
        try {
            final int MAP_ANONYMOUS = 0x20;
            long address = Os.mmap(0, length,
                    OsConstants.PROT_READ | OsConstants.PROT_WRITE | OsConstants.PROT_EXEC,
                    OsConstants.MAP_PRIVATE | MAP_ANONYMOUS,
                    null, 0);
            return address;
        } catch (ErrnoException e) {
            AlterLog.v("mmap failed: " + e.errno);
            return 0L;
        }
    }

    /**
     * 取消内存映射
     * @param addr   映射起始地址（必须页对齐，通常由 mmap 返回）
     * @param length 映射长度（单位：字节）
     * @return true 成功，false 失败
     */
    public static boolean munmap(long addr, long length) {
        try {
            // 直接调用 Os.munmap
            Os.munmap(addr, length);
            return true;
        } catch (ErrnoException e) {
            AlterLog.v( "munmap failed: " + e.getMessage() + " (" + e.errno + ")");
            return false;
        }
    }

    /**
     * 修改内存页保护属性为可读、可写、可执行
     * @param addr 起始地址（可能未页对齐）
     * @param len  需要保护的长度
     * @return true 成功，false 失败
     */
    public static boolean munprotect(long addr, long len) {
        // 获取页大小
        long pageSize = getPageSize();
        // 计算偏移
        long alignment = addr % pageSize;
        AlterLog.v( "munprotect page size: " + pageSize + ", alignment: " + alignment);

        // 对齐起始地址，并计算总长度
        long start = addr - alignment;
        long total = alignment + len;

        try {
            Syscall.mprotect(start, total,
                    OsConstants.PROT_READ | OsConstants.PROT_WRITE | OsConstants.PROT_EXEC);
            return true;
        } catch (ErrnoException e) {
            AlterLog.v( "mprotect failed: " + e.getMessage() + " (" + e.errno + ")");
            return false;
        }
    }

    /**
     * 获取系统页大小（单位：字节）
     */
    private static long getPageSize() {
        return (int) MemoryAccess.getPageSize();
    }


    public static void memput(byte[] bytes, long addr) {
        var len = bytes.length;
        copyMemory(bytes, addr, len);
        getUnsafe().putByte(addr + len, (byte) 0);
    }

    public static byte[] memget(long addr, int length) {
        byte[] bytes = new byte[length];
        getUnsafe().copyMemory(null, addr, bytes, ARRAY_BYTE_BASE_OFFSET, length);
        return bytes;
    }

    public static synchronized long read(long base, long offset) {
        long address = base + offset;
        byte[] bytes = memget(address, length);
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
        memput(bytes, address);
    }


    private static void copyMemory(Object srcArray, long dstAddr, int len) {
        var u = getUnsafe();
        try {
            u.copyMemoryFromPrimitiveArray(srcArray, 0, dstAddr, len);
            return;
        } catch (NoSuchMethodError ignored) {
        }
        Object[] arr = {srcArray};
        int srcAddr =
                u.getInt(arr, u.arrayBaseOffset(Object[].class))
                        + u.arrayBaseOffset(srcArray.getClass());
        u.copyMemory(srcAddr, dstAddr, len);
    }

    private static void copyMemory(long srcAddr, Object dstArray, int len) {
        var u = getUnsafe();
        try {
            u.copyMemoryToPrimitiveArray(srcAddr, dstArray, 0, len);
            return;
        } catch (NoSuchMethodError ignored) {
        }
        Object[] arr = {dstArray};
        int dstAddr =
                u.getInt(arr, u.arrayBaseOffset(Object[].class))
                        + u.arrayBaseOffset(dstArray.getClass());
        u.copyMemory(srcAddr, dstAddr, len);
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
