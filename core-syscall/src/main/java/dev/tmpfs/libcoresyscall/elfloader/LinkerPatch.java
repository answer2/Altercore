package dev.tmpfs.libcoresyscall.elfloader;

import java.io.IOException;

import dev.tmpfs.libcoresyscall.core.NativeHelper;
import dev.tmpfs.libcoresyscall.core.impl.NativeBridge;
import dev.tmpfs.libcoresyscall.core.impl.ReflectHelper;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.BaseShellcode;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISimpleInlineHook;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.SimpleInlineHookFactory;

/**
 * This is a linker patch to make the linker load the library in ashmem correctly.
 * If memfd is available, this patch is not needed.
 */
public class LinkerPatch {

    private LinkerPatch() {
        throw new AssertionError("no instances");
    }

    private static boolean sPatchApplied = false;

    private static final boolean sIs64Bit = NativeHelper.isCurrentRuntime64Bit();

    public static synchronized void applyPatch() {
        if (sPatchApplied) {
            return;
        }
        NativeBridge.initializeOnce();
        applyPatchInternalLocked();
        sPatchApplied = true;
    }

    private static void applyPatchInternalLocked() {
        SymbolResolver linker;
        try {
            linker = SymbolResolver.getModule(sIs64Bit ? "linker64" : "linker");
        } catch (SymbolResolver.NoSuchModuleException | IOException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
        long dl_fstat64 = linker.getSymbolAddress("__dl_fstat64");
        if (dl_fstat64 == 0) {
            throw new IllegalStateException("symbol not found: __dl_fstat64");
        }
        long dl_errno = linker.getSymbolAddress("__dl___errno");
        if (dl_errno == 0) {
            throw new IllegalStateException("symbol not found: __dl___errno");
        }
        long dl_mmap64 = linker.getSymbolAddress("__dl_mmap64");
        if (dl_mmap64 == 0) {
            throw new IllegalStateException("symbol not found: __dl_mmap64");
        }
        // some versions of Android also uses __dl_mmap, but it is just fine if it is not found
        long dl_mmap = linker.getSymbolAddress("__dl_mmap");
        // check if everything is ready
        BaseShellcode shellcode = NativeBridge.getShellcode();
        ISimpleInlineHook hookProvider = SimpleInlineHookFactory.create();
        long trampolineBase = NativeBridge.getTrampolineBase();
        if (trampolineBase == 0) {
            throw new IllegalStateException("trampolineBase is 0, is the trampoline initialized?");
        }
        // do the patch
        hookProvider.inlineHook(dl_fstat64, trampolineBase + shellcode.getFakeStat64Offset());
        hookProvider.inlineHook(dl_mmap64, trampolineBase + shellcode.getFakeMmap64Offset());
        if (dl_mmap != 0) {
            hookProvider.inlineHook(dl_mmap, trampolineBase + shellcode.getFakeMmapOffset());
        }
    }

}
