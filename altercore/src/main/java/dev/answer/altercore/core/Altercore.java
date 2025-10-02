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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dev.answer.altercore.utils.AlterException;
import dev.answer.altercore.utils.AlterLog;
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


    public static native void a();

    public static void b(){
        AlterLog.d("I am b");
    }


    private static SymbolResolver libopenjdkjvmti ;

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
            
            NativeObject artMethod = new ArtMethodObject(Altercore.class.getDeclaredMethod("a"));
            AlterLog.d("artMethod :" + artMethod.address());

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

    public static void compile_Method(Method method, long nativePeer){

    }

    // &runtime runtime的指针
    public static NativeObject getRuntime(){
        NativeObject runtime = new NativeObject( art_module.getSymbolAddress("_ZN3art7Runtime9instance_E"));
      return runtime.peekPointer();
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
