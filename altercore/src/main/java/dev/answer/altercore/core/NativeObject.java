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
package dev.answer.altercore.core;

import static dev.answer.altercore.utils.UnsafeWrapper.getUnsafe;
import android.os.Build;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;

import dev.answer.altercore.NativeImpl;
import dev.tmpfs.libcoresyscall.core.IAllocatedMemory;
import dev.tmpfs.libcoresyscall.core.MemoryAllocator;
import dev.tmpfs.libcoresyscall.core.NativeAccess;
import dev.tmpfs.libcoresyscall.core.NativeHelper;
import libcore.io.Memory;
import sun.misc.Unsafe;
public class NativeObject implements Closeable {
    protected static final Unsafe unsafe = getUnsafe();
    public static final NativeObject EMPTY_OBJECT = new NativeObject(0);
    private static ByteOrder byteOrder = null;
    private static int pageSize = -1;
    private long address;
    protected long allocationAddress;
    private int memorySize = 0;
    private IAllocatedMemory mAllocatedMemory;

    public NativeObject(long address) {
        this.allocationAddress = address;
        this.address = address;
    }

    public NativeObject(long address, long offset) {
        this.allocationAddress = address;
        this.address = address + offset;
    }
    public NativeObject(boolean zeroed, int size) {
       this.mAllocatedMemory = MemoryAllocator.allocate(size, zeroed);
       this.allocationAddress = mAllocatedMemory.getAddress();
       this.address = mAllocatedMemory.getAddress();
       this.memorySize = size;
    }

