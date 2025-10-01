package dev.tmpfs.libcoresyscall.core.impl.arch;

import dev.tmpfs.libcoresyscall.core.impl.ByteArrayUtils;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.BaseShellcode;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISimpleInlineHook;
import dev.tmpfs.libcoresyscall.core.impl.trampoline.ISyscallNumberTable;

public class ShellcodeImpl_Arm32 extends BaseShellcode implements ISimpleInlineHook, ISyscallNumberTable {

    public static final ShellcodeImpl_Arm32 INSTANCE = new ShellcodeImpl_Arm32();

    private ShellcodeImpl_Arm32() {
        super();
    }

    @Override
    public byte[] getShellcodeBytes() {
        //0000 g    DF .text  0008 NativeBridge_breakpoint
        //0000 g    D  .text  0000 ___text_section
        //0008 g    DF .text  0040 NativeBridge_nativeSyscall
        //0048 g    DF .text  0038 syscall_ext
        //0080 g    DF .text  0030 NativeBridge_nativeClearCache
        //00b0 g    DF .text  0020 __clear_cache
        //00d0 g    DF .text  0014 NativeBridge_nativeCallPointerFunction0
        //00e4 g    DF .text  0018 NativeBridge_nativeCallPointerFunction1
        //00fc g    DF .text  001c NativeBridge_nativeCallPointerFunction2
        //0118 g    DF .text  0024 NativeBridge_nativeCallPointerFunction3
        //013c g    DF .text  0028 NativeBridge_nativeCallPointerFunction4
        //0164 g    DF .text  0040 NativeBridge_nativeGetJavaVM
        //01a4 g    DF .text  0038 ashmem_dev_get_size_region
        //01dc g    DF .text  0010 get_hook_info
        //01ec g    DF .text  0008 get_current_pc
        //01f4 g    DF .text  00ec fake_fstat64
        //02e0 g    DF .text  0494 fake_mmap64
        //0878 g    DF .text  002c fake_mmap
        //0bf0 l     O .rodata  0018 _ZZ13get_hook_infoE9sHookInfo
        String b64 =
                "cAAg4R7/L+EQTC3pCLCN4hDQTeICAKDhCBCb5RAgm+UYMJvlIMCb5TBAm+Uo4JvlAFCN6AhAjeUC\n" +
                        "AADrABCg4wjQS+IQjL3oMEgt6QiwjeIIUIviA+Cg4QDAoOEBAKDhAhCg4TgAlegOIKDhBHAt5Qxw\n" +
                        "oOEAAADvBHCd5DCIveiATC3pCLCN4ggQm+UCcADjAgCg4Q9wQOMCEIHgACCg4wAAAO8AAFDjgIy9\n" +
                        "CP7e/+eAQC3pAnAA4wAgoOMPcEDjAAAA7wAAUOOAgL0I/t7/5wBILekNsKDhMv8v4QAQoOMAiL3o\n" +
                        "AEgt6Q2woOEIAJvlMv8v4QAQoOMAiL3oAEgt6Q2woOEIAJvlEBCb5TL/L+EAEKDjAIi96ABILekN\n" +
                        "sKDhAjCg4QgAm+UQEJvlGCCb5TP/L+EAEKDjAIi96ABILekNsKDhAsCg4QgAm+UQEJvlGCCb5SAw\n" +
                        "m+U8/y/hABCg4wCIvegQTC3pCLCN4gjQTeIAEJDlAECg4wRAjeVsI5HlBBCN4jL/L+EEEJ3lAABQ\n" +
                        "4wQQoBEBAKDhABCg4wjQS+IQjL3oAEgt6Q2woOEQ0E3iABCg4QAAoOMAAI3lBCcH4wQAjeUAMKDj\n" +
                        "CACN5TYAoOOc///rC9Cg4QCIvegEAJ/lAACP4B7/L+EICgAADgCg4R7/L+HwTS3pGLCN4hDQTeIB\n" +
                        "QKDhAFCg4QBwoOPFAKDjBRCg4QQgoOEAMKDjAHCN5QRwjeUIcI3lhv//6wEKcOMKAACaAGCg4ZQA\n" +
                        "n+UAAI/gCACQ5TD/L+EAEGbiABCA5QBw4OMHAKDhGNBL4vCNvehwAJ/l0CDE4QAAj+AEAJDlZBCf\n" +
                        "5QAwI+ABEJ/nASAi4AMgkuHy//8aAACR4fD//wowYITi0ADG4QEAkOHs//8aAIAP4wUAoOH/j0/j\n" +
                        "vP//6wgAUOHm//+KYBCE4oEAhugAcIHlBHCB5QcAoOEY0Evi8I296KwJAACACQAAcAkAAPBPLekc\n" +
                        "sI3i3NBN4hAAjeUDUKDhFBCN5QKgoOFgZJ/lAJCg4wgAm+UBcKDjEICb5QZgj+AAAFDjYwAASiIA\n" +
                        "BeICAFDjYAAAGnBwjeJQAMDyCBCb5QAwoOMHAKDhByCg4c0KQPTNCkD0zQpA9M0KQPTNCkD0zQpA\n" +
                        "9ACQgOXFAKDj1JCN5QCQjeUEkI3lCJCN5TT//+sBCnDjBgAAmgBAoOEIAJblMP8v4QAQZOIAEIDl\n" +
                        "AQCg40MAAOpwIJ3ldDCd5QQAluW8E5/lADAj4AEQn+cBICLgAyCS4RIAABoAAJHhEAAACjBAh+LQ\n" +
                        "AMThAQCQ4QwAABoIAJvlBpCg4QBgD+P/b0/jbv//6wYAUOEJYKDhBAAAimAQh+IAMKDjCQCE6AAw\n" +
                        "geUEMIHlcACd5XQQneUEIJblUDOf5QIQIeADMJ/nAwAg4AEAgOECEJPhEA9v4QEQABMEABrjoAKg\n" +
                        "4QGQAOABAKDjFwAAChggjeJQAMDyABCg4wAwoOMCAKDhbBCN5c0KQPTNCkD0zQpA9M0KQPTNCkD0\n" +
                        "ABCA5QsBAOMAEI3lBBCN5QgQjeUIEJvl7f7/6xgQneWUKQHjAiFA4wIQIeABAJDhAQAAEwlwwOEI\n" +
                        "AJblMP8v4f8fAOMWYKDjAQAY4SoAABoUQJvlShDg4wygjeUkprDhEQAAGggQm+UAYKDhDDCd5SgG\n" +
                        "oOEEEI3lAABZ4xAQneUECoDhFCCd5QIwgxMIAI3lwACg4wBQjeXN/v/rABCg4QEKcOMZAACaBgCg\n" +
                        "4QwgneWlMODhDQBx4yIh4OGlIoLhAyCC4QEgAuIHIILhASAAEwAAUuMBAAAKAGBh4gYAAOoAAFrj\n" +
                        "DWCg4wIQBQIBIKADoRAiAAcQkQE9AAAKAGCA5QAQ4OMBAKDhHNBL4vCPvegAAFnj+v//ChQAneUQ\n" +
                        "EI3lAABQ4xsAAAoQcJ3lAFCg4xSgneUBmqDjAGCg4wYAAOoAgJjgB3CA4ABApOIAoFrgAGDG4gYA\n" +
                        "muEOAAAKAQpa4wE6oOMIEJvlCjCgMQAAVuO0AKDjCTCgEQcgoOEgAY3oCECN5ZL+/+sAAFDj6///\n" +
                        "ygQAcOPu//8KXGGf5QAQoOMAEI3lBmCP4AQQjeUIEI3lFBCd5RAAluUBQEHiDFCd5QAQhOAAAGDi\n" +
                        "ACAB4BAQneV9AKDjBTCg4X3+/+sEABXjMwAAGhAQneUBAKDhHNBL4vCPvegoFqDhCCCb5QQageEM\n" +
                        "cJ3lBCCN5QCgoOEIEI3lBDDH4xAQneXAAKDjFCCd5QBQjeVq/v/rABCg4QoAoOEBCnHjsP//ihSQ\n" +
                        "neUAYKDjWwCg4wAwoOMAYI3lCSCg4QRgjeUIYI3lXf7/6wAQD+P/H0/jARCB4gEAUOEeAAAqIBCF\n" +
                        "4wAQjeUQEJ3lAADg4wIwh+NBAI3pwACg4wkgoOFP/v/rABCg4QAAD+P/D0/jAABR4Zv//5oAYGHi\n" +
                        "CgCg4ZH//+oQMJ3lEBCW5QNwhOAAIGHiARCH4AJwAOMDAALgAhAB4A9wQOMAIKDjAAAA7wMQoOEA\n" +
                        "AFDjhf//Cv7e/+fYCAAAPAgAAMwHAADQBQAAAEgt6Q2woOEQ0E3iASCg4QAQoOEAAKDjADCg4wAA\n" +
                        "jeUEAI3lCACN5QsBAOMo/v/rC9Cg4QCIvegQTC3pCLCN4hDQTeICwKDhFCCb5QHgoOEAEKDhIgaw\n" +
                        "4UoA4OMJAAAaEACb5QhAm+UYAI3oDDCg4SAGoOECCoDhCACN5cAAoOMOIKDhEv7/6wjQS+IQjL3o\n" +
                        "AEgt6Q2woOEQ0E3iAjCg4QEgoOEAEKDhAACg4wjgm+UMwJvlAUCN6LQAoOMIwI3lA/7/6wvQoOEA\n" +
                        "iL3oAEgt6Q2woOEQ0E3iAjCg4QEgoOEAEKDhAACg4wAAjeUEAI3lCACN5X0AoOP1/f/rC9Cg4QCI\n" +
                        "vegwSC3pCLCN4hDQTeIIwJvlAFCg4wxAm+X4QM3hAMCN5ZD+/+sI0EviMIi96ABILekNsKDhENBN\n" +
                        "4gEgoOEAEKDhAACg4wAwoOMAAI3lBACN5QgAjeVbAKDj3P3/6wvQoOEAiL3oAEgt6Q2woOEQ0E3i\n" +
                        "AjCg4QEgoOEAEKDhAACg4wAAjeUEAI3lCACN5QMAoOPO/f/rC9Cg4QCIvegASC3pDbCg4RDQTeIC\n" +
                        "MKDhASCg4QAQoOEAAKDjAACN5QQAjeUIAI3lBACg48D9/+sL0KDhAIi96ABILekNsKDhENBN4gLA\n" +
                        "oOEBIKDhABCg4QAAoOMAMI3lDDCg4QQAjeUIAI3lQgEA47H9/+sL0KDhAIi96ABILekNsKDhENBN\n" +
                        "4gLAoOEBIKDhABCg4QAAoOMAMI3lDDCg4QQAjeUIAI3lRwEA46L9/+sL0KDhAIi96ABILekNsKDh\n" +
                        "ENBN4gAgoOEAAKDjATCg4QAAjeUEAI3lYxDg4wgAjeVHAQDjlP3/6wvQoOEAiL3oAEgt6Q2woOEQ\n" +
                        "0E3iASCg4QAQoOEAAKDjADCg4wAAjeUEAI3lCACN5cUAoOOG/f/rC9Cg4QCIvegASC3pDbCg4RDQ\n" +
                        "TeIAEKDhAACg4wAAjeUAIKDjBACN5QAwoOMIAI3lBgCg43j9/+sL0KDhAIi96ABILekNsKDhENBN\n" +
                        "4gIwoOEBIKDhABCg4QAAoOMAAI3lBACN5QgAjeU2AKDjav3/6wvQoOEAiL3oAEgt6Q2woOEQ0E3i\n" +
                        "ABCg4QAAoOMAAI3lACCg4wQAjeUAMKDjCACN5fgAoONc/f/r/t7/5wAAUuM/AAAKADCg4QMAUuMC\n" +
                        "EMPmARBD5ToAADoHAFLjAhDA5QEQwOUDEEPlAhBD5TQAADoJAFLjAxDA5QQQQ+Ue/y8xAEgt6Q2w\n" +
                        "oOEBMQDjcRDv5gExQOORAwHgADBg4gPAA+IAMKDhDCBC4AwQo+cDwMLjDCCD4AkAXOMEEALlHwAA\n" +
                        "OhkAXOMEEIPlCBCD5QwQAuUIEALlGQAAOgwQg+UQEIPlFBCD5RgQg+UcEALlGBAC5RQQAuUQEALl\n" +
                        "BCAD4hjgguMOIEzgIABS4wwAADoOMIPgICBC4gAQg+UEEIPlHwBS4wgQg+UMEIPlEBCD5RQQg+UY\n" +
                        "EIPlHBCD5SAwg+Lz//+KAEi96B7/L+EAAAAAAAAAAAAAAADvvq/eAAAAABRFEQAAAAAAABAAAAAA\n" +
                        "AAD48/9/AQAAAPjz/38BAAAAMPT/fwEAAABg9P9/AQAAAIj0/38BAAAAoPT/fwEAAACs9P9/AQAA\n" +
                        "ALz0/38BAAAA0PT/fwEAAADs9P9/AQAAAAz1/38BAAAARPX/fwEAAAB09f9/AQAAAHz1/38BAAAA\n" +
                        "fPX/fwEAAABg9v9/AQAAAOz6/38BAAAAHPv/fwEAAABs+/9/AQAAAKD7/38BAAAA0Pv/fwEAAAD0\n" +
                        "+/9/AQAAACT8/38BAAAAVPz/fwEAAACE/P9/AQAAALj8/38BAAAA7Pz/fwEAAAAc/f9/AQAAAEz9\n" +
                        "/38BAAAAfP3/fwEAAACs/f9/AQAAANT9/38BAAAA0P3/fwEAAADU/v9/AQAAAA==\n";
        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        int hookInfoOffset = 0x0bf0;
        fillInHookInfo(bytes, hookInfoOffset);
        return bytes;
    }

