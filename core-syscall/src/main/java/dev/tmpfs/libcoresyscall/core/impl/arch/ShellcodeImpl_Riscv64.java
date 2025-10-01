package dev.tmpfs.libcoresyscall.core.impl.arch;

import dev.tmpfs.libcoresyscall.core.impl.ByteArrayUtils;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.BaseShellcode;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISimpleInlineHook;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISyscallNumberTable;

public class ShellcodeImpl_Riscv64 extends BaseShellcode implements ISimpleInlineHook, ISyscallNumberTable {

    public static final ShellcodeImpl_Riscv64 INSTANCE = new ShellcodeImpl_Riscv64();

    private ShellcodeImpl_Riscv64() {
        super();
    }

    @Override
    public byte[] getShellcodeBytes() {
        //0000 g    DF .text  0004 NativeBridge_breakpoint
        //0000 g    D  .text  0000 ___text_section
        //0004 g    DF .text  0012 NativeBridge_nativeSyscall
        //0016 g    DF .text  0014 syscall_ext
        //002a g    DF .text  0016 NativeBridge_nativeClearCache
        //0040 g    DF .text  0010 __clear_cache
        //0050 g    DF .text  0002 NativeBridge_nativeCallPointerFunction0
        //0052 g    DF .text  0004 NativeBridge_nativeCallPointerFunction1
        //0056 g    DF .text  0006 NativeBridge_nativeCallPointerFunction2
        //005c g    DF .text  000a NativeBridge_nativeCallPointerFunction3
        //0066 g    DF .text  000c NativeBridge_nativeCallPointerFunction4
        //0072 g    DF .text  0030 NativeBridge_nativeGetJavaVM
        //00a2 g    DF .text  0014 ashmem_dev_get_size_region
        //00b6 g    DF .text  000a get_hook_info
        //00c0 g    DF .text  0004 get_current_pc
        //00c4 g    DF .text  008c fake_fstat64
        //0150 g    DF .text  02b0 fake_mmap64
        //0480 g    DF .text  0002 fake_mmap
        //0750 l     O .rodata  0018 _ZZ13get_hook_infoE9sHookInfo
        String b64 =
                "ApCCgIJitoUyhTqGvoZCh8aHFogJoKqILoWyhTaGuoY+h8KHcwAAAIKAMoWzhcYAkwgwEAFGcwAA\n" +
                        "ABHhgoAAAJMIMBABRnMAAAAR4YKAAAAChjaFAoa6hTaFAoa6hTaFMoc+hgKHuoU2hTKHPobChgKH\n" +
                        "AREG7CLoABAMYQO2hW0jNAT+kwWE/gKWgzWE/jM1oAB9FW2NEwEE/uJgQmQFYYKAqoUdZRsGRXB1\n" +
                        "RYFGAUeBRwFIjbcXBQAAEwWlaYKABoWCgHlxBvQi8CbsSuhO5AAYrokqiRMFAAXKhU6GgUYBR4FH\n" +
                        "AUjv8B/z/XWFJWNttQCqhBcFAAADNUVmApW7BZBADMF9VS2gA7YJABcFAACDNUVkAUVjHbYAmckD\n" +
                        "tQkDGeVKhe/w//d9dmN8pgABRRMBBP2icAJ04mRCaaJpRWGCgKqFAUUjuLkCI7QJAM23SXGG9qLy\n" +
                        "pu7K6s7m0uJW/lr6XvZi8mbuaupu5oAavou6iTaLMokuiqqKgUyFTRcFAAATDCVdY00HDhN1KwKJ\n" +
                        "RWMYtQ5XcLjNVzQAXhMFBPEndAUCEwUABRMGBPHOhYFGAUeBRwFI7/Af5v11Y/ylAKqEAzWMAAKV\n" +
                        "gUy7BZBADMEFRW2ggzUE8QM1DABjkKUCEc0DNQT0GelOhe/wn+v9dWPmpQAjMKT0IzwE8AM1BPGD\n" +
                        "NQwALY0TNRUAszWwALN8tQCTdUkABUW1wRMFBO9XcIHNVzQAXid0BQITBQTuJ3QFAhMFBO0ndAUC\n" +
                        "V3CkzVc0AF4TBQTpJ3QFAiMwBPATBcACEwYE6c6FgUYBR4FHAUjv8H/bgzUE6TsFBQg3JgIBGwZG\n" +
                        "mbGNTY0zNaAAs32VQQM1jAAClZOVSwPZRPHlKo0TlRwAs2YlARMF4A3WhVKGWofOh16I7/Af1/11\n" +
                        "hSVjabUCmwUFALUFszWwABNG+/8FgpNWWwATR/n/CYPZjlWOBYozZrYB0Y29xbsEoEClqGOODAaq\n" +
                        "imMKCgIFa/FcVo3ShCmgiYyqmyqdhcCz1mQLEwUwBM6FaoZeh4FHAUjv8J/Q40Gg/uMClf8DNQwB\n" +
                        "fRqzBaoAMwagQG2OEwUgDtaFyoYBR4FHAUjv8P/NE3VJAB3tVoUhqBN1KwATNRUAM2W1AbVEOcVq\n" +
                        "hQTBfVUTAQTptnAWdPZkVmm2aRZq8npSe7J7EnzybFJtsm11YYKAgzUMATMGsEAzdVYBVprSlfGN\n" +
                        "kwgwEAFGcwAAAKqFVoXd3bmok3a5/xMF4A3WhVKGWofOh16I7/A/xv18qoVqhePtvPgTBXANUoaB\n" +
                        "RgFHgUcBSO/wf8QBJYUsY3OVA5NmKQATZwsCEwXgDf1X1oVShgFI7/B/wv11hSXjcbXu5bUAAEER\n" +
                        "BuQi4AAILoaqhRMFwAKBRgFHgUcBSO/w/78BJRMBBP+iYAJkQQGCgCqIEwXgDb6Iuoc2h7KGLobC\n" +
                        "hUaI4b4qhxMFMAS2h7KGLoa6hT6HgUcBSMm2QREG5CLgAAiyhi6GqoUTBSAOAUeBRwFI7/CfugEl\n" +
                        "EwEE/6JgAmRBAYKAwblBEQbkIuAACC6GqoUTBXANgUYBR4FHAUjv8N+3ASUTAQT/omACZEEBgoCq\n" +
                        "hhMF8AMyhy6GtoW6hgFHgUcBSJm+qoYTBQAEMocuhraFuoYBR4FHAUiBtkERBuQi4AAIsocuhqqF\n" +
                        "O4cGCBMFgAO+hoFHAUjv8D+yASUTAQT/omACZEEBgoBBEQbkIuAACDaHsoYuhqqFEwXwBIFHAUjv\n" +
                        "8J+vASUTAQT/omACZEEBgoBBEQbkIuAACK6GKoYTBfAEkwXA+QFHgUcBSO/w36wBJRMBBP+iYAJk\n" +
                        "QQGCgEERBuQi4AAILoaqhRMFAAWBRgFHgUcBSO/wP6oBJRMBBP+iYAJkQQGCgEERBuQi4AAIqoUT\n" +
                        "BZADAUaBRgFHgUcBSO/wn6cBJRMBBP+iYAJkQQGCgEERBuQi4AAIsoYuhqqFdUUBR4FHAUjv8B+l\n" +
                        "ASUTAQT/omACZEEBgoBBEQbkIuAACKqFEwXgBQFGgUYBR4FHAUjv8H+iAABjBgYUIwC1ALMGxQAN\n" +
                        "R6OPtv5jbeYSowC1ACMBtQAjj7b+HUejjrb+Y2LmEqMBtQAlRyOOtv5ja+YQuwagQJP3NgCzBvUA\n" +
                        "Mwj2QBN2yP/iFbcXEBCSB5OHBxCztfUCjMKzh8YAI663/mNi5g7MwozGI6q3/mVHI6y3/mNp5gzM\n" +
                        "xozKzMqMziOit/4jpLf+I6a3/hP3RgCTCIcBswIWQRMGAAIjqLf+Y+PCChOWBQK7hcUIMwboQBMG\n" +
                        "hvwVghMIFgBzJiDCE18mALOHFgFjdOgBvoaJqLMG4EGzeNgAk5ZYALOC0kC+lld3kA1XxAVeDgZX\n" +
                        "pQhSV7WilhMDAAKhQ0FO4U5Gh1fGpwIn9GcKJ/TDDid0zg4n9M4OMwfnQbKXffNjBhgDE4YC/n1H\n" +
                        "M1bmChNG9v8WlgGaNpYTBgYCjOKM5ozqjO6ThgYC45rG/oKAAAAAAAAAAAAAAAAA776v3gAAAAAU\n" +
                        "RREAAAAAAAAQAAAAAAAA\n";
        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        int hookInfoOffset = 0x0750;
        fillInHookInfo(bytes, hookInfoOffset);
        return bytes;
    }

