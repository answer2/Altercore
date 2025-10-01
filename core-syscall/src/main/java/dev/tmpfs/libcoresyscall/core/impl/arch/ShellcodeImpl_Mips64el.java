package dev.tmpfs.libcoresyscall.core.impl.arch;

import dev.tmpfs.libcoresyscall.core.impl.trampoline.BaseShellcode;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISimpleInlineHook;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISyscallNumberTable;

public class ShellcodeImpl_Mips64el extends BaseShellcode implements ISimpleInlineHook, ISyscallNumberTable {

    public static final ShellcodeImpl_Mips64el INSTANCE = new ShellcodeImpl_Mips64el();

    private ShellcodeImpl_Mips64el() {
        super();
    }

    @Override
    public byte[] getShellcodeBytes() {
        //0000 g     F .text  000c NativeBridge_breakpoint
        //0000 g       .text  0000 _ftext
        //0000 g       .text  0000 ___text_dummy
        //0000 l    d  .text  0000 .text
        //000c g     F .text  0018 __clear_cache
        //0024 g     F .text  0058 syscall_ext
        //007c g     F .text  0068 NativeBridge_nativeSyscall
        //00e4 g     F .text  0018 NativeBridge_nativeClearCache
        //00fc g     F .text  0034 NativeBridge_nativeCallPointerFunction0
        //0130 g     F .text  0038 NativeBridge_nativeCallPointerFunction1
        //0168 g     F .text  003c NativeBridge_nativeCallPointerFunction2
        //01a4 g     F .text  0044 NativeBridge_nativeCallPointerFunction3
        //01e8 g     F .text  0048 NativeBridge_nativeCallPointerFunction4
        //0230 g     F .text  0048 NativeBridge_nativeGetJavaVM
        //0278 g     F .text  0018 get_hook_info
        //0290 g     F .text  0068 lsw_pread64
        //02f8 g     F .text  0060 lsw_mprotect
        //0358 g     F .text  013c fake_fstat64
        //0494 g     F .text  034c fake_mmap64
        //07e0 g     F .text  0050 fake_mmap
        //0830  w    F .text  0118 memset
        //0950 l     O .text  0018 get_hook_info.sHookInfo
        String b64 =
                "DQAAAAgA4AMAAAAAOwgCfAMAQBQAAAAACADgAwAAAAANAAAA+P+9JwQAsK8YAKOPJQiAACAAvI8c\n" +
                        "ALCPJSCgACUowAAlMOAAJRAgACU4YADg/70nEACwrxQAvK8MAAAAIAC9JwQAsI8jCAIACghHACUQ\n" +
                        "IAAIAOADCAC9JwEAAjwUcEIk2P+9JyQAv68gAL6vJfCgAyHgWQBgAMSPUADCj1gAw49AAMGPSADH\n" +
                        "jzgAxY8YAKSvFACjrxAAoq8lIMAAGICZjwn4IAMlMCAAAAADJCXowAMgAL6PJAC/jwgA4AMoAL0n\n" +
                        "OwgCfAMAQBQAAAAACADgAwAAAAANAAAA6P+9JxQAv68QAL6vJfCgAyXIwAAJ+CADAAAAAAAAAyQl\n" +
                        "6MADEAC+jxQAv48IAOADGAC9J+j/vScUAL+vEAC+ryXwoAMoAMSPJcjAAAn4IAMAAAAAAAADJCXo\n" +
                        "wAMQAL6PFAC/jwgA4AMYAL0n6P+9JxQAv68QAL6vJfCgAzAAxY8oAMSPJcjAAAn4IAMAAAAAAAAD\n" +
                        "JCXowAMQAL6PFAC/jwgA4AMYAL0n6P+9JxQAv68QAL6vJfCgAyUIwAA4AMaPMADFjygAxI8lyCAA\n" +
                        "CfggAwAAAAAAAAMkJejAAxAAvo8UAL+PCADgAxgAvSfo/70nFAC/rxAAvq8l8KADJQjAAEAAx484\n" +
                        "AMaPMADFjygAxI8lyCAACfggAwAAAAAAAAMkJejAAxAAvo8UAL+PCADgAxgAvSfg/70nHAC/rxgA\n" +
                        "vq8l8KADFADArwAAgYxsAzmMCfggAxQAxScUAMGPAAADJAsIAgAlECAAJejAAxgAvo8cAL+PCADg\n" +
                        "AyAAvScBAAI8GG5CJCEIWQAcgCGMCADgA1AZIiQBAAI8AG5CJNj/vSckAL+vIAC+ryXwoAMh4FkA\n" +
                        "JRCgACUYgAA8AMWPOADEjyUIwAAlMEAAJTggABQApa8QAKSvGACgr2gQBCQYgJmPCfggAyUoYAAl\n" +
                        "6MADIAC+jyQAv48IAOADKAC9JwEAAjyYbUIk2P+9JyQAv68gAL6vJfCgAyHgWQAYAKCvFACgrxAA\n" +
                        "oK8lCMAAJRCgACUYgAAdEAQkGICZjyUoYAAlMEAACfggAyU4IAAl6MADIAC+jyQAv48IAOADKAC9\n" +
                        "JwEAAjw4bUIkyP+9JzQAv68wAL6vLACzrygAsq8kALGvIACwryXwoAMhkFkAGACgrxQAoK8QAKCv\n" +
                        "JYCgACWYgAB3EAQkAAAHJAAAESQYgFmOJShgAiUwAAIJ+CADJeBAAgHwQSwJACAUAAAAAByAQY5Q\n" +
                        "GSEkCAA5jAn4IAMjgAIAAABQrCMAABD//xEkHIBDjgAAAY5QGWKMUBljJAQAY4wmCEEAJQgjABoA\n" +
                        "IBQAAAAAJQhDABcAIBAAAAAAPAABjjgAAo4lCEEAEgAgFAAAAAAYAKCvFACgrxAAoK8AIAE81g8E\n" +
                        "JCUoYAIAAAckAAARJBiAWY4J+CADBHcmNAHwQSwEACAQAAAAAMMPAgA4AAKuPAABriUQIAIl6MAD\n" +
                        "IACwjyQAsY8oALKPLACzjzAAvo80AL+PCADgAzgAvScBAAI8/GtCJCj/vSfUAL+v0AC+r8wAt6/I\n" +
                        "ALavxAC1r8AAtK+8ALOvuACyr7QAsa+wALCvJfCgAyGIWQDwAMKP9ADBj+gA148AABAkNADFrzwA\n" +
                        "xq8wAMSvLADHr0AAwq///wIkOADBrwII4TBEAMKvAgACJBoAIhQAABMkAAAUJAAAFiQ2AOAGJRDA\n" +
                        "AEgA0icggDmOAAAFJGgABiQl4CACAAATJAn4IAMlIEACJIA5jiUg4AIlKEACCfggAyXgIAL//wEk\n" +
                        "CgBAEEQAwa88AMKPAAAQJAAAFCQhAAAQAAAWJAAAFCQAABYkHQAAECUQwAAcgCGOSADDj1AZIoxQ\n" +
                        "GSEkJiBDAAQAI4wlCIMAPADEjwoAIBQAABAkJQhDAAwAIBAAAAAANADTj0AA1I84ANaPAwCCNAoA\n" +
                        "ABBEANevAAATJAAAFCQAABYkBQAAECUQgAAAABMkAAAUJAAAFiQlEIAAHIA1jigAwq9QGaEmCAA5\n" +
                        "jAn4IAMAAAAAQADDj/8PYTAFACAQJZBAABYAAST//xckXwAAEAAAQa44AMKPABBBLAUAIBQkANWv\n" +
                        "FgABJP//FyRXAAAQAABBrhQAt68AFQIAchAEJCwAwY8QAKGvAgsDACUIIgAYAKGvGIA5jjAAxY80\n" +
                        "AMaPKADHjwn4IAMl4CACJbhAAAHw4S4FACAUAAAAACMIFwD//xckQQAAEAAAQa5EAMKP//8BJD0A\n" +
                        "QRAAAAAAJQhwAiYAIBAAAAAA/P8SJCWo4AIAEGEuFAC2rxAAtK8AEAYkJSigAiXgIAILCBAAKIA5\n" +
                        "jkQAxI8J+CADCzBhAhMAQBgAAAAAIaCCAisgYgLDLwIAIwhiAiGoVQArGIICISCFACWYIAAhEGUA\n" +
                        "IbDCAgEAAiQKAEAQI4AEAiUIcALk/yAUAAAAAAUAABAAAAAAJghSAAEAIiz4/0AUAAAAACQAwY80\n" +
                        "AMOPPADQjyyAOY4lIOACJeAgAlAZISQlMAACEAAhjCMQAQAhCGEA//8hJAn4IAMkKCIABAABMgQA\n" +
                        "IBAAAAAAOwgCfA8AQBQAAAAAJRDgAiXowAOwALCPtACxj7gAso+8ALOPwAC0j8QAtY/IALaPzAC3\n" +
                        "j9AAvo/UAL+PCADgA9gAvScNAAAAAQACPLBoQiTY/70nJAC/ryAAvq8l8KADIeBZADwAwo84AMGP\n" +
                        "GACirxAAoa8cAKCvMICZjwn4IAMAAAAAJejAAyAAvo8kAL+PCADgAygAvSdDAMAQAAAAACEQhgAD\n" +
                        "AMEsAACFoD4AIBT//0WgBwDBLAIAhaABAIWg/f9FoDgAIBT+/0WgCQDBLAMAhaA0ACAU/P9FoP8A\n" +
                        "ojAjCAQAACoCAAA8AgADACEwJSiiAAAWAgAhGIEAIwjBACUo5QAlEEUA/P8FJCQoJQAAAGKsITBl\n" +
                        "AAkAoSwiACAU/P/CrBkAoSwIAGKsBABirPj/wqwcACAU9P/CrAQAYTAQAGKsDABirBQAYqwYAGKs\n" +
                        "6P/CrOT/wqzs/8Ks8P/CrBgAJjQjKKYAIAChLA4AIBQAAAAAIRhmAOD/pSQIAGKsAABirBAAYqwY\n" +
                        "AGKsDABirAQAYqwUAGKsHABirCAAoSz1/yAQIABjJAgA4AMlEIAAAAAAAAAAAADvvq/eAAAAABRF\n" +
                        "EQAAAAAAAAAAAAAAAAA=";
        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        int hookInfoOffset = 0x0950;
        fillInHookInfo(bytes, hookInfoOffset);
        return bytes;
    }