    @Override
    public int getNativeDebugBreakOffset() {
        return 0x0000;
    }

    @Override
    public int getNativeClearCacheOffset() {
        return 0x0080;
    }

    @Override
    public int getNativeSyscallOffset() {
        return 0x0008;
    }

    @Override
    public int getNativeCallPointerFunction0Offset() {
        return 0x00d0;
    }

    @Override
    public int getNativeCallPointerFunction1Offset() {
        return 0x00e4;
    }

    @Override
    public int getNativeCallPointerFunction2Offset() {
        return 0x00fc;
    }

    @Override
    public int getNativeCallPointerFunction3Offset() {
        return 0x0118;
    }

    @Override
    public int getNativeCallPointerFunction4Offset() {
        return 0x013c;
    }

    @Override
    public int getNativeGetJavaVmOffset() {
        return 0x0164;
    }

    @Override
    public int getFakeStat64Offset() {
        return 0x01f4;
    }

    @Override
    public int getFakeMmap64Offset() {
        return 0x02e0;
    }

    @Override
    public int getFakeMmapOffset() {
        return 0x0878;
    }

    @Override
    public int __NR_mprotect() {
        // mprotect arm: 125
        return 125;
    }

    @Override
    public int __NR_memfd_create() {
        // memfd_create arm: 385
        return 385;
    }

