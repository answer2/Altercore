/*
 * Copyright (C) 2025 AnswerDev. All rights reserved.
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by AnswerDev
 */
package dev.answer.altercore.core;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;

import dev.tmpfs.libcoresyscall.core.NativeHelper;
import dev.tmpfs.libcoresyscall.core.impl.ArtMethodHelper;
import dev.tmpfs.libcoresyscall.core.impl.ReflectHelper;

public class ArtMethodObject extends NativeObject{

    private Method mMethod;

    public ArtMethodObject(Method method){
        super(getArtMethodAddressFromReflectedMethod(method));
        this.mMethod = method;
    }

    /*
    * * Copyright (C) LibcoreSyscall. All rights reserved.
    */
    @RequiresApi(23)
    private static long getArtMethodFromReflectedMethodForApi23To25(@NonNull Member method) {
        try {
            Class<?> kAbstractMethod = Class.forName("java.lang.reflect.AbstractMethod");
            Field artMethod = kAbstractMethod.getDeclaredField("artMethod");
            artMethod.setAccessible(true);
            return (long) Objects.requireNonNull(artMethod.get(method));
        } catch (ReflectiveOperationException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
    }

    @RequiresApi(26)
    private static long getArtMethodFromReflectedMethodAboveApi26(@NonNull Member method) {
        try {
            // Ljava/lang/reflect/Executable;->artMethod:J,unsupported
            //noinspection JavaReflectionMemberAccess
            Field artMethod = Executable.class.getDeclaredField("artMethod");
            artMethod.setAccessible(true);
            return (long) Objects.requireNonNull(artMethod.get(method));
        } catch (ReflectiveOperationException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
    }

    /**
     * Get the ArtMethod from a reflected method or constructor.
     *
     * @param method method or constructor
     * @return the ArtMethod address
     */
    private static long getArtMethodAddressFromReflectedMethod(@NonNull Member method) {
        if (Build.VERSION.SDK_INT >= 26) {
            return getArtMethodFromReflectedMethodAboveApi26(method);
        } else if (Build.VERSION.SDK_INT >= 23) {
            return getArtMethodFromReflectedMethodForApi23To25(method);
        } else {
            throw new UnsupportedOperationException("unsupported API: " + Build.VERSION.SDK_INT);
        }
    }

    private static Object getArtMethodObjectBelowSdk23(@NonNull Member method) {
        try {
            Class<?> kArtMethod = Class.forName("java.lang.reflect.ArtMethod");
            Class<?> kAbstractMethod = Class.forName("java.lang.reflect.AbstractMethod");
            Field artMethod = kAbstractMethod.getDeclaredField("artMethod");
            artMethod.setAccessible(true);
            return kArtMethod.cast(artMethod.get(method));
        } catch (ReflectiveOperationException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
    }

    private static long sArtMethodNativeEntryPointOffset = 0;

    private static long getArtMethodEntryPointFromJniOffset() {
        if (sArtMethodNativeEntryPointOffset != 0) {
            return sArtMethodNativeEntryPointOffset;
        }
        // For Android 6.0+/SDK23+, ArtMethod is no longer a mirror object.
        // We need to calculate the offset of the art::ArtMethod::entry_point_from_jni_ field.
        // See https://github.com/canyie/pine/blob/master/core/src/main/cpp/art/art_method.h
        boolean is64Bit = NativeHelper.isCurrentRuntime64Bit();
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.LOLLIPOP:
                sArtMethodNativeEntryPointOffset = 32;
                break;
            case Build.VERSION_CODES.LOLLIPOP_MR1:
                sArtMethodNativeEntryPointOffset = is64Bit ? 48 : 40;
                break;
            case Build.VERSION_CODES.M:
                sArtMethodNativeEntryPointOffset = is64Bit ? 40 : 32;
                break;
            case Build.VERSION_CODES.N:
            case Build.VERSION_CODES.N_MR1:
                sArtMethodNativeEntryPointOffset = is64Bit ? 40 : 28;
                break;
            case Build.VERSION_CODES.O:
            case Build.VERSION_CODES.O_MR1:
                sArtMethodNativeEntryPointOffset = is64Bit ? 32 : 24;
                break;
            case Build.VERSION_CODES.P:
            case Build.VERSION_CODES.Q:
            case Build.VERSION_CODES.R:
                sArtMethodNativeEntryPointOffset = is64Bit ? 24 : 20;
                break;
            case Build.VERSION_CODES.S:
            case Build.VERSION_CODES.S_V2:
            case Build.VERSION_CODES.TIRAMISU:
            case Build.VERSION_CODES.UPSIDE_DOWN_CAKE:
            case Build.VERSION_CODES.VANILLA_ICE_CREAM:
                sArtMethodNativeEntryPointOffset = 16;
                break;
            default:
                // use last/latest known offset
                sArtMethodNativeEntryPointOffset = 16;
                break;
        }
        return sArtMethodNativeEntryPointOffset;
    }

    public NativeObject getArtMethodEntryPoint() {
        return new NativeObject(address(), getArtMethodEntryPointFromJniOffset());
    }

    public NativeObject getArtMethodEntry(){
        NativeObject object = getArtMethodEntryPoint().peekPointer();
        long entryPoint = object.address();
        if (entryPoint == ArtMethodHelper.getJniDlsymLookupStub() || entryPoint == ArtMethodHelper.getJniDlsymLookupCriticalStub()) {
            return null;
        }
        return object;
    }





}