    @Override
    public int getNativeDebugBreakOffset() {
        return 0x0000;
    }

    @Override
    public int getNativeClearCacheOffset() {
        return 0x002a;
    }

    @Override
    public int getNativeSyscallOffset() {
        return 0x0004;
    }

    @Override
    public int getNativeCallPointerFunction0Offset() {
        return 0x0050;
    }

    @Override
    public int getNativeCallPointerFunction1Offset() {
        return 0x0052;
    }

    @Override
    public int getNativeCallPointerFunction2Offset() {
        return 0x0056;
    }

    @Override
    public int getNativeCallPointerFunction3Offset() {
        return 0x005c;
    }

    @Override
    public int getNativeCallPointerFunction4Offset() {
        return 0x0066;
    }

    @Override
    public int getNativeGetJavaVmOffset() {
        return 0x0072;
    }

    @Override
    public int getFakeStat64Offset() {
        return 0x00c4;
    }

    @Override
    public int getFakeMmap64Offset() {
        return 0x0150;
    }

    @Override
    public int getFakeMmapOffset() {
        return 0x0480;
    }

    @Override
    public int __NR_mprotect() {
        // mprotect riscv64 226
        return 226;
    }

    @Override
    public int __NR_memfd_create() {
        // memfd_create riscv64 279
        return 279;
    }

