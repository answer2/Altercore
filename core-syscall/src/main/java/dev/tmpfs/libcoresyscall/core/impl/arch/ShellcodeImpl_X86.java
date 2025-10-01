package dev.tmpfs.libcoresyscall.core.impl.arch;

import dev.tmpfs.libcoresyscall.core.impl.ByteArrayUtils;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.BaseShellcode;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISimpleInlineHook;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISyscallNumberTable;

public class ShellcodeImpl_X86 extends BaseShellcode implements ISimpleInlineHook, ISyscallNumberTable {

    public static final ShellcodeImpl_X86 INSTANCE = new ShellcodeImpl_X86();

    private ShellcodeImpl_X86() {
        super();
    }

    @Override
    public byte[] getShellcodeBytes() {
        //0000 g    DF .text  000b NativeBridge_breakpoint
        //0000 g    D  .text  0000 ___text_section
        //0010 g    DF .text  003e NativeBridge_nativeSyscall
        //0050 g    DF .text  0045 syscall_ext
        //00a0 g    DF .text  000a NativeBridge_nativeClearCache
        //00b0 g    DF .text  000a __clear_cache
        //00c0 g    DF .text  0021 NativeBridge_nativeCallPointerFunction0
        //00f0 g    DF .text  0027 NativeBridge_nativeCallPointerFunction1
        //0120 g    DF .text  002d NativeBridge_nativeCallPointerFunction2
        //0150 g    DF .text  0030 NativeBridge_nativeCallPointerFunction3
        //0180 g    DF .text  0030 NativeBridge_nativeCallPointerFunction4
        //01b0 g    DF .text  004a NativeBridge_nativeGetJavaVM
        //0200 g    DF .text  0043 ashmem_dev_get_size_region
        //0250 g    DF .text  001c get_hook_info
        //0270 g    DF .text  000d get_current_pc
        //0280 g    DF .text  00b2 fake_fstat64
        //0340 g    DF .text  041d fake_mmap64
        //0880 g    DF .text  003f fake_mmap
        //0c40 l     O .rodata  0018 _ZZ13get_hook_infoE9sHookInfo
        String b64 =
                "VYnlg+T8zInsXcOQkJCQkFWJ5VOD5PCD7BDoAAAAAFuBwzkMAACD7AT/dTz/dTT/dSz/dST/dRz/\n" +
                        "dRT/dRDoDQAAAIPEIDHSjWX8W13DkJBVieVXVoPk/IPsEItNEItVFIt1GIt9HItFDIlEJASLRSCJ\n" +
                        "RCQIi0UIiUQkDI1EJARVU4toBIsYi0AIzYBbXY1l+F5fXcOQkJCQkJCQkJCQkFWJ5YPk/InsXcOQ\n" +
                        "kJCQkJBVieWD5PyJ7F3DkJCQkJCQVYnlU4Pk8IPsEOgAAAAAW4HDiQsAAP9VEDHSjWX8W13DkJCQ\n" +
                        "kJCQkJCQkJCQkJCQVYnlU4Pk8IPsEOgAAAAAW4HDWQsAAItFGIkEJP9VEDHSjWX8W13DkJCQkJCQ\n" +
                        "kJCQVYnlU4Pk8IPsEOgAAAAAW4HDKQsAAIPsCP91IP91GP9VEIPEEDHSjWX8W13DkJCQVYnlU4Pk\n" +
                        "8IPsEOgAAAAAW4HD+QoAAIPsBP91KP91IP91GP9VEIPEEDHSjWX8W13DVYnlU4Pk8IPsEOgAAAAA\n" +
                        "W4HDyQoAAP91MP91KP91IP91GP9VEIPEEDHSjWX8W13DVYnlU4Pk8IPsEOgAAAAAW4HDmQoAAItF\n" +
                        "CMcEJAAAAACLCIPsCI1UJAhSUP+RbAMAAIPEEInBuAAAAACFyXUDiwQkMdKNZfxbXcOQkJCQkJBV\n" +
                        "ieVTg+Twg+ww6AAAAABbgcNJCgAAi0UIDyiD2P///w8RRCQIiUQkBMdEJBgAAAAAxwQkNgAAAOgT\n" +
                        "/v//jWX8W13DkJCQkJCQkJCQkJCQkFWJ5YPk/OgAAAAAWIHA/QkAAI2A6P///4nsXcOQkJCQVYnl\n" +
                        "g+T8i0UEiexdw5CQkFWJ5VNXVoPk8IPsIOgAAAAAW4HDxwkAAIt9DItFCA9XwA8RRCQMiXwkCIlE\n" +
                        "JATHBCTFAAAA6Jb9//89AfD//3ITicb/k/D////33okwuP/////rVouL6P///4uT7P///4t3BDHW\n" +
                        "iz8xzzHACfd1OwnRdDeLdQyLTiwLTjB1LItFCIkEJOj3/v//PQDw//93GIlGLMdGMAAAAADHRlwA\n" +
                        "AAAAx0ZYAAAAADHAjWX0Xl9bXcOQkJCQkJCQkJCQkJCQkFWJ5VNXVoPk8IHs4AAAAOgAAAAAW4HD\n" +
                        "BAkAAIt9HItFFLIBMcmDfRgAD4hyAQAAg+Aig/gCD4VmAQAAD1fADylEJHAPKUQkYA8pRCRQDylE\n" +
                        "JEAPKUQkMA8pRCQgg+wgDxFEJAyNRCRAiUQkCItFGIlEJATHBCTFAAAA6JH8//+DxCA9APD//3YV\n" +
                        "icb/k/D////33okwsAExyen9AAAAi4Po////i4vs////i1QkJDHKi3QkIDHGCdZ1PwnIdDuLRCRM\n" +
                        "C0QkUHUxg+wM/3UY6O39//+DxBA9APD//3cciUQkTMdEJFAAAAAAx0QkfAAAAADHRCR4AAAAAIuD\n" +
                        "6P///4uL7P///4tUJCQxyot0JCAxxgnWD5TCCcgPlcAg0InGsAGLTRD2wQR0cA9XwA8phCTAAAAA\n" +
                        "DymEJLAAAAAPKYQkoAAAAA8phCSQAAAADymEJIAAAADHhCTQAAAAAAAAAIPsIA8RRCQMjYQkoAAA\n" +
                        "AIlEJAiLRRiJRCQExwQkDQEAAOiL+///g8QguZQZAgEzjCSAAAAACcEPlcCJ8YnK9tIgwohUJAyJ\n" +
                        "zv+T8P////fH/w8AAHQQxwAWAAAAuP/////p6QEAAIlEJAiLTSCLVQy4tf////fBAPD//3U1ifAA\n" +
                        "wA+2wAtFEA+k+RSD7ARR/3UY/3UUUFL/dQhowAAAAOgN+///g8QgPQHw//8PgrgAAACJXCQQMcmD\n" +
                        "+PMPlcGJTCQUMduLTRT2wQIPlMOJzsHuBYPmATHJi1UQ9sIED5TBD7Z8JAwJ3wnxCc8LfCQUdBL3\n" +
                        "2ItMJAiJAbj/////6UwBAACLVRT2wgIPlMH3RSAA8P//i1wkEIt9HHU6CkwkDHU0i00Qg+H7i0Ug\n" +
                        "D6T4FIPsBFD/dRhSUf91DP91CGjAAAAA6Gn6//+DxCA9APD//w+GAQEAAItEJAjHAA0AAAC4////\n" +
                        "/+nlAAAAifGEyYtNIA+E2AAAAIl8JByJRCQYg30MAA+EmAAAAInPMdKLRCQYiUQkEItNDOstkJCQ\n" +
                        "kJCQkItMJAwpwYtUJAiD2gABx4l8JByLfCQUg9cAAcaJdCQQicgJ0HRaiXwkFIlUJAiJTCQMgfkA\n" +
                        "EAAAuAAQAAAPQsGF0r4AEAAAD0XGg+wEagBXi3wkKFdQi3QkJFb/dRhotAAAAOio+f//g8QghcB/\n" +
                        "kYP4/It8JBSLVCQIi0wkDHSgi4P4////i00MAcFJ99ghyIPsBGoAagBqAP91EFCLdCQwVmp96Gf5\n" +
                        "//+J8IPEII1l9F5fW13Dg+wgD1fADxFEJAyLdQyJdCQIiUQkBMcEJFsAAADoOPn//4PEID0B8P//\n" +
                        "czmLRRCDyAKLTRSDySCD7ARqAGr/UVBW/3UIaMAAAADoC/n//4PEID0B8P//i00gD4M6/v//6bv+\n" +
                        "//8PC5CQkFWJ5VOD5PCD7DDoAAAAAFuBw+kEAACLRQiLTQwPV8APEUQkDIlMJAiJRCQExwQkDQEA\n" +
                        "AOi4+P//jWX8W13DkJBVieVTg+Twg+wQ6AAAAABbgcOpBAAAi00guLX////3wQDw//91J4tFHA+k\n" +
                        "wRSD7ARR/3UY/3UU/3UQ/3UM/3UIaMAAAADoZvj//4PEII1l/Ftdw5CQkJCQkJCQkJCQkJBVieVT\n" +
                        "g+Twg+ww6AAAAABbgcNJBAAADyhFCItFGIlEJBQPEUQkBMdEJBgAAAAAxwQktAAAAOgW+P//jWX8\n" +
                        "W13DVYnlU4Pk8IPsEOgAAAAAW4HDCQQAAIPsBGoAagBqAP91EP91DP91CGp96OH3//+DxCCNZfxb\n" +
                        "XcOQkJCQkJCQkFWJ5VOD5PCD7DDoAAAAAFuBw8kDAAAPKEUIi0UYi00ciUwkFIlEJBAPEQQkx0Qk\n" +
                        "GAAAAADoh/r//41l/Ftdw5BVieVTg+Twg+ww6AAAAABbgcOJAwAAi0UIi00MD1fADxFEJAyJTCQI\n" +
                        "iUQkBMcEJFsAAADoWPf//41l/Ftdw5CQVYnlU4Pk8IPsEOgAAAAAW4HDSQMAAIPsBGoAagBqAP91\n" +
                        "EP91DP91CGoD6CH3//+DxCCNZfxbXcOQkJCQkJCQkFWJ5VOD5PCD7BDoAAAAAFuBwwkDAACD7ARq\n" +
                        "AGoAagD/dRD/dQz/dQhqBOjh9v//g8QgjWX8W13DkJCQkJCQkJBVieVTg+Twg+wQ6AAAAABbgcPJ\n" +
                        "AgAAD7dFFIPsBGoAagBQ/3UQ/3UM/3UIaCcBAADom/b//4PEII1l/Ftdw5CQVYnlU4Pk8IPsMOgA\n" +
                        "AAAAW4HDiQIAAA8oRQgPEUQkBMdEJBgAAAAAx0QkFAAAAADHBCQsAQAA6FX2//+NZfxbXcOQkJCQ\n" +
                        "kJCQkJCQkJCQkJBVieVTg+Twg+wQ6AAAAABbgcM5AgAAg+wEagBqAGoA/3UM/3UIapxoLAEAAOgP\n" +
                        "9v//g8QgjWX8W13DkJCQkJCQVYnlU4Pk8IPsMOgAAAAAW4HD+QEAAItFCItNDA9XwA8RRCQMiUwk\n" +
                        "CIlEJATHBCTFAAAA6Mj1//+NZfxbXcOQkFWJ5VOD5PCD7DDoAAAAAFuBw7kBAACLRQgPV8APEUQk\n" +
                        "CIlEJATHRCQYAAAAAMcEJAYAAADoh/X//41l/Ftdw5BVieVTg+Twg+wQ6AAAAABbgcN5AQAAg+wE\n" +
                        "agBqAGoA/3UQ/3UM/3UIajboUfX//4PEII1l/Ftdw5CQkJCQkJCQVYnlU4Pk8IPsMOgAAAAAW4HD\n" +
                        "OQEAAItFCA9XwA8RRCQIiUQkBMdEJBgAAAAAxwQk/AAAAOgH9f//kJCQkJCQkFWJ5YPk/A8LkJCQ\n" +
                        "kJCQkJBVieVTV1aD5PyLTRCLRQiFyQ+ErQAAAItdDIgYiFwI/4P5Aw+CmwAAAIhYAYhYAohcCP6I\n" +
                        "XAj9g/kHD4KEAAAAiFgDiFwI/IP5CXJ4icf334PnA40UOCn5g+H8D7bzafYBAQEBiTQ4iXQR/IP5\n" +
                        "CXJUiXIEiXIIiXQK9Il0CviD+RlyQWYPbsZmD3DAAPMPf0IM8w9/RArkidaD5gSDzhgp8YP5IHIe\n" +
                        "AfKQkJCQkJCQkPMPfwLzD39CEIPB4IPCIIP5H3fsjWX0Xl9bXcMAAAAABHcAAAAAAAAAAAAAAAAA\n" +
                        "AO++r94AAAAAFEURAAAAAAAAEAAAAAAAAAAQAAAAAAAAAAAAAA==\n";
        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        int hookInfoOffset = 0x0c40;
        fillInHookInfo(bytes, hookInfoOffset);
        return bytes;
    }

