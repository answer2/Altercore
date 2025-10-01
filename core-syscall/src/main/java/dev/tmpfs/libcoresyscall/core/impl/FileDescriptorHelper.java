package dev.tmpfs.libcoresyscall.core.impl;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.lang.reflect.Method;

public class FileDescriptorHelper {

    private FileDescriptorHelper() {
        throw new AssertionError("no instance for you!");
    }

    private static Method sGetIntMethod;
    private static Method sSetIntMethod;

    public static int getInt(@NonNull FileDescriptor fd) {
        if (sGetIntMethod == null) {
            try {
                //noinspection JavaReflectionMemberAccess
                sGetIntMethod = FileDescriptor.class.getDeclaredMethod("getInt$");
                sGetIntMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw ReflectHelper.unsafeThrow(e);
            }
        }
        try {
            //noinspection DataFlowIssue
            return (int) sGetIntMethod.invoke(fd);
        } catch (ReflectiveOperationException e) {
            throw ReflectHelper.unsafeThrowForIteCause(e);
        }
    }

    public static void setInt(@NonNull FileDescriptor fdObj, int fdInt) {
        if (sSetIntMethod == null) {
            try {
                //noinspection JavaReflectionMemberAccess
                sSetIntMethod = FileDescriptor.class.getDeclaredMethod("setInt$", int.class);
                sSetIntMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw ReflectHelper.unsafeThrow(e);
            }
        }
        if (fdInt < 0) {
            throw new IllegalArgumentException("invalid raw fd given: " + fdInt);
        }
        if (fdObj.valid()) {
            throw new IllegalArgumentException("the file descriptor object is already has a valid fd set: " + fdObj);
        }
        try {
            sSetIntMethod.invoke(fdObj, fdInt);
        } catch (ReflectiveOperationException e) {
            throw ReflectHelper.unsafeThrowForIteCause(e);
        }
    }

    public static FileDescriptor wrap(int fd) {
        FileDescriptor fileDescriptor = new FileDescriptor();
        setInt(fileDescriptor, fd);
        return fileDescriptor;
    }

}
