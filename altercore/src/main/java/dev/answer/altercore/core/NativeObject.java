package dev.answer.altercore.core;

import static dev.answer.altercore.utils.UnsafeWrapper.getUnsafe;
import java.nio.ByteOrder;
import sun.misc.Unsafe;

class NativeObject {
    static final boolean $assertionsDisabled = false;
    private final long address;
    protected long allocationAddress;
    protected static final Unsafe unsafe = getUnsafe();
    private static ByteOrder byteOrder = null;
    private static int pageSize = -1;

    public NativeObject(long address) {
        this.allocationAddress = address;
        this.address = address;
    }

    public NativeObject(long address, long offset) {
        this.allocationAddress = address;
        this.address = address + offset;
    }

    protected NativeObject(int size, boolean pageAligned) {
        if (!pageAligned) {
            long allocateMemory = unsafe.allocateMemory(size);
            this.allocationAddress = allocateMemory;
            this.address = allocateMemory;
        } else {
            int ps = pageSize();
            long a = unsafe.allocateMemory(size + ps);
            this.allocationAddress = a;
            this.address = (ps + a) - (((long) (ps - 1)) & a);
        }
    }

    public long address() {
        return this.address;
    }

    public long allocationAddress() {
        return this.allocationAddress;
    }

    public NativeObject subObject(int offset) {
        return new NativeObject(offset + this.address);
    }

    public NativeObject getObject(int offset) {
        long newAddress;
        int addressSize = addressSize();
        if (addressSize == 4) {
            newAddress = unsafe.getInt(offset + this.address) & (-1);
        } else if (addressSize == 8) {
            newAddress = unsafe.getLong(offset + this.address);
        } else {
            throw new InternalError("Address size not supported");
        }
        return new NativeObject(newAddress);
    }

    public void putObject(int offset, NativeObject ob) {
        int addressSize = addressSize();
        if (addressSize == 4) {
            putInt(offset, (int) (ob.address & (-1)));
        } else {
            if (addressSize == 8) {
                putLong(offset, ob.address);
                return;
            }
            throw new InternalError("Address size not supported");
        }
    }

    public final byte getByte(int offset) {
        return unsafe.getByte(offset + this.address);
    }

    public final void putByte(int offset, byte value) {
        unsafe.putByte(offset + this.address, value);
    }

    public final short getShort(int offset) {
        return unsafe.getShort(offset + this.address);
    }

    public final void putShort(int offset, short value) {
        unsafe.putShort(offset + this.address, value);
    }

    public final char getChar(int offset) {
        return unsafe.getChar(offset + this.address);
    }

    public final void putChar(int offset, char value) {
        unsafe.putChar(offset + this.address, value);
    }

    public final int getInt(int offset) {
        return unsafe.getInt(offset + this.address);
    }

    public final void putInt(int offset, int value) {
        unsafe.putInt(offset + this.address, value);
    }

    public final long getLong(int offset) {
        return unsafe.getLong(offset + this.address);
    }

    public final void putLong(int offset, long value) {
        unsafe.putLong(offset + this.address, value);
    }

    public final float getFloat(int offset) {
        return unsafe.getFloat(offset + this.address);
    }

    public final void putFloat(int offset, float value) {
        unsafe.putFloat(offset + this.address, value);
    }

    public final double getDouble(int offset) {
        return unsafe.getDouble(offset + this.address);
    }

    public final void putDouble(int offset, double value) {
        unsafe.putDouble(offset + this.address, value);
    }

    public static int addressSize() {
        return unsafe.addressSize();
    }

   public static ByteOrder byteOrder() {
        ByteOrder byteOrder2 = byteOrder;
        if (byteOrder2 != null) {
            return byteOrder2;
        }
        long a = unsafe.allocateMemory(8L);
        try {
            unsafe.putLong(a, 72623859790382856L);
            byte b = unsafe.getByte(a);
            if (b == 1) {
                byteOrder = ByteOrder.BIG_ENDIAN;
            } else if (b == 8) {
                byteOrder = ByteOrder.LITTLE_ENDIAN;
            }
            unsafe.freeMemory(a);
            return byteOrder;
        } catch (Throwable th) {
            unsafe.freeMemory(a);
            th.printStackTrace();
            return null;
        }
    }

    static int pageSize() {
        if (pageSize == -1) {
            pageSize = unsafe.pageSize();
        }
        return pageSize;
    }
}

