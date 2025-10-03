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
import java.util.Objects;

import dev.answer.altercore.AlterConfig;
import dev.answer.altercore.NativeImpl;
import dev.answer.altercore.utils.AlterLog;
import dev.answer.altercore.utils.MemberOffest;
import dev.tmpfs.libcoresyscall.core.NativeHelper;
import dev.tmpfs.libcoresyscall.core.impl.ArtMethodHelper;
import dev.tmpfs.libcoresyscall.core.impl.ReflectHelper;
import dev.tmpfs.libcoresyscall.elfloader.SymbolResolver;

public class ArtMethodObject extends NativeObject {

    private static int kAccPreCompiled;
    private static int kAccCompileDontBother;

    private static int android_version = Build.VERSION.SDK_INT;

    private static MemberOffest entryPointFromJni = new MemberOffest();
    private static NativeObject art_quick_to_interpreter_bridge;
    private static NativeObject art_quick_generic_jni_trampoline;
    private static NativeObject art_interpreter_to_compiled_code_bridge;
    private static NativeObject art_interpreter_to_interpreter_bridge;
    private static NativeObject throw_invocation_time_error;
    private static NativeObject ExecuteNterpImpl;
    private static NativeObject copy_from;

    private static MemberOffest declaring_class = new MemberOffest();
    private static MemberOffest accessFlags = new MemberOffest();
    private static MemberOffest entryPointFromCompiledCode = new MemberOffest();
    private static MemberOffest entryPointFromInterpreter = new MemberOffest();

    private Member mMember;

    public ArtMethodObject(Member member) {
        super(getArtMethodAddressFromReflectedMethod(member));
        this.mMember = member;
    }

    public static void init(SymbolResolver symbolResolver) {

        art_quick_to_interpreter_bridge = NativeImpl.getSymbolObject(symbolResolver,
                "art_quick_to_interpreter_bridge");

        art_quick_generic_jni_trampoline = NativeImpl.getSymbolObject(symbolResolver,
                "art_quick_generic_jni_trampoline");

        ExecuteNterpImpl = NativeImpl.getSymbolObject(symbolResolver,
                "ExecuteNterpImpl");

        if (android_version < Build.VERSION_CODES.N) {
            art_interpreter_to_compiled_code_bridge = NativeImpl.getSymbolObject(symbolResolver,
                    "artInterpreterToCompiledCodeBridge");
            art_interpreter_to_interpreter_bridge = NativeImpl.getSymbolObject(symbolResolver,
                    "artInterpreterToInterpreterBridge");
        }

        String symbol_copy_from = null;
        if (android_version >= Build.VERSION_CODES.O) {
            // art::ArtMethod::CopyFrom(art::ArtMethod *, art::PointerSize)
            symbol_copy_from = "_ZN3art9ArtMethod8CopyFromEPS0_NS_11PointerSizeE";
        } else if (android_version >= Build.VERSION_CODES.N) {
            if (isCurrentRuntime64Bit())
                // art::ArtMethod::CopyFrom(art::ArtMethod *, unsigned long)
                symbol_copy_from = "_ZN3art9ArtMethod8CopyFromEPS0_m";
            else
                // art::ArtMethod::CopyFrom(art::ArtMethod *, unsigned int)
                symbol_copy_from = "_ZN3art9ArtMethod8CopyFromEPS0_j";

        } else if (android_version >= Build.VERSION_CODES.M) {
            if (isCurrentRuntime64Bit())
                // art::ArtMethod::CopyFrom(art::ArtMethod const *, unsigned long)
                symbol_copy_from = "_ZN3art9ArtMethod8CopyFromEPKS0_m";
            else
                // art::ArtMethod::CopyFrom(art::ArtMethod const *, unsigned int)
                symbol_copy_from = "_ZN3art9ArtMethod8CopyFromEPKS0_j";
        }

        if (symbol_copy_from != null)
            copy_from =
                    /*reinterpret_cast<void (*)(ArtMethod*, ArtMethod*, size_t)>*/
                    (NativeImpl.getSymbolObject(symbolResolver, symbol_copy_from));

        if (android_version == Build.VERSION_CODES.O)
            throw_invocation_time_error =
                    /*reinterpret_cast<void (*)(ArtMethod*)>*/
                    (NativeImpl.getSymbolObject(symbolResolver,
                            "_ZN3art9ArtMethod24ThrowInvocationTimeErrorEv"));


    }

