package dev.answer.altercore;

import static dev.answer.altercore.utils.UnsafeWrapper.getUnsafe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class NativeImpl {
    private static final int length = getUnsafe().addressSize();
    public static void memcpy(long src, long dest, int length) {
        getUnsafe().copyMemory(src, dest, length);
    }

    public static synchronized byte[] get(long address, int size){
        byte[] bytes = new byte[size];
        getUnsafe().copyMemoryToPrimitiveArray(address, bytes, 0, size);
        System.out.println(Arrays.toString( bytes));
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
}