    @Override
    public int __NR_ioctl() {
        // ioctl arm: 36
        return 36;
    }

    @Override
    public int __NR_tgkill() {
        // tgkill arm: 268
        return 268;
    }

    @Override
    public void inlineHook(long address, long hook) {
        if (address == 0) {
            throw new IllegalArgumentException("address is 0");
        }
        if (hook == 0) {
            throw new IllegalArgumentException("hook is 0");
        }
        boolean sourceIsThumb = (address & 1) != 0;
        long sourceAddress = address & ~1;
        byte[] trampoline;
        if (sourceIsThumb) {
            // Thumb 16-bit
            if ((sourceAddress & 4) == 0) {
                // c0 46          nop
                // df f8 04 c0    ldr.w   ip, [pc, #4]
                // 60 47          bx      ip
                // .uint32 address
                trampoline = new byte[]{
                        (byte) 0xc0, 0x46,
                        (byte) 0xdf, (byte) 0xf8, 0x04, (byte) 0xc0,
                        0x60, 0x47,
                        0, 0, 0, 0
                };
                ByteArrayUtils.writeInt32(trampoline, 8, (int) hook);
            } else {
                // df f8 02 c0   ldr.w   ip, [pc, #2]
                // 60 47         bx      ip
                // .uint32 address
                trampoline = new byte[]{
                        (byte) 0xdf, (byte) 0xf8, 0x02, (byte) 0xc0,
                        0x60, 0x47,
                        0, 0, 0, 0
                };
                ByteArrayUtils.writeInt32(trampoline, 6, (int) hook);
            }
        } else {
            // ARM 32-bit
            // 00 c0 9f e5    ldr ip, [pc]
            // 1c ff 2f e1    bx ip
            // .uint32 address
            trampoline = new byte[]{
                    (byte) 0x00, (byte) 0xc0, (byte) 0x9f, (byte) 0xe5,
                    (byte) 0x1c, (byte) 0xff, (byte) 0x2f, (byte) 0xe1,
                    0, 0, 0, 0
            };
            ByteArrayUtils.writeInt32(trampoline, 8, (int) hook);
        }
        writeByteArrayToTextSection(trampoline, sourceAddress);
    }

}