    @Override
    public int getNativeDebugBreakOffset() {
        return 0x0000;
    }

    @Override
    public int getNativeClearCacheOffset() {
        return 0x00e4;
    }

    @Override
    public int getNativeSyscallOffset() {
        return 0x007c;
    }

    @Override
    public int getNativeCallPointerFunction0Offset() {
        return 0x00fc;
    }

    @Override
    public int getNativeCallPointerFunction1Offset() {
        return 0x0130;
    }

    @Override
    public int getNativeCallPointerFunction2Offset() {
        return 0x0168;
    }

    @Override
    public int getNativeCallPointerFunction3Offset() {
        return 0x01a4;
    }

    @Override
    public int getNativeCallPointerFunction4Offset() {
        return 0x01e8;
    }

    @Override
    public int getNativeGetJavaVmOffset() {
        return 0x0230;
    }

    @Override
    public int getFakeStat64Offset() {
        return 0x0358;
    }

    @Override
    public int getFakeMmap64Offset() {
        return 0x0494;
    }

    @Override
    public int getFakeMmapOffset() {
        return 0x07e0;
    }

    @Override
    public int __NR_mprotect() {
        // mprotect mips64el n64 5000+10
        return 5010;
    }

    @Override
    public int __NR_memfd_create() {
        // memfd_create mips64el n64 5000+314
        return 5314;
    }

    @Override
    public int __NR_ioctl() {
        // ioctl mips64el n64 5000+15
        return 5015;
    }

    @Override
    public int __NR_tgkill() {
        // tgkill mips64el n64 5000+225
        return 5225;
    }

    @Override
    public void inlineHook(long address, long hook) {
        // TODO: 2024-11-29 implement inline hook for mips64 if anyone really needs it
        throw new UnsupportedOperationException("Not implemented");
    }

}
