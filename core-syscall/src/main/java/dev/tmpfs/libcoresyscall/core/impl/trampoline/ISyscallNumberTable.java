package dev.tmpfs.libcoresyscall.core.impl.trampoline;

public interface ISyscallNumberTable {

    int __NR_mprotect();

    int __NR_memfd_create();

    int __NR_tgkill();

    int __NR_ioctl();

}
