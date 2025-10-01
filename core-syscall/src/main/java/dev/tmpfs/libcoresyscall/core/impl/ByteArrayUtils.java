package dev.tmpfs.libcoresyscall.core.impl;

import androidx.annotation.NonNull;

public class ByteArrayUtils {

    private ByteArrayUtils() {
        throw new AssertionError("no instances");
    }

    public static void writeInt32(@NonNull byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xff);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xff);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xff);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    public static void writeInt64(@NonNull byte[] buffer, int offset, long value) {
        buffer[offset] = (byte) (value & 0xff);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xff);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xff);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xff);
        buffer[offset + 4] = (byte) ((value >> 32) & 0xff);
        buffer[offset + 5] = (byte) ((value >> 40) & 0xff);
        buffer[offset + 6] = (byte) ((value >> 48) & 0xff);
        buffer[offset + 7] = (byte) ((value >> 56) & 0xff);
    }

    public static void writeBytes(@NonNull byte[] buffer, int offset, byte... value) {
        System.arraycopy(value, 0, buffer, offset, value.length);
    }

    public static long alignDown(long value, long alignment) {
        return value & -alignment;
    }

    public static long alignUp(long value, long alignment) {
        return (value + alignment - 1) & -alignment;
    }

    public static long alignDownToPage(long value) {
        return alignDown(value, NativeBridge.getPageSize());
    }

    public static long alignUpToPage(long value) {
        return alignUp(value, NativeBridge.getPageSize());
    }

}