    @Override
    public int __NR_ioctl() {
        // ioctl riscv64 29
        return 29;
    }

    @Override
    public int __NR_tgkill() {
        // tgkill riscv64 131
        return 131;
    }

    @Override
    public void inlineHook(long address, long hook) {
        if (address == 0) {
            throw new IllegalArgumentException("address is 0");
        }
        if (hook == 0) {
            throw new IllegalArgumentException("hook is 0");
        }
        if (address % 2 != 0 || hook % 2 != 0) {
            throw new IllegalArgumentException("address or hook is not aligned, address: " + address + ", hook: " + hook);
        }
        int nopCount = ((((int) address % 8) + 8 + 4) % 8) % 2;
        // 01 00         nop
        // nopCount * 2 + 12 + 8
        int nopBytes = nopCount * 2;
        byte[] trampoline = new byte[nopBytes + 20];
        for (int i = 0; i < nopBytes; i += 2) {
            trampoline[i] = 0x01;
            trampoline[i + 1] = 0x00;
        }
        // add jump to hook
        // 17 0e 00 00   auipc   t3, 0x0
        // 03 3e ce 00   ld      t3, 0xc(t3)
        // 67 03 0e 00   jalr    t1, t3
        // .addr hook
        ByteArrayUtils.writeBytes(trampoline, nopBytes, new byte[]{
                0x17, 0x0e, 0x00, 0x00,
                0x03, 0x3e, (byte) 0xce, 0x00,
                0x67, 0x03, 0x0e, 0x00
        });
        ByteArrayUtils.writeInt64(trampoline, nopBytes + 12, hook);
        writeByteArrayToTextSection(trampoline, address);
    }

}