    protected NativeObject(int size, boolean pageAligned) {
        this.memorySize = size;
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

    public int getMemorySize(){
        return this.memorySize;
    }

    public NativeObject peekPointer() {
        long newAddress;
        int addressSize = addressSize();
        if (addressSize == 4) {
            // 32位架构处理，使用 & 0xFFFFFFFFL 确保无符号扩展
            newAddress = peekInt( this.address, false) & 0xFFFFFFFFL;
        } else if (addressSize == 8) {
            newAddress = peekLong( this.address, false);
        } else {
            throw new InternalError("Address size not supported");
        }
        return new NativeObject(newAddress);
    }


    public NativeObject peekPointer(int offset) {
        long newAddress;
        if (!isCurrentRuntime64Bit()) {
            // 32位架构处理，使用 & 0xFFFFFFFFL 确保无符号扩展
            newAddress = peekInt(offset + this.address, false) & 0xFFFFFFFFL;
        } else if(isCurrentRuntime64Bit()) {
            newAddress = peekLong(offset + this.address, false);
        } else {
            throw new InternalError("Address size not supported");
        }
        return new NativeObject(newAddress);
    }

    public void pokePointer(int offset, NativeObject ob) {
        if (!isCurrentRuntime64Bit()) {
            pokeInt(offset + this.address, (int) (ob.address & 0xFFFFFFFFL), false);
        } else if(isCurrentRuntime64Bit()){
            pokeLong(offset + this.address, ob.address, false);
        } else {
            throw new InternalError("Address size not supported");
        }
    }

    // 内存访问方法 - 使用统一的 peek/poke 接口
    public final byte getByte(int offset) {
        return peekByte(offset + this.address);
    }

    public final void putByte(int offset, byte value) {
        pokeByte(offset + this.address, value);
    }

    public final short getShort(int offset) {
        // 由于隐藏API限制，使用两个字节拼接
        long addr = offset + this.address;
        if (isBigEndian()) {
            return (short) ((peekByte(addr) << 8) | (peekByte(addr + 1) & 0xFF));
        } else {
            return (short) ((peekByte(addr + 1) << 8) | (peekByte(addr) & 0xFF));
        }
    }

    public final void putShort(int offset, short value) {
        long addr = offset + this.address;
        if (isBigEndian()) {
            pokeByte(addr, (byte) (value >> 8));
            pokeByte(addr + 1, (byte) value);
        } else {
            pokeByte(addr, (byte) value);
            pokeByte(addr + 1, (byte) (value >> 8));
        }
    }

    public final char getChar(int offset) {
        return (char) getShort(offset);
    }

    public final void putChar(int offset, char value) {
        putShort(offset, (short) value);
    }

    public final int getInt(int offset) {
        return peekInt(offset + this.address, false);
    }

    public final void putInt(int offset, int value) {
        pokeInt(offset + this.address, value, false);
    }

    public final long getLong(int offset) {
        return peekLong(offset + this.address, false);
    }

    public final void putLong(int offset, long value) {
        pokeLong(offset + this.address, value, false);
    }

    public final float getFloat(int offset) {
        return Float.intBitsToFloat(getInt(offset));
    }

    public final void putFloat(int offset, float value) {
        putInt(offset, Float.floatToIntBits(value));
    }

    public final double getDouble(int offset) {
        return Double.longBitsToDouble(getLong(offset));
    }

    public final void putDouble(int offset, double value) {
        putLong(offset, Double.doubleToLongBits(value));
    }

    // 统一的 peek/poke 方法，自动处理 API 版本
    private static byte peekByte(long address) {
        if (Build.VERSION.SDK_INT >= 23) {
            return Memory.peekByte(address);
        } else {
            return unsafe.getByte(address);
        }
    }

    private static int peekInt(long address, boolean swap) {
        if (Build.VERSION.SDK_INT >= 23) {
            return Memory.peekInt(address, swap);
        } else {
            int result = unsafe.getInt(address);
            return swap ? Integer.reverseBytes(result) : result;
        }
    }

    private static long peekLong(long address, boolean swap) {
        if (Build.VERSION.SDK_INT >= 23) {
            return Memory.peekLong(address, swap);
        } else {
            long result = unsafe.getLong(address);
            return swap ? Long.reverseBytes(result) : result;
        }
    }

    private static void pokeByte(long address, byte value) {
        if (Build.VERSION.SDK_INT >= 23) {
            Memory.pokeByte(address, value);
        } else {
            unsafe.putByte(address, value);
        }
    }

    private static void pokeInt(long address, int value, boolean swap) {
        if (Build.VERSION.SDK_INT >= 23) {
            Memory.pokeInt(address, value, swap);
        } else {
            unsafe.putInt(address, swap ? Integer.reverseBytes(value) : value);
        }
    }

    private static void pokeLong(long address, long value, boolean swap) {
        if (Build.VERSION.SDK_INT >= 23) {
            Memory.pokeLong(address, value, swap);
        } else {
            unsafe.putLong(address, swap ? Long.reverseBytes(value) : value);
        }
    }

    public long invokePointer(Object... args){
        return NativeImpl.callPointerFunction(address(), args);
    }
    public long invokePointerL(long... args){
        return NativeAccess.callPointerFunction(address(), args);
    }

    public NativeObject invokePointerObject(Object... args){
        return new NativeObject(invokePointer(args));
    }

    public NativeObject invokePointerObjectL(long... args){
        return new NativeObject(invokePointerL(args));
    }

    public static boolean isCurrentRuntime64Bit() {
        return NativeHelper.isCurrentRuntime64Bit();
    }

    public static int addressSize() {
        return unsafe.addressSize();
    }

    private static boolean isBigEndian() {
        ByteOrder order = byteOrder();
        return order == ByteOrder.BIG_ENDIAN;
    }

    public static ByteOrder byteOrder() {
        ByteOrder byteOrder2 = byteOrder;
        if (byteOrder2 != null) {
            return byteOrder2;
        }

        // 使用更安全的方式检测字节序
        long a = unsafe.allocateMemory(8L);
        try {
            // 写入测试数据
            unsafe.putLong(a, 0x0102030405060708L);
            byte firstByte = unsafe.getByte(a);

            if (firstByte == 0x01) {
                byteOrder = ByteOrder.BIG_ENDIAN;
            } else if (firstByte == 0x08) {
                byteOrder = ByteOrder.LITTLE_ENDIAN;
            } else {
                // 默认值
                byteOrder = ByteOrder.nativeOrder();
            }
            return byteOrder;
        } finally {
            unsafe.freeMemory(a);
        }
    }

    static int pageSize() {
        if (pageSize == -1) {
            pageSize = unsafe.pageSize();
        }
        return pageSize;
    }

    public boolean isNull() {
        return address == 0;
    }

    public boolean isNotNull(){
        return address != 0;
    }

    public void free() {
        if (allocationAddress != 0) {
            unsafe.freeMemory(allocationAddress);
            allocationAddress = 0;
        }
    }

    @Override
    public void close() throws IOException {
        if (address != 0) {
            address = 0;
            allocationAddress = 0;
            memorySize = 0;
            if (mAllocatedMemory == null)
                free();
            else
                mAllocatedMemory.free();
        }
    }

//    @Override
//    protected void finalize() throws Throwable {
//        super.finalize();
//        if (allocationAddress != 0) {
//            unsafe.freeMemory(allocationAddress);
//        }
//    }
}