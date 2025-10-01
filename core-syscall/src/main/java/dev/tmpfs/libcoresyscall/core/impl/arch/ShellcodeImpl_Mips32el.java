package dev.tmpfs.libcoresyscall.core.impl.arch;

import dev.tmpfs.libcoresyscall.core.impl.trampoline.BaseShellcode;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISimpleInlineHook;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISyscallNumberTable;

public class ShellcodeImpl_Mips32el extends BaseShellcode implements ISimpleInlineHook, ISyscallNumberTable {

    public static final ShellcodeImpl_Mips32el INSTANCE = new ShellcodeImpl_Mips32el();

    private ShellcodeImpl_Mips32el() {
        super();
    }

    @Override
    public byte[] getShellcodeBytes() {
        //0000 g     F .text  0008 NativeBridge_breakpoint
        //0000 g     O .text  0000 ___text_dummy
        //0000 g       .text  0000 _ftext
        //0008 g     F .text  0040 __clear_cache
        //0048 g     F .text  0038 syscall_ext
        //0080 g     F .text  0060 NativeBridge_nativeSyscall
        //00e0 g     F .text  0044 NativeBridge_nativeClearCache
        //0128 g     F .text  002c NativeBridge_nativeCallPointerFunction0
        //0158 g     F .text  0030 NativeBridge_nativeCallPointerFunction1
        //0188 g     F .text  0034 NativeBridge_nativeCallPointerFunction2
        //01c0 g     F .text  003c NativeBridge_nativeCallPointerFunction3
        //0200 g     F .text  0040 NativeBridge_nativeCallPointerFunction4
        //0240 g     F .text  0044 NativeBridge_nativeGetJavaVM
        //0288 g     F .text  0018 get_hook_info
        //02a0 g     F .text  0068 lsw_pread64
        //0308 g     F .text  006c lsw_mprotect
        //0378 g     F .text  0100 fake_fstat64
        //0478 g     F .text  02d4 fake_mmap64
        //0750 g     F .text  0040 fake_mmap
        //0790  w    F .text  010c memset
        //08a0 l     O .text  0018 get_hook_info.sHookInfo
        String b64 =
                "DQAAAAAAH9g7CAJ8DQBA2CsIhQAGACDYAAAAAAAAnwQtIIIAKwiFAPz/P/gAAAAADwAAAAMAIOwJ\n" +
                        "BCAAAAAAAC0IAAAAAB/YLQgAAS0QgAAtIKAALSjAAC0w4AAtQCABLUhAAS04IAAMAAAALwgCADUQ\n" +
                        "RwA3CCcACQDgAyUQQQDg/71nGAC//xAAvv8IALz/LfCgAwIAATwtIMAALSjgAC0wAAEtOCABLUBA\n" +
                        "AS1IYAEtCDkAcI88ZCAAwd8ggJnfCfggAy1QIAAt6MADCAC83xAAvt8YAL/fCQDgAyAAvWc7CAJ8\n" +
                        "DgBA2C0Y5gArCMMABgAg2AAAAAAAAN8ELTDCACsIwwD8/z/4AAAAAA8AAAADACDsCQQgAAAAAAAt\n" +
                        "CAAAAAAf2AAAAADw/71nCAC//wAAvv8t8KADLcjAAAAAGfgt6MADAAC+3wgAv98JAOADEAC9ZwAA\n" +
                        "AADw/71nCAC//wAAvv8t8KADLcjAAAn4IAMtIOAALejAAwAAvt8IAL/fCQDgAxAAvWfw/71nCAC/\n" +
                        "/wAAvv8t8KADLSDgAC3IwAAJ+CADLSgAAS3owAMAAL7fCAC/3wkA4AMQAL1nAAAAAPD/vWcIAL//\n" +
                        "AAC+/y3woAMtCMAALSDgAC0oAAEtyCAACfggAy0wIAEt6MADAAC+3wgAv98JAOADEAC9ZwAAAADw\n" +
                        "/71nCAC//wAAvv8t8KADLQjAAC0g4AAtKAABLTAgAS3IIAAJ+CADLThAAS3owAMAAL7fCAC/3wkA\n" +
                        "4AMQAL1n4P+9ZxgAv/8QAL7/LfCgAwgAwP8AAIHc2AY53An4IAMIAMVnAAgCAAgAwt81EEEALejA\n" +
                        "AxAAvt8YAL/fCQDgAyAAvWcAAAAAAgABPC0IOQBojSFkKIAh3AkA4AOgGCJk4P+9ZxgAv/8QAL7/\n" +
                        "CAC8/y3woAMCAAE8LRigAC0QwAAtKIAAmBMEZAAACWQAAApkLQg5AC0wYABQjTxkLQjgAC04QAAg\n" +
                        "gJnfCfggAy1AIAAt6MADCAC83xAAvt8YAL/fCQDgAyAAvWfg/71nGAC//xAAvv8IALz/LfCgAwIA\n" +
                        "ATwtGIAALRCgAJITBGQAAAhkAAAJZAAACmQtCDkALShgAOiMPGQtCMAALTBAACCAmd8J+CADLTgg\n" +
                        "AAAQAgAt6MADCAC83xAAvt8YAL/fCQDgAyAAvWcAAAAA0P+9ZygAv/8gAL7/GAC8/xAAsv8IALH/\n" +
                        "AACw/y3woAMCAAE8LYCgAC2QgACNEwRkAAAHZAAACGQAAAlkAAAKZC0IOQAtKEACeIw8ZCCAmd8J\n" +
                        "+CADLTAAAgHwQSwJACD4AAgCACOAAQAogIHfoBghZAgAOdwAABn4AABQrBYAABD//xEkKICB36AY\n" +
                        "ItwAAAGeEQBBFAAAESQPAEDYOAAB3g0AIPgAIAE8IICZ35cTBGQtKEACAAAHZAAACGQAAAlkAAAK\n" +
                        "ZAn4IAMEdyY0AfBBLAEAINg4AAL+LRAgAi3owAMAALDfCACx3xAAst8YALzfIAC+3ygAv98JAOAD\n" +
                        "MAC9ZxD/vWfoAL//4AC+/9gAvP/QALf/yAC2/8AAtf+4ALT/sACz/6gAsv+gALH/mACw/y3woAMC\n" +
                        "AAE8LbDgAC2QIAEtqAABLbjAAP//AyQAABFkAgACJCgAxf8tCDkAeIs8ZAIIwTInACIUIADE/wAA\n" +
                        "E2QmAKAGLYDgAjAA02cwgJnfAAAFZGgABmQAABFkCfggAy0gYAI4gJnfACAVAAn4IAMtKGAC//8D\n" +
                        "JAAAE2QXAEAULYDgAiiAgd8wAMKfoBgh3CYQIgABACEsKxACACUIQQADAOI2NRBBADcY4QIlgGIA\n" +
                        "//8DJDUQoQI3GGEAAAgBACUYYgAoAMLfNZhBAgMAABA1iEEAAAATZC2A4AIogJTfHADDrxAA1/+g\n" +
                        "GIFmCAA53AAAGfj/D0EyBQAgEC24QAAWAAEk//8SZFcAABAAAOGuIICZ3yAAxd8oAMbfADgQAABA\n" +
                        "FgAASBUAkRMEZC1QQAIJ+CADCADU/y2QQAAB8EEuBQAg+AAIEgD//xJkIwgBAEUAABAAAOGuHADC\n" +
                        "j///ASRBACIgCADQ3xsAINoAEBZkAKACAPz/F2QtqEACABAhLkCAmd8tIIACLSigAi04YAIACAEA\n" +
                        "NRDBAjcIIQIJ+CADJTAiAAkAAlgtqFUALZhTAC+IIgIBAAIkBwBA2AAAAADu/z/6AAAAAAMAAMgm\n" +
                        "CFcAAQAiLPr/X/igGBBmKADD3xAA099IgJnfLSBAAhAAAd7//3FkADATAC8QAQAtCCECCfggAyQo\n" +
                        "IgAEAGEyFgAg2AAAAAA7CAJ8EwBA2BAAAd4tGDICLyABAC0IYQAkGCQAJCBEAisIgwAGACDYAAAA\n" +
                        "AAAAnwQtIIIAKwiDAPz/P/gAAAAADwAAAAMAIOwJBCAAAAAAAC0IAAAtEEACLejAA5gAsN+gALHf\n" +
                        "qACy37AAs9+4ALTfwAC138gAtt/QALff2AC83+AAvt/oAL/fCQDgA/AAvWcAAAAA4P+9ZxgAv/8Q\n" +
                        "AL7/CAC8/y3woAMCAAE8LQg5AKCIPGRQgJnfAAAZ+C3owAMIALzfEAC+3xgAv98JAOADIAC9Z0AA\n" +
                        "wNgtEIYAAwDBLAAAhaA8ACAU//9FoAcAwSwCAIWgAQCFoP3/RaA2ACAU/v9FoAkAwSwDAIWgMgAg\n" +
                        "FPz/RaD/AKMwLwgEAAAqAwAAPAMAAwAhMCUoowAAHgMALRCBAC8IwQAlKOUAJRhlAPz/BWQkKCUA\n" +
                        "AABDrC0wRQAJAKEsIAAgFPz/w6wZAKEsCABDrAQAQ6z4/8OsGgAgFPT/w6wEAEEwEABDrAwAQ6wU\n" +
                        "AEOsGABDrOj/w6zk/8Os7P/DrPD/w6wYACY0LyimACAAoSwMACD4A/hhfC0QRgA8GAEAJRhhAOD/\n" +
                        "pWQIAEP8AABD/BAAQ/wYAEP8IAChLPn/IBAgAEJkCQDgAy0QgAAAAAAA776v3gAAAAAURREAAAAA\n" +
                        "AAAAAAAAAAAAAAAAAAAAAAA=";
        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        int hookInfoOffset = 0x08a0;
        fillInHookInfo(bytes, hookInfoOffset);
        return bytes;
    }

