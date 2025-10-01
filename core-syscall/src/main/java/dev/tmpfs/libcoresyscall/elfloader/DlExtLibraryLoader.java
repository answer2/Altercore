package dev.tmpfs.libcoresyscall.elfloader;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.MemoryFile;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Objects;

import dev.tmpfs.libcoresyscall.core.IAllocatedMemory;
import dev.tmpfs.libcoresyscall.core.MemoryAccess;
import dev.tmpfs.libcoresyscall.core.MemoryAllocator;
import dev.tmpfs.libcoresyscall.core.NativeAccess;
import dev.tmpfs.libcoresyscall.core.NativeHelper;
import dev.tmpfs.libcoresyscall.core.Syscall;
import dev.tmpfs.libcoresyscall.core.impl.FileDescriptorHelper;
import dev.tmpfs.libcoresyscall.core.impl.NativeBridge;
import dev.tmpfs.libcoresyscall.core.impl.ReflectHelper;

public class DlExtLibraryLoader {

    private DlExtLibraryLoader() {
        throw new AssertionError("no instance");
    }

    // LP32 has historical ABI breakage.
    private static final boolean IS_64_BIT = NativeHelper.isCurrentRuntime64Bit();

    public static final int RTLD_LOCAL = 0;
    public static final int RTLD_LAZY = 0x00001;
    public static final int RTLD_NOW = IS_64_BIT ? 0x00002 : 0x00000;
    public static final int RTLD_NOLOAD = 0x00004;
    public static final int RTLD_GLOBAL = IS_64_BIT ? 0x00100 : 0x00002;
    public static final int RTLD_NODELETE = 0x01000;

    public static final int ANDROID_DLEXT_USE_LIBRARY_FD = 0x10;
    public static final int ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET = 0x20;
    public static final int ANDROID_DLEXT_USE_NAMESPACE = 0x200;

    private static long sHandleLibdl = 0;
    private static long sFnDlopen = 0;
    private static long sFnDlsym = 0;
    private static long sFnDlerror = 0;

    private static long alignUp(long value, long alignment) {
        return (value + alignment - 1) & -alignment;
    }

    private static long alignUpToPage(long value) {
        return alignUp(value, NativeBridge.getPageSize());
    }