    @Override
    public int getNativeDebugBreakOffset() {
        return 0x0000;
    }

    @Override
    public int getNativeClearCacheOffset() {
        return 0x00a0;
    }

    @Override
    public int getNativeSyscallOffset() {
        return 0x0010;
    }

    @Override
    public int getNativeCallPointerFunction0Offset() {
        return 0x00c0;
    }

    @Override
    public int getNativeCallPointerFunction1Offset() {
        return 0x00f0;
    }

    @Override
    public int getNativeCallPointerFunction2Offset() {
        return 0x0120;
    }

    @Override
    public int getNativeCallPointerFunction3Offset() {
        return 0x0150;
    }

    @Override
    public int getNativeCallPointerFunction4Offset() {
        return 0x0180;
    }

    @Override
    public int getNativeGetJavaVmOffset() {
        return 0x01b0;
    }

    @Override
    public int getFakeStat64Offset() {
        return 0x0280;
    }

    @Override
    public int getFakeMmap64Offset() {
        return 0x0340;
    }

    @Override
    public int getFakeMmapOffset() {
        return 0x0880;
    }

    @Override
    public int __NR_mprotect() {
        // __NR_mprotect x86 125
        return 125;
    }

    @Override
    public int __NR_memfd_create() {
        // __NR_memfd_create x86 356
        return 356;
    }

    @Override
    public int __NR_ioctl() {
        // __NR_ioctl x86 54
        return 54;
    }

    @Override
    public int __NR_tgkill() {
        // __NR_tgkill x86 270
        return 270;
    }

    @Override
    public void inlineHook(long address, long hook) {
        if (address == 0) {
            throw new IllegalArgumentException("address is 0");
        }
        if (hook == 0) {
            throw new IllegalArgumentException("hook is 0");
        }
        // 68 [hook]  push address:hook
        // c3        ret
        // maybe this will make the intel shadow stack unhappy
        // but let's ignore it for now
        byte[] stub = new byte[6];
        stub[0] = 0x68;
        ByteArrayUtils.writeInt32(stub, 1, (int) hook);
        stub[5] = (byte) 0xc3;
        writeByteArrayToTextSection(stub, address);
    }

}