    @Override
    public int getNativeDebugBreakOffset() {
        return 0x0000;
    }

    @Override
    public int getNativeClearCacheOffset() {
        return 0x00e0;
    }

    @Override
    public int getNativeSyscallOffset() {
        return 0x0080;
    }

    @Override
    public int getNativeCallPointerFunction0Offset() {
        return 0x0128;
    }

    @Override
    public int getNativeCallPointerFunction1Offset() {
        return 0x0158;
    }

    @Override
    public int getNativeCallPointerFunction2Offset() {
        return 0x0188;
    }

    @Override
    public int getNativeCallPointerFunction3Offset() {
        return 0x01c0;
    }

    @Override
    public int getNativeCallPointerFunction4Offset() {
        return 0x0200;
    }

    @Override
    public int getNativeGetJavaVmOffset() {
        return 0x0240;
    }

    @Override
    public int getFakeStat64Offset() {
        return 0x0378;
    }

    @Override
    public int getFakeMmap64Offset() {
        return 0x0478;
    }

    @Override
    public int getFakeMmapOffset() {
        return 0x0750;
    }

    @Override
    public int __NR_mprotect() {
        // mprotect mipsel o32 4000+125
        return 4125;
    }

    @Override
    public int __NR_memfd_create() {
        // memfd_create mipsel o32 4000+354
        return 4354;
    }

    @Override
    public int __NR_ioctl() {
        // ioctl mipsel o32 4000+54
        return 4054;
    }

    @Override
    public int __NR_tgkill() {
        // tgkill mipsel o32 4000+266
        return 4266;
    }

    @Override
    public void inlineHook(long address, long hook) {
        // TODO: 2024-11-29 implement inline hook for mips if anyone really needs it
        throw new UnsupportedOperationException("Not implemented");
    }

}
