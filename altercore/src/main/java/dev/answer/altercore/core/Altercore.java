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

import static dev.answer.altercore.core.DefineField.*;

import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import dev.answer.altercore.AlterConfig;
import dev.answer.altercore.utils.AlterException;
import dev.answer.altercore.utils.AlterLog;
import dev.answer.altercore.utils.MemberOffest;
import dev.answer.altercore.utils.UnsafeWrapper;
import dev.tmpfs.libcoresyscall.core.NativeAccess;
import dev.tmpfs.libcoresyscall.elfloader.SymbolResolver;
import sun.misc.Unsafe;

public class Altercore {
    private static Unsafe theUnsafe = UnsafeWrapper.getUnsafe();
    private static Class objectArrayClass = Object[].class;


    public static long nativePeer;
    private static long jit_compile_method;
    private static long jit_load;
    private static long addWeakGloablReference;
    private static long jit_compiler_handle_;
    private static SymbolResolver art_module;
    private static SymbolResolver art_compiler_module;
    private static long fromReflectedMethod;
    private static long getEnv;


    private  static native void a();
    private  static native void b();

    private interface Interface {
        void c();
    }

    private static SymbolResolver libopenjdkjvmti ;

    public static void init(){

    }

    private static int expected_access_flags;

    static {

        try {
            Field field =Thread.class.getDeclaredField("nativePeer");
            field.setAccessible(true);
            nativePeer = field.getLong(Thread.currentThread());



            art_module = SymbolResolver.getModule("libart.so");
            art_compiler_module = SymbolResolver.getModule("libart-compiler.so");
            libopenjdkjvmti = SymbolResolver.getModule("libopenjdkjvmti.so");

            jit_compile_method = art_compiler_module.getSymbolAddress("_ZN3art3jit11JitCompiler13CompileMethodEPNS_6ThreadEPNS0_15JitMemoryRegionEPNS_9ArtMethodEbb");
            jit_load = art_compiler_module.getSymbolAddress("jit_load");
            getEnv = art_module .getSymbolAddress("_ZN3art3JII6GetEnvEP7_JavaVMPPvi");

            addWeakGloablReference = art_module.getSymbolAddress("_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadENS_6ObjPtrINS_6mirror6ObjectEEE");

            fromReflectedMethod = art_module.getSymbolAddress("_ZN3art12_GLOBAL__N_18CheckJNI19FromReflectedMethodEP7_JNIEnvP8_jobject");


            jit_compiler_handle_ = NativeAccess.callPointerFunction(jit_load, FALSE);

            System.out.println("This is addWeakGloablReference : " + addWeakGloablReference);
            System.out.println("This is jit_compile_method : " + jit_compile_method);
            System.out.println("This is jit_load :" + jit_load);
            System.out.println("This is jit_compiler_handle_ :" + jit_compiler_handle_);

            System.out.println("GetEnv : "+ getEnv().address());

            JitHelper.init(art_module, art_compiler_module);
            
            ArtMethodObject artMethod_a = new ArtMethodObject(Altercore.class.getDeclaredMethod("a"));
            ArtMethodObject artMethod_b = new ArtMethodObject(Altercore.class.getDeclaredMethod("b"));

             expected_access_flags = artMethod_a.getMemeber().getModifiers();
            expected_access_flags = AccessFlags.kPrivate | AccessFlags.kStatic | AccessFlags.kNative;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                expected_access_flags |= AccessFlags.kPublicApi;
            }

            ArtMethodObject artMethod_c = new ArtMethodObject(Altercore.Interface.class.getDeclaredMethod("c"));


            AlterLog.d("artMethod :" + artMethod_a.address());
            AlterLog.d("FromCompiledCode :" + artMethod_a.getArtMethodEntryFromCompiledCode().address());
            AlterLog.d("getArtMethodEntry :" + artMethod_a.getArtMethodEntry());
//            AlterLog.d(""+ JitHelper.compileMethod(artMethod));

            NativeObject runtime = new NativeObject( art_module.getSymbolAddress("_ZN3art7Runtime9instance_E"));
            AlterLog.d("Runtime: " + runtime.address());
            AlterLog.d("Runtime :" + runtime.peekPointer().address());




        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static NativeObject getSymbol(SymbolResolver symbolResolver, String symbol){
        long address = symbolResolver.getSymbolAddress(symbol);
       return new NativeObject(address);
    }

    // 双指针
    public static NativeObject getEnv() {
        // Assuming you have access to the JavaVM pointer
        long javaVM = NativeAccess.getJavaVM();
        int size = theUnsafe.addressSize();
        // Allocate memory for the JNIEnv pointer
        try  {
            NativeObject envPtr = new NativeObject(false, size);
            // Call GetEnv - this should write the JNIEnv pointer to our allocated memory
            long result = NativeAccess.callPointerFunction(
                    getEnv,
                    javaVM,
                    envPtr.address(),
                    JNI_VERSION_1_6);
            if (result == JNI_OK) {
                return envPtr;
            } else {
                throw new RuntimeException("Failed to get JNI environment, error: " + result);
            }
        }catch (Exception e){
            throw new AlterException("Failed to get JNI environment, error: " + e.getMessage());
        }
    }


    public static void hook0(long thread, Class<?> declaring, Object hookRecord,
                             Member javaTarget, Member javaBridge, boolean isInlineHook,
                             boolean jni, boolean proxy){

        ArtMethodObject target = new ArtMethodObject(javaTarget);
        ArtMethodObject bridge = new ArtMethodObject(javaBridge);

        if (AlterConfig.jit_compilation_allowed && AlterConfig.auto_compile_bridge) {
            // The bridge method entry will be hardcoded in the trampoline, subsequent optimization
            // operations that require modification of the bridge method entry will not take effect.
            // Try to do JIT compilation first to get the best performance.
//            bridge->Compile(thread);
        }

    }

    public static void compile_Method(Method method, long nativePeer){

    }

    // &runtime runtime的指针
    public static NativeObject getRuntime(){
        NativeObject runtime = new NativeObject( art_module.getSymbolAddress("_ZN3art7Runtime9instance_E"));
      return runtime.peekPointer();
    }


    public static NativeObject getThreadPtr(){
        try {
            Field field = Thread.class.getDeclaredField("nativePeer");
            field.setAccessible(true);
            long nativePeer = field.getLong(Thread.currentThread());
            return new NativeObject(nativePeer);
        }catch (Exception e){
            AlterLog.e("Geting nativePeer, but Failure! reason:" + e.getMessage());
        }
        return NativeObject.EMPTY_OBJECT;
    }



    public static Object getObject(long nativePeer, long address) {
        long weakRef = NativeAccess.callPointerFunction(
                addWeakGloablReference,
                NativeAccess.getJavaVM(),
                (nativePeer),
                (address)
        );

        long NewLocalRef = art_module.getSymbolAddress("_ZN3art3JNIILb1EE11NewLocalRefEP7_JNIEnvP8_jobject");
        long DeleteWeakGlobalRef = art_module.getSymbolAddress("_ZN3art3JNIILb1EE19DeleteWeakGlobalRefEP7_JNIEnvP8_jobject");
        long newLocalObj = NativeAccess.callPointerFunction(NewLocalRef, NativeAccess.getJavaVM(), weakRef);
        NativeAccess.callPointerFunction(DeleteWeakGlobalRef, NativeAccess.getJavaVM(), weakRef);

        return newLocalObj; // 正确转换回 Java 对象
    }


}
