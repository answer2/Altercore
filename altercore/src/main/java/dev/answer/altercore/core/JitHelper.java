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
import static dev.answer.altercore.utils.UnsafeWrapper.getUnsafe;

import android.os.Build;

import dev.answer.altercore.NativeImpl;
import dev.answer.altercore.utils.AlterLog;
import dev.answer.altercore.utils.MemberOffest;
import dev.tmpfs.libcoresyscall.elfloader.SymbolResolver;

public class JitHelper {
    private static NativeObject global_compiler_ptr;
    private static NativeObject jit_load;
    private static NativeObject jit_update_options_ptr;
    private static NativeObject self_compiler;
    private static NativeObject jit_compile_method;
    private static MemberOffest CompilerOptions_inline_max_code_units;
    private static NativeObject jit_compile_method_q;

    public static void init(SymbolResolver art_lib_handle, SymbolResolver jit_lib_handle){
        global_compiler_ptr = NativeImpl.getSymbolObject(art_lib_handle,
                "_ZN3art3jit3Jit20jit_compiler_handle_E");

        jit_load = NativeImpl.getSymbolObject(jit_lib_handle,
                 "jit_load");

        if (!jit_load.isNull()){
            long generate_debug_info = FALSE;
             self_compiler = jit_load.invokePointerObjectL(generate_debug_info);
        }else {
            AlterLog.w("Failed to create new JitCompiler: jit_load not found");
        }

        NativeObject jit_compile_method = NativeImpl.getSymbolObject(jit_lib_handle, "jit_compile_method");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Jit::jit_compile_method_q = reinterpret_cast<bool (*)(void*, void*, void*, bool, bool)>(jit_compile_method);
            jit_compile_method_q = jit_compile_method;
            jit_update_options_ptr = NativeImpl.getSymbolObject(art_lib_handle,
                    "_ZN3art3jit3Jit19jit_update_options_E");
        } else {
            JitHelper.jit_compile_method = jit_compile_method;
            // Jit::jit_compile_method = reinterpret_cast<bool (*)(void*, void*, void*, bool)>(jit_compile_method);
        }

        int thresholds_count = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 5 : 6;
        //CompilerOptions_inline_max_code_units = new Member<void, size_t>(
        //            sizeof(void*) + thresholds_count * sizeof(size_t));

        CompilerOptions_inline_max_code_units = new MemberOffest(getUnsafe().addressSize() * 2 + thresholds_count);

    }

    public static boolean compileMethod(NativeObject method){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlterLog.w("JIT compilation is not supported in Android R yet");
            return false;
        }
        NativeObject compiler = getCompiler();
        if (compiler.isNull()) {
            AlterLog.e("No JitCompiler available for JIT compilation!");
            return false;
        }

        boolean result;

        // JIT compilation will modify the state of the thread, so we backup and restore it after compilation.
        NativeObject thread = Altercore.getThreadPtr();
        int origin_state_and_flags = thread.getInt(0);//thread->GetStateAndFlags();
        if (jit_compile_method.isNotNull()) {
            //result = jit_compile_method(compiler, method, thread, false/*osr*/);
            result = jit_compile_method.invokePointerObject(compiler, method, thread, FALSE_OBJECT).isNotNull();
        } else if (jit_compile_method_q.isNotNull()) {
            //result = jit_compile_method_q(compiler, method, thread, false/*baseline*/, false/*osr*/);
            result = jit_compile_method_q.invokePointerObject(compiler, method, thread, FALSE_OBJECT, FALSE_OBJECT).isNotNull();
        } else {
            AlterLog.e("Compile method failed: jit_compile_method not found");
            return false;
        }

        thread.putInt(0, origin_state_and_flags);

        return result;
    }

    public static NativeObject getCompiler() {
        return (!self_compiler.isNull()) ? self_compiler : getGlobalCompiler();
    }

    public static NativeObject getGlobalCompiler() {
        return (!global_compiler_ptr.isNull()) ? global_compiler_ptr : NativeObject.EMPTY_OBJECT;
    }
}