    private static void ensureCommonSymbols() {
        if (sHandleLibdl == 0) {
            // since API 26, there is a working real libdl.so
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk >= 26) {
                // parse the libdl.so to get these symbols
                SymbolResolver libdl;
                try {
                    libdl = SymbolResolver.getModule("libdl.so");
                } catch (SymbolResolver.NoSuchModuleException | IOException e) {
                    throw ReflectHelper.unsafeThrow(e);
                }
                long dlerror = libdl.getSymbolAddress("dlerror");
                if (dlerror == 0) {
                    throw new IllegalStateException("dlerror not found");
                }
                long dlsym = libdl.getSymbolAddress("dlsym");
                if (dlsym == 0) {
                    throw new IllegalStateException("dlsym not found");
                }
                long dlopen = libdl.getSymbolAddress("dlopen");
                if (dlopen == 0) {
                    throw new IllegalStateException("dlopen not found");
                }
                sFnDlsym = dlsym;
                sFnDlerror = dlerror;
                sFnDlopen = dlopen;
            } else {
                // for SDK 21 to 25, we have to use the __dl_ hidden symbols, that is bad, but we have no choice
                String linkerName = IS_64_BIT ? "linker64" : "linker";
                SymbolResolver linker;
                try {
                    linker = SymbolResolver.getModule(linkerName);
                } catch (SymbolResolver.NoSuchModuleException | IOException e) {
                    throw ReflectHelper.unsafeThrow(e);
                }
                long dlsym = linker.getSymbolAddress("__dl_dlsym");
                if (dlsym == 0) {
                    throw new IllegalStateException("__dl_dlsym not found");
                }
                long dlopen = linker.getSymbolAddress("__dl_dlopen");
                if (dlopen == 0) {
                    throw new IllegalStateException("__dl_dlopen not found");
                }
                long dlerror = linker.getSymbolAddress("__dl_dlerror");
                if (dlerror == 0) {
                    throw new IllegalStateException("__dl_dlerror not found");
                }
                sFnDlsym = dlsym;
                sFnDlerror = dlerror;
                sFnDlopen = dlopen;
            }
            long libdlHandle;
            try (IAllocatedMemory sn = MemoryAllocator.copyCString("libdl.so")) {
                libdlHandle = NativeAccess.callPointerFunction(sFnDlopen, sn.getAddress(), RTLD_NOW | RTLD_NOLOAD);
                if (libdlHandle == 0) {
                    throw new IllegalStateException("dlerror: " + MemoryAccess.peekCString(NativeAccess.callPointerFunction(sFnDlerror)));
                }
            }
            sHandleLibdl = libdlHandle;
        }
    }

    /**
     * Load a shared library from memory using android_dlopen_ext.
     *
     * @param elfData         the elf data, must be the complete elf file
     * @param name            the name of the library, e.g., "libfoo.so", must not be null or empty
     * @param flags           the flags to pass to android_dlopen_ext, dlfcn.h RTLD_*
     * @param extFlags        the flags to pass to android_dlopen_ext, dlext.h ANDROID_DLEXT_*
     * @param linkerNamespace the linker namespace where the library should be loaded into, 0 for the anonymous namespace
     *                        this parameter is only used on Android 24 and newer, and is ignored on older versions
     * @return the handle returned by android_dlopen_ext
     * @throws UnsatisfiedLinkError if the library could not be loaded
     */
    @SuppressLint("PrivateApi")
    public static long dlopenExtFromMemory(@NonNull byte[] elfData, @NonNull String name, int flags, int extFlags, long linkerNamespace) throws UnsatisfiedLinkError {
        // check the elf valid
        if (NativeHelper.getIsaFromElfHeader(elfData) != NativeHelper.getCurrentRuntimeIsa()) {
            String expectedIsa = NativeHelper.getIsaName(NativeHelper.getCurrentRuntimeIsa());
            String actualIsa = NativeHelper.getIsaName(NativeHelper.getIsaFromElfHeader(elfData));
            throw new IllegalArgumentException("Unsupported ISA: " + actualIsa + ", expected: " + expectedIsa);
        }
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name is empty");
        }
        int allocSize = Math.toIntExact(alignUpToPage(elfData.length));
        // args looks good, let's create an anonymous file and call android_dlopen_ext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try (SharedMemory ashmem = SharedMemory.create(name, allocSize)) {
                // map the ashmem, and write the elf data
                ByteBuffer buffer = ashmem.mapReadWrite();
                try {
                    buffer.put(elfData);
                    buffer.flip();
                } finally {
                    SharedMemory.unmap(buffer);
                }
                // peek the file descriptor for android_dlopen_ext,
                // but we must not close the fd, because it is owned by the SharedMemory object
                int peekFd;
                try {
                    //noinspection JavaReflectionMemberAccess
                    Method getFd = SharedMemory.class.getDeclaredMethod("getFd");
                    getFd.setAccessible(true);
                    //noinspection DataFlowIssue
                    peekFd = (int) getFd.invoke(ashmem);
                } catch (ReflectiveOperationException e) {
                    throw ReflectHelper.unsafeThrow(e);
                }
                patchLinkerIfRequired(peekFd);
                return dlopenExt(name, flags, extFlags, peekFd, 0, linkerNamespace);
                // try-with-resources will close the ashmem, we don't need to close it manually
            } catch (ErrnoException e) {
                throw ReflectHelper.unsafeThrow(e);
            }
        } else {
            // We are on older Android SDK 26 and older, we can't use SharedMemory.
            // The hidden api restriction starting from API 28, but we are far from that.
            Method native_open;
            try {
                //noinspection JavaReflectionMemberAccess
                native_open = MemoryFile.class.getDeclaredMethod("native_open", String.class, int.class);
                native_open.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw ReflectHelper.unsafeThrow(e);
            }
            FileDescriptor theFdObj;
            try {
                theFdObj = (FileDescriptor) native_open.invoke(null, name, elfData.length);
            } catch (ReflectiveOperationException e) {
                throw ReflectHelper.unsafeThrow(e);
            }
            if (theFdObj == null) {
                throw new IllegalStateException("native_open failed with null FileDescriptor without exception");
            }
            patchLinkerIfRequired(FileDescriptorHelper.getInt(theFdObj));
            try {
                // mmap the file descriptor, and write the elf data
                long address = 0;
                try {
                    address = Os.mmap(0, allocSize,
                            OsConstants.PROT_READ | OsConstants.PROT_WRITE,
                            OsConstants.MAP_SHARED, theFdObj, 0);
                    // write the elf data
                    MemoryAccess.pokeByteArray(address, elfData, 0, elfData.length);
                } catch (ErrnoException e) {
                    throw ReflectHelper.unsafeThrow(e);
                } finally {
                    if (address != 0) {
                        try {
                            Os.munmap(address, allocSize);
                        } catch (ErrnoException e) {
                            // ignore any exception
                        }
                    }
                }
                int theFdInt;
                try {
                    //noinspection DataFlowIssue,JavaReflectionMemberAccess
                    theFdInt = (int) FileDescriptor.class.getDeclaredMethod("getInt$").invoke(theFdObj);
                } catch (ReflectiveOperationException e) {
                    throw ReflectHelper.unsafeThrow(e);
                }
                return dlopenExt(name, flags, extFlags, theFdInt, 0, linkerNamespace);
            } finally {
                // we need to close the file descriptor manually, as it is not owned by MemoryFile
                if (theFdObj.valid()) {
                    try {
                        Os.close(theFdObj);
                    } catch (ErrnoException e) {
                        // ignore any exception
                    }
                }
            }
        }
    }

    /**
     * Call android_dlopen_ext with the given parameters.
     *
     * @param name            the name or path of the library to load, must not be null or empty
     * @param flags           the flags to pass to android_dlopen_ext, dlfcn.h RTLD_*
     * @param extFlags        the flags to pass to android_dlopen_ext, dlext.h ANDROID_DLEXT_*
     * @param fd              the file descriptor to pass to android_dlopen_ext, or -1 if not used
     * @param offset          the offset to use with the file descriptor, or 0 if not used
     * @param linkerNamespace the linker namespace to use, 0 for the anonymous namespace, ignored on Android 23 and older
     * @return the handle returned by android_dlopen_ext
     * @throws UnsatisfiedLinkError if the library could not be loaded
     */
    public static long dlopenExt(@NonNull String name, int flags, int extFlags, int fd, long offset, long linkerNamespace) throws UnsatisfiedLinkError {
        // check basic arguments
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name is empty");
        }
        if (fd < -1) {
            throw new IllegalArgumentException("fd is invalid");
        }
        if (offset != 0 && fd == -1) {
            throw new IllegalArgumentException("can't use offset without fd");
        }
        // Android do not support dlopen with RTLD_LAZY, replace it with RTLD_NOW
        if ((flags & RTLD_LAZY) != 0) {
            flags &= ~RTLD_LAZY;
            flags |= RTLD_NOW;
        }
        ensureCommonSymbols();
        final boolean useApi23BugWorkaround = fd != -1 && Build.VERSION.SDK_INT == 23;
        if (useApi23BugWorkaround) {
            WorkaroundForApi23Bug.prepare();
        }
        try (IAllocatedMemory extinfo = MemoryAllocator.allocate(48, true)) {
            long dlopenExtAddress;
            boolean withCallerAddress;
            final int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < 24) {
                // no linker namespace support, no caller address
                withCallerAddress = false;
                try (IAllocatedMemory sn = MemoryAllocator.copyCString("android_dlopen_ext")) {
                    dlopenExtAddress = NativeAccess.callPointerFunction(sFnDlsym, sHandleLibdl, sn.getAddress());
                    if (dlopenExtAddress == 0) {
                        throw new IllegalStateException("android_dlopen_ext not found");
                    }
                }
            } else if (sdk >= 26) {
                // we have __loader_android_dlopen_ext, which is a dynamic symbol
                // we need a "real" libdl.so handle for that
                withCallerAddress = true;
                try (IAllocatedMemory sn = MemoryAllocator.copyCString("__loader_android_dlopen_ext")) {
                    dlopenExtAddress = NativeAccess.callPointerFunction(sFnDlsym, sHandleLibdl, sn.getAddress());
                    if (dlopenExtAddress == 0) {
                        throw new IllegalStateException("__loader_android_dlopen_ext not found");
                    }
                }
            } else {
                // for Android 24 and 25, we have to use the __dl__ZL10dlopen_extPKciPK17android_dlextinfoPv from linker
                String linkerName = IS_64_BIT ? "linker64" : "linker";
                SymbolResolver linker;
                try {
                    linker = SymbolResolver.getModule(linkerName);
                } catch (SymbolResolver.NoSuchModuleException | IOException e) {
                    throw ReflectHelper.unsafeThrow(e);
                }
                long dlopen_ext_static = linker.getSymbolAddress("__dl__ZL10dlopen_extPKciPK17android_dlextinfoPv");
                long android_dlopen_ext_standard = linker.getSymbolAddress("__dl_android_dlopen_ext");
                long g_anonymous_namespace = linker.getSymbolAddress("__dl__ZL21g_anonymous_namespace");
                if (dlopen_ext_static != 0) {
                    // we do prefer the static symbol, since it can be specified with the caller address
                    withCallerAddress = true;
                    dlopenExtAddress = dlopen_ext_static;
                } else {
                    if (android_dlopen_ext_standard == 0) {
                        throw new IllegalStateException("__dl_android_dlopen_ext not found");
                    }
                    if (g_anonymous_namespace == 0) {
                        throw new IllegalStateException("__dl__ZL21g_anonymous_namespace not found");
                    }
                    // override the linker namespace, if it is not specified, for consistency
                    if (linkerNamespace == 0) {
                        linkerNamespace = MemoryAccess.peekPointer(g_anonymous_namespace);
                    }
                    withCallerAddress = false;
                    dlopenExtAddress = android_dlopen_ext_standard;
                }
            }
            // prepare the android_dlextinfo structure
            //typedef struct {
            //  uint64_t flags;
            //  void*   reserved_addr;
            //  size_t  reserved_size;
            //  int     relro_fd;
            //  int     library_fd;
            //  off64_t library_fd_offset;
            //  void* library_namespace;
            //} android_dlextinfo;
            int dlextinfo_flags = extFlags;
            int offset_library_fd = IS_64_BIT ? 28 : 20;
            int offset_library_fd_offset = IS_64_BIT ? 32 : 24;
            int offset_linker_namespace = IS_64_BIT ? 40 : 32;
            long extinfoAddress = extinfo.getAddress();
            if (fd != -1) {
                dlextinfo_flags |= ANDROID_DLEXT_USE_LIBRARY_FD;
                MemoryAccess.pokeInt(extinfoAddress + offset_library_fd, fd);
                if (offset != 0) {
                    if (sdk < 23) {
                        throw new IllegalArgumentException("ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET is not supported before Android 23");
                    }
                    dlextinfo_flags |= ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET;
                    MemoryAccess.pokeLong(extinfoAddress + offset_library_fd_offset, offset);
                } else {
                    if (sdk < 23) {
                        // ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET since it is not supported before Android 23
                        // it is just fine if offset is 0
                        dlextinfo_flags &= ~ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET;
                    }
                }
            }
            if (linkerNamespace != 0 && sdk >= 24) {
                dlextinfo_flags |= ANDROID_DLEXT_USE_NAMESPACE;
                if (IS_64_BIT) {
                    MemoryAccess.pokeLong(extinfoAddress + offset_linker_namespace, linkerNamespace);
                } else {
                    MemoryAccess.pokeInt(extinfoAddress + offset_linker_namespace, (int) linkerNamespace);
                }
            }
            MemoryAccess.pokeLong(extinfoAddress, dlextinfo_flags);
            long result;
            String errMsg = "(null)";
            if (useApi23BugWorkaround) {
                WorkaroundForApi23Bug.apply();
            }
            try (IAllocatedMemory libname = MemoryAllocator.copyCString(name)) {
                if (withCallerAddress) {
                    result = NativeAccess.callPointerFunction(dlopenExtAddress, libname.getAddress(), flags,
                            extinfoAddress, NativeBridge.getTrampolineBase() + 1024);
                } else {
                    result = NativeAccess.callPointerFunction(dlopenExtAddress, libname.getAddress(), flags, extinfoAddress);
                }
                long errPtr = NativeAccess.callPointerFunction(sFnDlerror);
                if (errPtr != 0) {
                    errMsg = MemoryAccess.peekCString(errPtr);
                }
            } finally {
                if (useApi23BugWorkaround) {
                    WorkaroundForApi23Bug.restore();
                }
            }
            if (result == 0) {
                throw new UnsatisfiedLinkError(errMsg);
            }
            return result;
        }
    }

    public static String dlerror() {
        ensureCommonSymbols();
        return MemoryAccess.peekCString(NativeAccess.callPointerFunction(sFnDlerror));
    }

    public static long dlsym(long handle, @NonNull String symbol) {
        // allow null handle
        Objects.requireNonNull(symbol, "symbol");
        ensureCommonSymbols();
        try (IAllocatedMemory sn = MemoryAllocator.copyCString(symbol)) {
            return NativeAccess.callPointerFunction(sFnDlsym, handle, sn.getAddress());
        }
    }

    public static long dlopen(@NonNull String name, int flags) {
        Objects.requireNonNull(name, "name");
        ensureCommonSymbols();
        try (IAllocatedMemory sn = MemoryAllocator.copyCString(name)) {
            return NativeAccess.callPointerFunction(sFnDlopen, sn.getAddress(), flags);
        }
    }

    private static void patchLinkerIfRequired(int fdInt) {
        if (fdInt < 0) {
            throw new IllegalArgumentException("invalid file descriptor: " + fdInt);
        }
        String path;
        try {
            path = Os.readlink("/proc/self/fd/" + fdInt);
        } catch (ErrnoException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
        if (!path.startsWith("/dev/ashmem")) {
            // not ashmem, no need to patch
            return;
        }
        LinkerPatch.applyPatch();
    }

    /**
     * Set the linker debug verbosity level.
     *
     * @param level the new verbosity level, 0 for no debug, 1 for info, 2 for trace, 3 for debug. default is 0
     */
    public static void setLdDebugVerbosity(int level) {
        SymbolResolver linker;
        try {
            linker = SymbolResolver.getModule(IS_64_BIT ? "linker64" : "linker");
        } catch (SymbolResolver.NoSuchModuleException | IOException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
        long g_ld_debug_verbosity = linker.getSymbolAddress("__dl_g_ld_debug_verbosity");
        if (g_ld_debug_verbosity == 0) {
            throw new IllegalStateException("symbol not found: __dl_g_ld_debug_verbosity");
        }
        MemoryAccess.pokeInt(g_ld_debug_verbosity, level);
    }

    private static class WorkaroundForApi23Bug {


        /**
         * Android 6.0.0_r1 has a bug in the linker, which causes the ANDROID_DLEXT_USE_LIBRARY_FD dlextinfo
         * to be applied to the DT_NEEDED libraries as well.
         * This will cause the linker to try to load the DT_NEEDED libraries from the same file descriptor.
         * If there are no DT_NEEDED libraries, this is not a problem.
         * But if there are, the linker will fail to load the library because in that case
         * linker will not be able to find the DT_NEEDED libraries correctly.
         * Affected android versions: [android-6.0.0_r1, android-6.0.0_r41).
         * <p>
         * Please see <a href="https://cs.android.com/android/platform/superproject/+/android-6.0.0_r26:bionic/linker/linker.cpp;drc=de0fb393ae8136a5958fe17eee0c6285e2f7f91a;l=1471">android-6.0.0_r26</a>
         * and <a href="https://cs.android.com/android/platform/superproject/+/android-6.0.0_r41:bionic/linker/linker.cpp;drc=cf92738fa5dee24050028a1235f815f2a0fd33b5;l=1481">android-6.0.0_r41</a>
         * for more details.
         */
        private WorkaroundForApi23Bug() {
            throw new AssertionError("no instance");
        }

        private static long android_get_application_target_sdk_version = 0;
        private static long set_application_target_sdk_version = 0;
        private static int sSavedTargetSdkVersion = -1;

        public static void prepare() {
            ensureCommonSymbols();
            android_get_application_target_sdk_version = dlsym(sHandleLibdl, "android_get_application_target_sdk_version");
            if (android_get_application_target_sdk_version == 0) {
                throw new IllegalStateException("android_get_application_target_sdk_version not found");
            }
            SymbolResolver linker;
            try {
                linker = SymbolResolver.getModule(IS_64_BIT ? "linker64" : "linker");
            } catch (SymbolResolver.NoSuchModuleException | IOException e) {
                throw ReflectHelper.unsafeThrow(e);
            }
            set_application_target_sdk_version = linker.getSymbolAddress("__dl__Z42android_set_application_target_sdk_versionj");
            if (set_application_target_sdk_version == 0) {
                throw new IllegalStateException("android_set_application_target_sdk_version not found");
            }
        }

        public static void apply() {
            if (sSavedTargetSdkVersion != -1) {
                throw new IllegalStateException("already applied");
            }
            sSavedTargetSdkVersion = (int) NativeAccess.callPointerFunction(android_get_application_target_sdk_version);
            // set API level to so-called "current" level
            NativeAccess.callPointerFunction(set_application_target_sdk_version, 0);
        }

        public static void restore() {
            if (sSavedTargetSdkVersion == -1) {
                throw new IllegalStateException("not applied");
            }
            NativeAccess.callPointerFunction(set_application_target_sdk_version, sSavedTargetSdkVersion);
            sSavedTargetSdkVersion = -1;
        }

    }

    private static long getAshmemRegionSize(int fd) throws ErrnoException {
        int ASHMEM_GET_SIZE = 0x7704;
        return Syscall.ioctl(fd, ASHMEM_GET_SIZE, 0);
    }

}
