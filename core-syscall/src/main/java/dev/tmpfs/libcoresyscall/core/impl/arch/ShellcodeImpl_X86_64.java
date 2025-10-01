package dev.tmpfs.libcoresyscall.core.impl.arch;

import dev.tmpfs.libcoresyscall.core.impl.ByteArrayUtils;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.BaseShellcode;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISimpleInlineHook;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISyscallNumberTable;

public class ShellcodeImpl_X86_64 extends BaseShellcode implements ISimpleInlineHook, ISyscallNumberTable {

    public static final ShellcodeImpl_X86_64 INSTANCE = new ShellcodeImpl_X86_64();

    private ShellcodeImpl_X86_64() {
        super();
    }

    @Override
    public byte[] getShellcodeBytes() {
        //0000 g    DF .text  0007 NativeBridge_breakpoint
        //0000 g    D  .text  0000 ___text_section
        //0010 g    DF .text  0035 NativeBridge_nativeSyscall
        //0050 g    DF .text  0031 syscall_ext
        //0090 g    DF .text  0006 NativeBridge_nativeClearCache
        //00a0 g    DF .text  0006 __clear_cache
        //00b0 g    DF .text  0007 NativeBridge_nativeCallPointerFunction0
        //00c0 g    DF .text  000a NativeBridge_nativeCallPointerFunction1
        //00d0 g    DF .text  000d NativeBridge_nativeCallPointerFunction2
        //00e0 g    DF .text  0013 NativeBridge_nativeCallPointerFunction3
        //0100 g    DF .text  001a NativeBridge_nativeCallPointerFunction4
        //0120 g    DF .text  0033 NativeBridge_nativeGetJavaVM
        //0160 g    DF .text  0031 ashmem_dev_get_size_region
        //01a0 g    DF .text  000d get_hook_info
        //01b0 g    DF .text  000a get_current_pc
        //01c0 g    DF .text  0099 fake_fstat64
        //0260 g    DF .text  03e2 fake_mmap64
        //0730 g    DF .text  000a fake_mmap
        //0a50 l     O .rodata  0018 _ZZ13get_hook_infoE9sHookInfo
        String b64 =
                "VUiJ5cxdw2YPH4QAAAAAAFVIieVIg+wQTYnKTInAiddMi0UQTItNGEiLVSBIiRQkSInOSInCTInR\n" +
                        "Z+gRAAAASIPEEF3DZmYuDx+EAAAAAABVSInlQVdBVlNMictNicZMi30QSGPHSIn3SInWSInKTYny\n" +
                        "SYnYTYn5DwVbQV5BX13DZmZmZmZmLg8fhAAAAAAAVUiJ5V3DZi4PH4QAAAAAAFVIieVdw2YuDx+E\n" +
                        "AAAAAABVSInlXf/iZg8fhAAAAAAAVUiJ5UiJz13/4mYPH0QAAFVIieVMicZIic9d/+IPHwBVSInl\n" +
                        "TInGSInQSInPTInKXf/gZmZmZi4PH4QAAAAAAFVIieVMicZIidBMi0UQSInPTInKTInBXf/gZg8f\n" +
                        "RAAAVUiJ5UiD7BBIx0X4AAAAAEiLB0iNdfj/kNgGAACFwHUKSItF+EiDxBBdwzHASIPEEF3DZmZm\n" +
                        "Zi4PH4QAAAAAAFVIieVIg+wQSGP3SMcEJAAAAAC6BHcAAL8QAAAAMclFMcBFMcln6MX+//9Ig8QQ\n" +
                        "XcNmZmZmZmYuDx+EAAAAAABVSInlSI0FpQgAAF3DDx8AVUiJ5UiLRQhdw2YPH0QAAFVIieVBV0FW\n" +
                        "QVRTSIPsEEmJ9kGJ/0hj90jHBCQAAAAAMdu/BQAAAEyJ8jHJRTHARTHJZ+hY/v//SD0B8P//chZJ\n" +
                        "icT/FU8IAABB99xEiSC7/////+s0SIsFMwgAAEk5BnUoSIXAdCNJg34wAHUcRIn/6Cr///9IPQDw\n" +
                        "//93DEmJRjBJx0YIAAAAAInYSIPEEFtBXEFeQV9dww8fgAAAAABVSInlQVdBVkFVQVRTSIHsWAEA\n" +
                        "AE2JzkWJx0GJ1EiJdchIiX24RTHtswFFhcCJTbQPiIIBAACJyIPgIoP4Ag+FdAEAAESJZdAPV8AP\n" +
                        "KUWQDylFgA8phXD///8PKYVg////DymFUP///w8phUD///8PKYUw////DymFIP///w8phRD///9E\n" +
                        "iftIxwQkAAAAAEiNlRD///+/BQAAAEiJ3jHJRTHARTHJZ+hE/f//SD0A8P//dh1JicT/FTsHAABB\n" +
                        "99xEiSCwAUUx7USLZdDp4AAAAEiLBRgHAABIOYUQ////dTFIhcB0LEiDvUD///8AdSJEif/oCP7/\n" +
                        "/0g9APD//3cSSImFQP///0jHhRj///8AAAAASIsF1wYAAEg5hRD///8PlMFIhcBBD5XFQSDNsAFE\n" +
                        "i2XQQfbEBHR4D1fADymF8P7//w8pheD+//8PKYXQ/v//DymFwP7//w8phbD+//8PKYWg/v//DymF\n" +
                        "kP7//0jHhQD///8AAAAASMcEJAAAAABIjZWQ/v//v4oAAABIid4xyUUxwEUxyWfoU/z//4nAuZQZ\n" +
                        "AgFIM42Q/v//SAnBD5XARInr9tMgw/8VOgYAAEyJ8UjB4TR0IscAFgAAAEnHxP////9MieBIgcRY\n" +
                        "AQAAW0FcQV1BXkFfXcNIiUXAQo0EbQAAAAAPtsBECeBIY8hEifhEi320TWPHTGPITIk0JL8JAAAA\n" +
                        "SIt1uEiLVchMiUXQTIlNqGfoxvv//0SJ50mJxEg9AfD//3JFMcBBg/zzD5XAMclB9scCD5TBRIn6\n" +
                        "weoFg+IBMfZA9scEQA+UxgnWD7bTCcoJ8gnCD4S0AAAAQffcSItFwESJIOlQ////iX3QRYTtD4RL\n" +
                        "////SIN9yAB0VkyNLVn7//9MieNMi33I6w5JKcdJAcZIAcNNhf90OEmB/wAQAAC5ABAAAEkPQs9I\n" +
                        "xwQkAAAAAL8RAAAASIt1qEiJ2k2J8EUxyUH/1UiFwH/ASIP4/HTDSIsVEwUAAEiLRchIAdBI/8hI\n" +
                        "99pIIcJIY03QSMcEJAAAAAC/CgAAAEyJ5kUxwEUxyWfo0/r//+my/v//RIn4qAIPlMAI2EyLZch1\n" +
                        "MIn7ifiD4PtIY8hMiTQkvwkAAABIi3W4TIniTItF0EyLTahn6Jb6//9IPQDw//92D0iLRcDHAA0A\n" +
                        "AADpXP7//0jHBCQAAAAAvwsAAABIicZMieIxyUUxwEUxyWfoXvr//z0B8P//c0eJXdCJ2IPIAkGD\n" +
                        "zyBIY8hNY8dIxwQkAAAAAL8JAAAASIt1uEyJ4knHwf////9n6CT6//9JicRIPQHw//8Pg5P+///p\n" +
                        "qf7//w8LZmZmZmYuDx+EAAAAAABVSInlSIPsEEiJ8khj90jHBCQAAAAAv4oAAAAxyUUxwEUxyWfo\n" +
                        "1/n//0iDxBBdw5BVSInlU1BIifBIif5MY9JMY9lJY9hMiQwkvwkAAABIicJMidFNidhJidln6KD5\n" +
                        "//9Ig8QIW13DZg8fhAAAAAAAVUiJ5UiD7BBJichIidFIifJIY/dIxwQkAAAAAL8RAAAARTHJZ+hm\n" +
                        "+f//SIPEEF3DVUiJ5UiD7BBIifBIif5IY8pIxwQkAAAAAL8KAAAASInCRTHARTHJZ+gz+f//SIPE\n" +
                        "EF3DZmZmZi4PH4QAAAAAAFVIieVd6Sb7//9mDx9EAABVSInlSIPsEEiJ8kiJ/kjHBCQAAAAAvwsA\n" +
                        "AAAxyUUxwEUxyWfo5/j//0iDxBBdw5BVSInlSIPsEEiJ0UiJ8khj90jHBCQAAAAAMf9FMcBFMcln\n" +
                        "6Ln4//9Ig8QQXcMPHwBVSInlSIPsEEiJ0UiJ8khj90jHBCQAAAAAvwEAAABFMcBFMcln6Ib4//9I\n" +
                        "g8QQXcNVSInlSIPsEEiJ8Ehj90xjykGJyEjHBCQAAAAAvwEBAABIicJMiclFMcln6FD4//9Ig8QQ\n" +
                        "XcNmLg8fhAAAAAAAVUiJ5UiD7BBIidBIifJIY/dMY8FIxwQkAAAAAL8GAQAASInBRTHJZ+gT+P//\n" +
                        "SIPEEF3DZmZmZi4PH4QAAAAAAFVIieVIg+wQSInxSIn6SMcEJAAAAAC/BgEAAEjHxpz///9FMcBF\n" +
                        "Mcln6NL3//9Ig8QQXcNmZmYuDx+EAAAAAABVSInlSIPsEEiJ8khj90jHBCQAAAAAvwUAAAAxyUUx\n" +
                        "wEUxyWfol/f//0iDxBBdw5BVSInlSIPsEEhj90jHBCQAAAAAvwMAAAAx0jHJRTHARTHJZ+ho9///\n" +
                        "SIPEEF3DZpBVSInlSIPsEEiJ0UiJ8khj90jHBCQAAAAAvxAAAABFMcBFMcln6Db3//9Ig8QQXcNV\n" +
                        "SInlSIPsEEhj90jHBCQAAAAAv+cAAAAx0jHJRTHARTHJZ+gI9///Dx+EAAAAAABVSInlDwtmLg8f\n" +
                        "hAAAAAAASIn4SIXSD4TiAAAAQIgwQIh0EP9Ig/oDD4LQAAAAQIhwAUCIcAJAiHQQ/kCIdBD9SIP6\n" +
                        "Bw+CtAAAAECIcANAiHQQ/EiD+gkPgqEAAACJx/ffg+cDSI0MOEgp+kiD4vxAD7b2afYBAQEBiTQ4\n" +
                        "iXQK/EiD+glyeIlxBIlxCIl0EfSJdBH4SIP6GXJkZg9uxmYPcMAA8w9/QQzzD39EEeSJz4PnBEiD\n" +
                        "zxhIKfpIg/ogcj5VSInlQYnwTInGSMHmIEwJxkgB+WZmZi4PH4QAAAAAAEiJMUiJcQhIiXEQSIlx\n" +
                        "GEiDwuBIg8EgSIP6H3fjXcMA776v3gAAAAAURREAAAAAAAAQAAAAAAAA\n";
        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        int hookInfoOffset = 0x0a50;
        fillInHookInfo(bytes, hookInfoOffset);
        return bytes;
    }

