package dev.tmpfs.libcoresyscall.core.impl.trampoline;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

import dev.tmpfs.libcoresyscall.core.MemoryAccess;
import dev.tmpfs.libcoresyscall.core.NativeAccess;
import dev.tmpfs.libcoresyscall.core.NativeHelper;
import dev.tmpfs.libcoresyscall.core.Syscall;
import dev.tmpfs.libcoresyscall.core.impl.ByteArrayUtils;
import dev.tmpfs.libcoresyscall.core.impl.NativeBridge;
import dev.tmpfs.libcoresyscall.core.impl.ReflectHelper;
import dev.tmpfs.libcoresyscall.elfloader.SymbolResolver;

public abstract class BaseShellcode {

    protected BaseShellcode() {
    }

    public abstract int getNativeDebugBreakOffset();

    public abstract int getNativeClearCacheOffset();

    public abstract int getNativeSyscallOffset();

    public abstract int getNativeCallPointerFunction0Offset();

    public abstract int getNativeCallPointerFunction1Offset();

    public abstract int getNativeCallPointerFunction2Offset();

    public abstract int getNativeCallPointerFunction3Offset();

    public abstract int getNativeCallPointerFunction4Offset();

    public abstract int getNativeGetJavaVmOffset();

    public abstract int getFakeStat64Offset();

    public abstract int getFakeMmap64Offset();

    public abstract int getFakeMmapOffset();

    public abstract byte[] getShellcodeBytes();

    /**
     * Get the ashmem dev_t id. If /dev/ashmem is not available, return 0.
     *
     * @return the ashmem dev_t id.
     */
    protected long getAshmemDeviceId() {
        StructStat statAshmem;
        try {
            statAshmem = Os.stat("/dev/ashmem");
        } catch (ErrnoException e) {
            if (e.errno == OsConstants.ENOENT) {
                // fine, ashmem is not available
                return 0;
            } else {
                // something wired?
                throw ReflectHelper.unsafeThrow(e);
            }
        }
        return statAshmem.st_dev;
    }

    /**
     * Get the __dl___errno function address.
     *
     * @return the __dl___errno function address.
     */
    protected long getDlErrnoFunctionAddress() {
        SymbolResolver linker;
        try {
            linker = SymbolResolver.getModule(NativeHelper.isCurrentRuntime64Bit() ? "linker64" : "linker");
        } catch (SymbolResolver.NoSuchModuleException | IOException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
        long dl_errno = linker.getSymbolAddress("__dl___errno");
        if (dl_errno == 0) {
            throw new IllegalStateException("symbol not found: __dl___errno");
        }
        return dl_errno;
    }

    protected void fillInHookInfo(@NonNull byte[] shellcode, int offset) {
        //struct HookInfo {
        //    // dev_t for ashmem
        //    uint64_t ashmem_dev_v;
        //    union {
        //        int* (* fn_dl_errno)();
        //        uint64_t _padding_1;
        //    };
        //    union {
        //        size_t page_size;
        //        uint64_t _padding_2;
        //    };
        //};
        ByteArrayUtils.writeInt64(shellcode, offset, getAshmemDeviceId());
        ByteArrayUtils.writeInt64(shellcode, offset + 8, getDlErrnoFunctionAddress());
        ByteArrayUtils.writeInt64(shellcode, offset + 16, NativeBridge.getPageSize());
    }

    public TrampolineInfo generateTrampoline() {
        final int pageSize = (int) NativeBridge.getPageSize();
        final byte[] shellcodeBytes = getShellcodeBytes();
        if (shellcodeBytes.length > pageSize) {
            throw new IllegalStateException("trampoline size is too large: " + shellcodeBytes.length);
        }
        byte[] trampolinePage = new byte[pageSize];
        System.arraycopy(shellcodeBytes, 0, trampolinePage, 0, shellcodeBytes.length);
        HashMap<Method, Integer> nativeEntryOffsetMap = new HashMap<>();
        // collect all native method offsets used in NativeBridge.class
        try {
            nativeEntryOffsetMap.put(
                    NativeBridge.class.getMethod("nativeSyscall",
                            int.class, long.class, long.class, long.class, long.class, long.class, long.class),
                    getNativeSyscallOffset());
            nativeEntryOffsetMap.put(
                    NativeBridge.class.getMethod("nativeClearCache", long.class, long.class),
                    getNativeClearCacheOffset());
            nativeEntryOffsetMap.put(
                    NativeBridge.class.getMethod("nativeCallPointerFunction0", long.class),
                    getNativeCallPointerFunction0Offset());
            nativeEntryOffsetMap.put(
                    NativeBridge.class.getMethod("nativeCallPointerFunction1", long.class, long.class),
                    getNativeCallPointerFunction1Offset());
            nativeEntryOffsetMap.put(
                    NativeBridge.class.getMethod("nativeCallPointerFunction2", long.class, long.class, long.class),
                    getNativeCallPointerFunction2Offset());
            nativeEntryOffsetMap.put(
                    NativeBridge.class.getMethod("nativeCallPointerFunction3", long.class, long.class, long.class, long.class),
                    getNativeCallPointerFunction3Offset());
            nativeEntryOffsetMap.put(
                    NativeBridge.class.getMethod("nativeCallPointerFunction4", long.class, long.class, long.class, long.class, long.class),
                    getNativeCallPointerFunction4Offset());
            nativeEntryOffsetMap.put(
                    NativeBridge.class.getMethod("nativeGetJavaVM"),
                    getNativeGetJavaVmOffset());
        } catch (NoSuchMethodException e) {
            // should not happen
            throw ReflectHelper.unsafeThrow(e);
        }
        return new TrampolineInfo(trampolinePage, nativeEntryOffsetMap);
    }

    /**
     * Write the shellcode to the text section.
     * <p>
     * If the destination address is not text section, the result is undefined.
     *
     * @param shellcode the shellcode bytes to write
     * @param address   the address to write the shellcode
     */
    protected void writeByteArrayToTextSection(@NonNull byte[] shellcode, long address) {
        if (!NativeHelper.isCurrentRuntime64Bit() && (address & 0xffffffff00000000L) != 0) {
            throw new IllegalArgumentException("address overflow");
        }
        // make the page writable
        final int pageSize = (int) MemoryAccess.getPageSize();
        try {
            long pageStart = ByteArrayUtils.alignDown(address, pageSize);
            long pageEnd = ByteArrayUtils.alignUp(address + shellcode.length, pageSize);
            Syscall.mprotect(pageStart, pageEnd - pageStart,
                    OsConstants.PROT_READ | OsConstants.PROT_EXEC | OsConstants.PROT_WRITE);
            // place the hook
            MemoryAccess.pokeByteArray(address, shellcode, 0, shellcode.length);
            // restore the protection
            Syscall.mprotect(pageStart, pageEnd - pageStart, OsConstants.PROT_READ | OsConstants.PROT_EXEC);
            NativeAccess.clearCache(address, shellcode.length);
        } catch (ErrnoException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
    }

}
