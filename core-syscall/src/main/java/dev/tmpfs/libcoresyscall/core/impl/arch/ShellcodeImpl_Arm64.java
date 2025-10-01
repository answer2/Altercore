package dev.tmpfs.libcoresyscall.core.impl.arch;

import dev.tmpfs.libcoresyscall.core.impl.ByteArrayUtils;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.BaseShellcode;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISimpleInlineHook;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISyscallNumberTable;

public class ShellcodeImpl_Arm64 extends BaseShellcode implements ISimpleInlineHook, ISyscallNumberTable {

    public static final ShellcodeImpl_Arm64 INSTANCE = new ShellcodeImpl_Arm64();

    private ShellcodeImpl_Arm64() {
        super();
    }

    @Override
    public byte[] getShellcodeBytes() {
        //0000 g    DF .text  0008 NativeBridge_breakpoint
        //0000 g    D  .text  0000 ___text_section
        //0008 g    DF .text  0024 NativeBridge_nativeSyscall
        //002c g    DF .text  0028 syscall_ext
        //0054 g    DF .text  0064 NativeBridge_nativeClearCache
        //00b8 g    DF .text  0068 __clear_cache
        //0120 g    DF .text  0004 NativeBridge_nativeCallPointerFunction0
        //0124 g    DF .text  0008 NativeBridge_nativeCallPointerFunction1
        //012c g    DF .text  000c NativeBridge_nativeCallPointerFunction2
        //0138 g    DF .text  0014 NativeBridge_nativeCallPointerFunction3
        //014c g    DF .text  0018 NativeBridge_nativeCallPointerFunction4
        //0164 g    DF .text  0038 NativeBridge_nativeGetJavaVM
        //019c g    DF .text  0020 ashmem_dev_get_size_region
        //01bc g    DF .text  000c get_hook_info
        //01c8 g    DF .text  0014 get_current_pc
        //01dc g    DF .text  00ec fake_fstat64
        //02c8 g    DF .text  03d4 fake_mmap64
        //0754 g    DF .text  0004 fake_mmap
        //0a60 l     O .rodata  0018 _ZZ13get_hook_infoE9sHookInfo
        String b64 =
                "AAA+1MADX9boAwaq5gNA+eEDA6rgAwIq4gMEquMDBarkAwiq5QMHqgEAABQJfECT4AMBquEDAqri\n" +
                        "AwOq4wMEquQDBarlAwaq6AMJqgEAANTAA1/WaAACiykAO9VfAAjrYgIAVCpNEFOLAIBSaiHKGusD\n" +
                        "AqorewvVawEKi38BCOuj//9UKQ0AEooAgFKfOwPVSSHJGiJ1C9VCAAmLXwAI66P//1TfPwPVwANf\n" +
                        "1p87A9XfPwPVwANf1h8AAesoADvVogIAVAlNEFOKAIBSSSHJGuoDAKoqewvVSgEJi18BAeuj//9U\n" +
                        "HwAB6587A9UCAQBUCA0AEokAgFIoIcgaIHUL1QAACIsfAAHro///VN8/A9XAA1/WnzsD1d8/A9XA\n" +
                        "A1/WQAAf1uADA6pAAB/W4QMEquADA6pAAB/W4QMEquADA6rjAwKq4gMFqmAAH9bhAwSq4AMDquQD\n" +
                        "AqriAwWq4wMGqoAAH9b/gwDR/XsBqf1DAJEIAED54SMAkf8HAPkIbUP5AAE/1ugHQPkfAABxAAGf\n" +
                        "mv17Qan/gwCRwANf1gF8QJOC4I5S4wMfqqADgFLkAx+q5QMfquYDH6qd//8XHyAD1QBFABDAA1/W\n" +
                        "/g8f+P8gA9XgAx6q/gdB+MADX9b9e72p9QsA+fRPAqn9AwCR9AMAKvMDAaoACoBSiH5Ak+IDE6rj\n" +
                        "Ax+q5AMfquUDH6rmAx+q4QMIqob//5cf/D+xgwEAVAgAAJD1AwCqCDVF+QABP9boAxVLCAAAuQAA\n" +
                        "gBL0T0Kp9QtA+f17w6jAA1/WCAAAkGkCQPngAx8qCDFF+T8BCOvh/v9UyP7/tGgaQPnIAAC04AMf\n" +
                        "KvRPQqn1C0D5/XvDqMADX9bgAxQqxf//lx8EQLHJAABU4AMfKvRPQqn1C0D5/XvDqMADX9boAwCq\n" +
                        "4AMfKn8GAPloGgD59E9CqfULQPn9e8OowANf1v/DBdH9exGp/G8SqfpnE6n4XxSp9lcVqfRPFqn9\n" +
                        "QwSR9AMFqvgDBCr3AwMq8wMCKvUDAar2AwCq/AMfKjoAgFIbAACQBAn4N0gEgFLoAggKHwkAcYEI\n" +
                        "AFQA5ABv+QMYKuIDApEACoBS4QMZquMDH6rkAx+q5QMfquYDH6rgAwSt4AMFreADBq3gAwetNv//\n" +
                        "lx8EQLEpAQBUaDdF+foDAKoAAT/W6AMaS/wDHyoIAAC5KACAUisAABQaAACQ6UNA+UgzRfk/AQjr\n" +
                        "QQEAVCgBALToW0D56AAAteADGCp+//+XHwRAsWgAAFTgWwD5/0cA+ehDQPlJM0X5HwEJ6yQJQPr8\n" +
                        "B58acwAQNygAgFIVAAAUAOQAb+IDAJGABYBS4QMZquMDH6rkAx+q5QMfquYDH6r/OwD54IMAreCD\n" +
                        "Aa3ggwKt4AOAPQj//5foA0D5iTKDUh8AAHFJIKByAAFJ+ugHnxoaATwKaDdF+QABP9afLkDygAEA\n" +
                        "VMgCgFIIAAC5AACAkvRPVqn2V1Wp+F9UqfpnU6n8b1Kp/XtRqf/DBZHAA1/WnwMAcUgAgFL5fkCT\n" +
                        "CBGfGhh/QJP7AwCqCAETKsAbgFLhAxaqA31Ak+IDFarkAxmq5QMYquYDFKrj/v+XH/w/saMBAFTo\n" +
                        "fgVTHzQAMQgJcyoIBXcqCAEAEggBGioIBZ8aqAUANOgDAEsAAICSaAMAudz//xd8+/809gMAqtUC\n" +
                        "ALQZAIJS9wMWqvoDFaoFAAAUWgMAyxQAFIsXABeL2gEAtF8HQPFgCIBS4QMYqkMzmZriAxeq5AMU\n" +
                        "quUDH6rmAx+qwf7/lx8AAPFM/v9UHxAAsWD+/1QXAACQtAYA0WN+QJPoOkX5QByAUuEDFqrkAx+q\n" +
                        "5QMfquYDH6qJAgiL6AMIyyIBCIqw/v+XswIQN+ADFqq0//8X/wIfckgXnxqIAQA3aHodEsAbgFLh\n" +
                        "AxaqA31Ak+IDFarkAxmq5QMYquYDFKqh/v+XHwRAsWkEAFSoAYBSAACAkmgDALmi//8X6DpF+YkC\n" +
                        "FovgAxaq6gMIyykBCItIARaKKQEKiioAO9UfAQnrQgIAVEtNEFOMAIBSiyHLGuwDCKosewvVjAEL\n" +
                        "i58BCeuj//9USg0AEosAgFKfOwPVaiHKGih1C9UIAQqLHwEJ66P//1QCAAAUnzsD1d8/A9WE//8X\n" +
                        "4QMAquAagFLiAxWq4wMfquQDH6rlAx+q5gMfqnX+/5cf/D8xwgEAVGgCHzLpAhsywBuAUgN9QJMk\n" +
                        "fUCT4QMWquIDFaoFAICS5gMfqmn+/5cf/D+x4vH/VJP//xcgACDU/Xu/qf0DAJEIfECT4gMBquMD\n" +
                        "H6qABYBS5AMfquUDH6rhAwiq5gMfqlr+/5f9e8GowANf1kl8QJNqfECT5gMFqugDAaqFfECT4QMA\n" +
                        "qsAbgFLiAwiq4wMJquQDCqpN/v8XCHxAk+QDA6rjAwKqYAiAUuIDAarlAx+q4QMIquYDH6pE/v8X\n" +
                        "/Xu/qf0DAJHoAwGqQ3xAk+EDAKpAHIBS4gMIquQDH6rlAx+q5gMfqjn+/5f9e8GowANf1t3+/xf9\n" +
                        "e7+p/QMAkeIDAarhAwCq4BqAUuMDH6rkAx+q5QMfquYDH6os/v+X/XvBqMADX9YIfECT4wMCquID\n" +
                        "AargB4BS5AMfquUDH6rhAwiq5gMfqiH+/xcIfECT4wMCquIDAaoACIBS5AMfquUDH6rhAwiq5gMf\n" +
                        "qhj+/xf9e7+p/QMAkUl8QJPoAwGqAXxAk+QDAyoAB4BS4gMIquMDCarlAx+q5gMfqgz+/5f9e8Go\n" +
                        "wANf1v17v6n9AwCRCXxAk+gDAqpkfECT4gMBquAJgFLjAwiq4QMJquUDH6rmAx+q/v3/l/17wajA\n" +
                        "A1/W/Xu/qf0DAJHjAwGq4gMAquAJgFJhDICS5AMfquUDH6rmAx+q8v3/l/17wajAA1/W/Xu/qf0D\n" +
                        "AJEIfECT4gMBquMDH6oACoBS5AMfquUDH6rhAwiq5gMfquX9/5f9e8GowANf1v17v6n9AwCRAXxA\n" +
                        "k+IDH6rjAx+qIAeAUuQDH6rlAx+q5gMfqtn9/5f9e8GowANf1v17v6n9AwCRCHxAk+MDAqriAwGq\n" +
                        "oAOAUuQDH6rlAx+q4QMIquYDH6rM/f+X/XvBqMADX9b9e7+p/QMAkQF8QJPiAx+q4wMfqsALgFLk\n" +
                        "Ax+q5QMfquYDH6rA/f+XIAAg1CIJALQIAAKLXwwA8QEAADkB8R84gwgAVF8cAPEBBAA5AQgAOQHh\n" +
                        "HzgB0R84wwcAVF8kAPEBDAA5AcEfOEMHAFToAwBLKRwAEurDADILBUCSKX0KG0wAC8sKAAuLiPV+\n" +
                        "kk0BCIsfJQDxSQEAuanBH7ijBQBUH2UA8UmlACmppT4pIwUAVE4BfpIgDQROzwV9sggBD8sfgQDx\n" +
                        "QMGAPKBBnjwjBABUjAEOyymBCapKAQ+LjOEA0Z+BAfGjAgBUjP1F0yANCE7LAQuLawEAi4wFAJFr\n" +
                        "YQGRjeV+kq/pe9PuAw2qCAEPy0oBD4tgAT6tzhEA8WABP61gAQCtYAEBrWsBApFB//9UnwEN6+AA\n" +
                        "AFQIgQDRSSUAqR99APFJJQGpSoEAkWj//1TAA1/WAAAAAO++r94AAAAAFEURAAAAAAAAEAAAAAAA\n" +
                        "AA==\n";
        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        int hookInfoOffset = 0x0a60;
        fillInHookInfo(bytes, hookInfoOffset);
        return bytes;
    }