    @Override
    public int getNativeDebugBreakOffset() {
        return 0x0000;
    }

    @Override
    public int getNativeClearCacheOffset() {
        return 0x0090;
    }

    @Override
    public int getNativeSyscallOffset() {
        return 0x0010;
    }

    @Override
    public int getNativeCallPointerFunction0Offset() {
        return 0x00b0;
    }

    @Override
    public int getNativeCallPointerFunction1Offset() {
        return 0x00c0;
    }

    @Override
    public int getNativeCallPointerFunction2Offset() {
        return 0x00d0;
    }

    @Override
    public int getNativeCallPointerFunction3Offset() {
        return 0x00e0;
    }

    @Override
    public int getNativeCallPointerFunction4Offset() {
        return 0x0100;
    }

    @Override
    public int getNativeGetJavaVmOffset() {
        return 0x0120;
    }

    @Override
    public int getFakeStat64Offset() {
        return 0x01c0;
    }

    @Override
    public int getFakeMmap64Offset() {
        return 0x0260;
    }

    @Override
    public int getFakeMmapOffset() {
        return 0x0730;
    }

    @Override
    public int __NR_mprotect() {
        // mprotect x86_64: 10
        return 10;
    }

    @Override
    public int __NR_memfd_create() {
        // memfd_create x86_64: 319
        return 319;
    }

    @Override
    public int __NR_ioctl() {
        // ioctl x86_64: 16
        return 16;
    }

    @Override
    public int __NR_tgkill() {
        // tgkill x86_64: 234
        return 234;
    }

    @Override
    public void inlineHook(long address, long hook) {
        if (address == 0) {
            throw new IllegalArgumentException("address is 0");
        }
        if (hook == 0) {
            throw new IllegalArgumentException("hook is 0");
        }
        // 49 BA [8bytes]  movabs r10, hook
        // 41 FF E2  jmp r10
        byte[] stub = new byte[2 + 8 + 3];
        stub[0] = 0x49;
        stub[1] = (byte) 0xba;
        ByteArrayUtils.writeInt64(stub, 2, hook);
        stub[10] = 0x41;
        stub[11] = (byte) 0xff;
        stub[12] = (byte) 0xe2;
        writeByteArrayToTextSection(stub, address);
    }

}
