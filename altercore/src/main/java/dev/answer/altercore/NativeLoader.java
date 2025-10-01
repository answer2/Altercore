package dev.answer.altercore;

import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import dev.tmpfs.libcoresyscall.core.NativeAccess;
import dev.tmpfs.libcoresyscall.core.NativeHelper;
import dev.tmpfs.libcoresyscall.core.impl.ReflectHelper;
import dev.tmpfs.libcoresyscall.elfloader.DlExtLibraryLoader;
import dev.tmpfs.libcoresyscall.elfloader.NativeRegistrationHelper;

public class NativeLoader {

    public static String TAG = "NativeLoader";
    private static final Map<String, Long> sHandleMap = new HashMap<>();
    private static final int JNI_ERR = -1;
    private static final int JNI_VERSION_1_2 = 0x00010002;
    private static final int JNI_VERSION_1_4 = 0x00010004;
    private static final int JNI_VERSION_1_6 = 0x00010006;
    private static final int SHELLCODE_SIZE = 0x200;
    public String sLoadLog = "";

    private byte[] mElfData;
    private long mHandle;
    private long mJniOnLoad;

    private NativeLoader() {
    }

    public NativeLoader(File path){
        this.mHandle = loadSo(path);
    }

    public NativeLoader(String soName){
        this.mHandle = loadSoLocal(soName);
    }

    public static String getNativeLibraryDirName(int isa) {
        switch (isa) {
            case NativeHelper.ISA_X86:
                return "x86";
            case NativeHelper.ISA_X86_64:
                return "x86_64";
            case NativeHelper.ISA_ARM:
                // we only support armeabi-v7a, not armeabi
                return "armeabi-v7a";
            case NativeHelper.ISA_ARM64:
                return "arm64-v8a";
            case NativeHelper.ISA_MIPS:
                // not sure, I have never seen a mips device
                return "mips";
            case NativeHelper.ISA_MIPS64:
                // not sure, I have never seen a mips64 device
                return "mips64";
            case NativeHelper.ISA_RISCV64:
                // not sure, I have never seen a riscv64 device
                return "riscv64";
            default:
                throw new IllegalArgumentException("Unsupported ISA: " + isa);
        }
    }

    public byte[] getElfDataPath(String path) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new FileInputStream(path)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
    }

    public byte[] getElfDataLocal(String soname) {
        String path = "/lib/" + getNativeLibraryDirName(NativeHelper.getCurrentRuntimeIsa()) + "/" + soname;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = NativeLoader.class.getResourceAsStream(path)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
    }

    private FileDescriptor createTempReadOnlyFile(@NonNull Context ctx, @NonNull String name, @NonNull byte[] data) {
        File cacheDir = ctx.getCacheDir();
        File randomDir = new File(cacheDir, "tmpfs-" + System.nanoTime());
        if (!randomDir.mkdirs()) {
            throw new IllegalStateException("Cannot create directory: " + randomDir);
        }
        File file = new File(randomDir, name);
        // write data to file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
        // make the file read-only
        if (!file.setWritable(false, false)) {
            throw new IllegalStateException("Cannot set file permissions: " + file);
        }
        // re-open the file in read-only mode
        FileDescriptor fd;
        try {
            fd = Os.open(file.getAbsolutePath(), OsConstants.O_RDONLY, 0);
        } catch (ErrnoException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
        // delete the file
        if (!file.delete()) {
            throw new IllegalStateException("Cannot delete file: " + file);
        }
        // delete the directory, if empty
        randomDir.delete();
        return fd;
    }

    public synchronized long loadSo(File soFile) {
        String soName = soFile.getName();
        if (sHandleMap.containsKey(soName)) {
            return sHandleMap.get(soName);
        }

        this.mElfData = getElfDataPath(soFile.getAbsolutePath());
        return load(soName, mElfData);
    }

    public synchronized long loadSoLocal(String soName) {
        if (sHandleMap.containsKey(soName)) {
            return sHandleMap.get(soName);
        }
        this.mElfData = getElfDataLocal(soName);
        return load(soName, mElfData);
    }

    private synchronized long load(String soname,byte[] elfData) {
        long handle = DlExtLibraryLoader.dlopenExtFromMemory(elfData, soname, DlExtLibraryLoader.RTLD_NOW, 0, 0);

        if (handle != 0) {
            NativeRegistrationHelper.RegistrationSummary summary =
                    NativeRegistrationHelper.registerNativeMethodsForLibrary(handle, elfData);

            Log.d(TAG, soname + ": registerNativeMethodsForLibrary: " + summary);
            sLoadLog += soname + ": registerNativeMethodsForLibrary: " + summary + "\n";

            long jniOnLoad = DlExtLibraryLoader.dlsym(handle, "JNI_OnLoad");
            if (jniOnLoad != 0) {
                long javaVm = NativeAccess.getJavaVM();
                long ret = NativeAccess.callPointerFunction(jniOnLoad, javaVm, 0);
                if (ret < 0) {
                    throw new IllegalStateException("JNI_OnLoad failed: " + ret);
                }
            }
        }
        sHandleMap.put(soname, handle);
        Log.d(TAG, soname + " -> " + handle);
        sLoadLog += soname + " -> " + handle + "\n";
        return handle;
    }

    public long dlsym(@NonNull String symbol){
        return DlExtLibraryLoader.dlsym(mHandle, "JNI_OnLoad");
    }

    public long callPointerFunction(long func, long... args){
        return  NativeAccess.callPointerFunction(func, args);
    }

}