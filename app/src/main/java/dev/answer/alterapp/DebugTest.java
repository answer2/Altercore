/*
 * Copyright (C) 2026 AnswerDev
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
package dev.answer.alterapp;

import java.lang.reflect.Method;

import dev.answer.altercore.AlterCore;
import dev.answer.altercore.callback.MethodHook;
import dev.answer.altercore.core.HookParams;

public class DebugTest {

    public String multiply(int value) {
        return "Result: " + (value * 2);
    }

    public static void init(){
        try {
            Method method = DebugTest.class.getMethod("multiply", int.class);

            MethodHook.Unhook unhook = AlterCore.hook(method, new MethodHook() {
                @Override
                public void before(HookParams params) throws Throwable {
                    super.before(params);
                    Object original = params.invokeOriginalMethod();

                    System.out.println("before " + original);
                }

                @Override
                public void after(HookParams params) throws Throwable {
                    super.after(params);
                   System.out.println("after");
                }
            });


            var debug = new DebugTest();

            System.out.println(debug.multiply(5));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