    @Override
    public int getNativeDebugBreakOffset() {
        return 0x0000;
    }

    @Override
    public int getNativeClearCacheOffset() {
        return 0x0054;
    }

    @Override
    public int getNativeSyscallOffset() {
        return 0x0008;
    }

    @Override
    public int getNativeCallPointerFunction0Offset() {
        return 0x0120;
    }

    @Override
    public int getNativeCallPointerFunction1Offset() {
        return 0x0124;
    }

    @Override
    public int getNativeCallPointerFunction2Offset() {
        return 0x012c;
    }

    @Override
    public int getNativeCallPointerFunction3Offset() {
        return 0x0138;
    }

    @Override
    public int getNativeCallPointerFunction4Offset() {
        return 0x014c;
    }

    @Override
    public int getNativeGetJavaVmOffset() {
        return 0x0164;
    }

    @Override
    public int getFakeStat64Offset() {
        return 0x01dc;
    }

    @Override
    public int getFakeMmap64Offset() {
        return 0x02c8;
    }

    @Override
    public int getFakeMmapOffset() {
        return 0x0754;
    }

    @Override
    public int __NR_mprotect() {
        // mprotect arm64: 226
        return 226;
    }

    @Override
    public int __NR_memfd_create() {
        // memfd_create arm64: 279
        return 279;
    }

    @Override
    public int __NR_ioctl() {
        // ioctl arm64: 29
        return 29;
    }

    @Override
    public int __NR_tgkill() {
        // tgkill arm64: 131
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
        if (address % 4 != 0 || hook % 4 != 0) {
            throw new IllegalArgumentException("address or hook is not aligned, address: " + address + ", hook: " + hook);
        }
        // 51 00 00 58  ldr x17, [pc, #8]
        // 20 02 1F D6  br x17
        // qword. address
        byte[] trampoline = {
                0x51, 0x00, 0x00, 0x58,
                0x20, 0x02, 0x1F, (byte) 0xD6,
                0, 0, 0, 0, 0, 0, 0, 0
        };
        ByteArrayUtils.writeInt64(trampoline, 8, hook);
        if (address % 8 != 0) {
            byte[] old = trampoline;
            // add a nop to align address
            // 1f 20 03 d5  nop
            byte[] aligned = new byte[old.length + 4];
            aligned[0] = 0x1f;
            aligned[1] = 0x20;
            aligned[2] = 0x03;
            aligned[3] = (byte) 0xd5;
            System.arraycopy(old, 0, aligned, 4, old.length);
            trampoline = aligned;
        }
        writeByteArrayToTextSection(trampoline, address);
    }

}