    public static void init(ArtMethodObject a, ArtMethodObject b, ArtMethodObject c,
                            int access_flags) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            kAccCompileDontBother = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
                    ? AccessFlags.kCompileDontBother_O_MR1
                    : AccessFlags.kCompileDontBother_N;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                kAccPreCompiled = (Build.VERSION.SDK_INT == Build.VERSION_CODES.R)
                        ? AccessFlags.kPreCompiled_R
                        : AccessFlags.kPreCompiled_S;
        }

        int size = difference(a.address(), b.address());
        declaring_class.setOffset(android_version >= Build.VERSION_CODES.M ? 0 : 8);


        // 预处理条件编译
        if (android_version >= Build.VERSION_CODES.LOLLIPOP) {
            // 扫描内存查找偏移量
            for (int offset = 0; offset < size; offset += 2) {
                NativeObject ptr = new NativeObject(a.address(), offset);

                if (ptr.peekPointer().address() == access_flags) {
                    accessFlags.setOffset(offset);
                } else if (android_version == Build.VERSION_CODES.LOLLIPOP) {
                    // On Android 5.0, type of entry_point_from_jni_ is uint64_t
                    if (ptr.address() == a.address()) { // native function
                        entryPointFromJni.setOffset(offset);
                    }
                } else if (ptr.peekPointer().address() == a.address()) { // native function
                    entryPointFromJni.setOffset(offset);
                }

                boolean done = accessFlags.isValid() && entryPointFromJni.isValid();
                if (done) break;
            }

            // 处理access flags未找到的情况
            if (!accessFlags.isValid()) {
                if (android_version >= Build.VERSION_CODES.N) {
                    // TODO: Is this really possible?
                    AlterLog.w("failed to find access_flags_ with default access flags, try again with kAccCompileDontBother");
                    access_flags |= kAccCompileDontBother;
                    int offset = findOffset(a.address(), access_flags, size, 2);
                    if (offset >= 0) {
                        AlterLog.w("Found access_flags_ with kAccCompileDontBother, offset %d", offset);
                        accessFlags.setOffset(offset);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Android R has a new access flags: kAccPreCompiled
                        // TODO: Is this really possible?
                        AlterLog.w("failed to find access_flags_ with default access flags, try again with kAccPreCompiled");
                        access_flags |= kAccPreCompiled;
                        // Don't clear kAccCompileDontBother.
                        offset = findOffset(a.address(), access_flags, size, 2);
                        if (offset >= 0) {
                            AlterLog.w("Found access_flags_ with kAccPreCompiled, offset %d", offset);
                            accessFlags.setOffset(offset);
                        }
                    }
                }
                AlterLog.w("Member access_flags_ not found in ArtMethod, use default.");
                accessFlags.setOffset(getDefaultAccessFlagsOffset());
            }

            // 设置entry point相关偏移量
            int entry_point_member_size = (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP
                    ? 8 : 4);

            if (entryPointFromJni.isValid()) {
                long compiled_code_entry_offset = entryPointFromJni.getOffset()
                        + entry_point_member_size;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Only align offset on Android O+ (PtrSizedFields is PACKED(4) in Android N or lower.)
                    compiled_code_entry_offset = alignUp(compiled_code_entry_offset,
                            entry_point_member_size);
                }

                entryPointFromCompiledCode.setOffset(compiled_code_entry_offset);
            } else {
                AlterLog.e("Member entry_point_from_jni_ not found in ArtMethod, use default.");
                entryPointFromJni.setOffset(getArtMethodEntryPointFromJniOffset());
                entryPointFromCompiledCode.setOffset(
                        getDefaultEntryPointFromQuickCompiledCodeOffset());
            }

            // 处理Android N以下的interpreter entry point
            if (android_version < Build.VERSION_CODES.N) {
                // Not aligned: PtrSizedFields is PACKED(4) in the android version.
                entryPointFromInterpreter = new MemberOffest(
                        entryPointFromJni.getOffset() - entry_point_member_size);
            }
        } else {
            // Hardcode members offset for Kitkat
            AlterLog.w("Android Kitkat, hardcode offset only...");
            accessFlags.setOffset(28);
            entryPointFromCompiledCode.setOffset(32);

            // FIXME This offset has not been verified, so it may be wrong
            entryPointFromInterpreter.setOffset(36);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Hardcode members offset for Kitkat :(
            AlterLog.e("Android Kitkat, hardcode offset only...");
            accessFlags.setOffset(28);
            entryPointFromCompiledCode.setOffset(32);

            // FIXME This offset has not been verified, so it may be wrong
            entryPointFromInterpreter = new MemberOffest(36);
        }

        if (throw_invocation_time_error != null && throw_invocation_time_error.isNotNull()) {
            // See https://github.com/canyie/pine/issues/8
//            if (m3->TestDontCompile(env)) {
//                AlterLog.w("Detected android 8.1 runtime on android 8.0 device");
//                AlterLog.w("For more info, see https://github.com/canyie/pine/issues/8");
//                kAccCompileDontBother = AccessFlags.kCompileDontBother_O_MR1;
//            }
        }

        AlterLog.d(entryPointFromCompiledCode.getOffset() + "");

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
    private static long sEntryPointFromQuickCompiledCodeOffset = 0;
    private static long sAccessFlagsOffset = 0;

    private static long getDefaultAccessFlagsOffset() {
        if (sAccessFlagsOffset != 0) {
            return sAccessFlagsOffset;
        }
        // see https://github.com/canyie/pine/blob/master/core/src/main/cpp/art/art_method.h
        switch (Build.VERSION.SDK_INT) {
            default:
                AlterLog.w(String.format("Unsupported Android API level %d, using Android VanillaIceCream", Build.VERSION.SDK_INT));
            case Build.VERSION_CODES.VANILLA_ICE_CREAM:
            case Build.VERSION_CODES.UPSIDE_DOWN_CAKE:
            case Build.VERSION_CODES.TIRAMISU:
            case Build.VERSION_CODES.S_V2:
            case Build.VERSION_CODES.S:
            case Build.VERSION_CODES.R:
            case Build.VERSION_CODES.Q:
            case Build.VERSION_CODES.P:
            case Build.VERSION_CODES.O_MR1:
            case Build.VERSION_CODES.O:
            case Build.VERSION_CODES.N_MR1:
            case Build.VERSION_CODES.N:
                sAccessFlagsOffset = 4;
            case Build.VERSION_CODES.M:
                sAccessFlagsOffset = 12;
            case Build.VERSION_CODES.LOLLIPOP_MR1:
                sAccessFlagsOffset = 20;
            case Build.VERSION_CODES.LOLLIPOP:
                sAccessFlagsOffset = 56;
        }
        return sAccessFlagsOffset;
    }


    public static long getDefaultEntryPointFromQuickCompiledCodeOffset() {
        if (sEntryPointFromQuickCompiledCodeOffset != 0) {
            return sEntryPointFromQuickCompiledCodeOffset;
        }

        boolean is64Bit = NativeHelper.isCurrentRuntime64Bit();
        // See https://github.com/canyie/pine/blob/master/core/src/main/cpp/art/art_method.h
        switch (Build.VERSION.SDK_INT) {
            default:
                AlterLog.w(String.format("Unsupported Android API level %d, using Android VanillaIceCream", Build.VERSION.SDK_INT));
            case Build.VERSION_CODES.VANILLA_ICE_CREAM:
            case Build.VERSION_CODES.UPSIDE_DOWN_CAKE:
            case Build.VERSION_CODES.TIRAMISU:
            case Build.VERSION_CODES.S_V2:
            case Build.VERSION_CODES.S:
                sEntryPointFromQuickCompiledCodeOffset = is64Bit ? 24 : 20;
            case Build.VERSION_CODES.R:
            case Build.VERSION_CODES.Q:
            case Build.VERSION_CODES.P:
                sEntryPointFromQuickCompiledCodeOffset = is64Bit ? 32 : 24;
            case Build.VERSION_CODES.O_MR1:
            case Build.VERSION_CODES.O:
                sEntryPointFromQuickCompiledCodeOffset = is64Bit ? 40 : 28;
            case Build.VERSION_CODES.N_MR1:
            case Build.VERSION_CODES.N:
                sEntryPointFromQuickCompiledCodeOffset = is64Bit ? 48 : 32;
            case Build.VERSION_CODES.M:
                sEntryPointFromQuickCompiledCodeOffset = is64Bit ? 48 : 36;
            case Build.VERSION_CODES.LOLLIPOP_MR1:
                sEntryPointFromQuickCompiledCodeOffset = is64Bit ? 56 : 44;
            case Build.VERSION_CODES.LOLLIPOP:
                sEntryPointFromQuickCompiledCodeOffset = 40;
        }
        return sEntryPointFromQuickCompiledCodeOffset;
    }

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

    //entry_point_from_compiled_code_
    public NativeObject getEntryPointFromCompiledCodeD() {
        return new NativeObject(address(), getDefaultEntryPointFromQuickCompiledCodeOffset());
    }

    public NativeObject getArtMethodEntry() {
        NativeObject object = getArtMethodEntryPoint().peekPointer();
        long entryPoint = object.address(); //541366891680
        AlterLog.d("Address " + entryPoint);
        if (entryPoint == ArtMethodHelper.getJniDlsymLookupStub() || entryPoint == ArtMethodHelper.getJniDlsymLookupCriticalStub()) {
            return null;
        }
        return object;
    }

    public NativeObject getArtMethodEntryFromCompiledCode() {
        NativeObject object = getEntryPointFromCompiledCode().peekPointer();

        return object;
    }


    public boolean compile() {
        if (IsCompiled()) return true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        if ((!AlterConfig.jit_compilation_allowed)) return false;
        if ((hasAccessFlags(kAccCompileDontBother))) return false;
        return JitHelper.compileMethod(this);
    }

    public Member getMemeber() {
        return this.mMember;
    }

    public boolean IsCompiled() {
        NativeObject entry = getEntryPointFromCompiledCode();
        return entry != getInterpreterBridge() && entry != ExecuteNterpImpl;
    }

    public NativeObject getInterpreterBridge() {
        return IsNative() ? art_quick_generic_jni_trampoline
                : art_quick_to_interpreter_bridge;
    }

    public void setNonCompilable() {
        if (android_version < Build.VERSION_CODES.N) return;
        int access_flags = getAccessFlags();
        if (android_version >= Build.VERSION_CODES.R) access_flags &= ~kAccPreCompiled;
        access_flags |= kAccCompileDontBother;
        setAccessFlags(access_flags);
    }

    public void setFastNative() {
        addAccessFlags(AccessFlags.kFastNative);
    }

    public boolean IsStatic() {
        return hasAccessFlags(AccessFlags.kStatic);
    }

    public boolean IsNative() {
        return hasAccessFlags(AccessFlags.kNative);
    }

    public int getAccessFlags() {
        return (int) accessFlags.get(this).peekPointer().address();
    }

    public boolean hasAccessFlags(int flags) {
        return (getAccessFlags() & flags) == flags;
    }

    public void setAccessFlags(int new_access_flags) {
        accessFlags.set(this, new_access_flags);
    }

    public void addAccessFlags(int flags) {
        setAccessFlags(getAccessFlags() | flags);
    }

    public void removeAccessFlags(int flags) {
        setAccessFlags(getAccessFlags() & ~flags);
    }

    /*
     * due to NativeObject get and set method processing 32 and 64 bit
     */
   public NativeObject getEntryPointFromCompiledCode() {
//        if (android_version == Build.VERSION_CODES.LOLLIPOP) {
//            // Android 5.0, entry_point_from_compiled_code_ is a uint64_t
//            return entryPointFromCompiledCode.get(this);
//        }
        return entryPointFromCompiledCode.get(this);
    }

    void SetEntryPointFromCompiledCode(NativeObject entry) {
//        if (android_version == Build.VERSION_CODES.LOLLIPOP) {
//            // Android 5.0, entry_point_from_compiled_code_ is a uint64_t
//            entry_point_from_compiled_code_.SetAs<uint64_t>(this, reinterpret_cast<uint64_t>(entry));
//            return;
//        }
        entryPointFromCompiledCode.set(this, entry);
    }

    public NativeObject getEntryPointFromJni() {
//        if (Android::version == Android::kL) {
//            // Android 5.0, entry_point_from_jni_ is a uint64_t
//            return reinterpret_cast<void*>(entry_point_from_jni_.GetAs<uint64_t>(this));
//        }
        return entryPointFromJni.get(this);
    }

    void SetEntryPointFromJni(NativeObject entry) {
//        if (Android::version == Android::kL) {
//            // Android 5.0, entry_point_from_jni_ is a uint64_t
//            entry_point_from_jni_.SetAs<uint64_t>(this, reinterpret_cast<uint64_t>(entry));
//            return;
//        }
        entryPointFromJni.set(this, entry);
    }

    public NativeObject getEntryPointFromInterpreter() {
//        if (Android::version == Android::kL) {
//            // Android 5.0, entry_point_from_interpreter_ is a uint64_t
//            return reinterpret_cast<void*>(entry_point_from_interpreter_->GetAs<uint64_t>(this));
//        }
        return entryPointFromInterpreter.get(this);
    }

    public void setEntryPointFromInterpreter(NativeObject entry) {
//        if (Android::version == Android::kL) {
//            // Android 5.0, entry_point_from_interpreter_ is a uint64_t
//            entry_point_from_interpreter_->SetAs<uint64_t>(this, reinterpret_cast<uint64_t>(entry));
//            return;
//        }
        entryPointFromInterpreter.set(this, entry);
    }


    public static NativeObject getQuickToInterpreterBridge() {
        return art_quick_to_interpreter_bridge;
    }

    public static void setQuickToInterpreterBridge(NativeObject entry) {
        art_quick_to_interpreter_bridge = entry;
    }

    private static int findOffset(long start, long value, int size, int step) {
        for (int offset = 0; offset < size; offset += step) {
            NativeObject current = new NativeObject((start) + offset);
            if (current.peekPointer().address() == value) return offset;
        }
        return -1;
    }

    private static long alignUp(long value, long align_with) {
        long alignment = value % align_with;
        if (alignment != 0) {
            value += (align_with - alignment);
        }
        return value;
    }

    private static int difference(long a, long b) {
        long size = b - a;
        if (size < 0) size = -size;
        return (int) size;
    }


}
