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

import android.util.Log;

import java.lang.reflect.Method;

import dev.answer.altercore.AlterCore;
import dev.answer.altercore.callback.MethodHook;
import dev.answer.altercore.callback.MethodReplacement;
import dev.answer.altercore.core.HookParams;

public class TestHook {

    private String mText = null;

    private static final String TAG = "TestHook";

    public String a(String arg1) {
        return "args" + arg1;
    }

    public static String b() {
        return "origin b";
    }

    public TestHook(String arg1){
        mText = arg1;
    }

    public static void test() {
        try {
            Method a_method = TestHook.class.getMethod("a", String.class);
            Method b_method = TestHook.class.getMethod("b");

            MethodHook.Unhook un = AlterCore.hook(a_method, new MethodHook() {
                @Override
                public void after(HookParams params) throws Throwable {
                    super.after(params);
                    params.setResult("你好哇");
                }

                @Override
                public void before(HookParams params) throws Throwable {
                    super.before(params);

                }
            });

            MethodHook.Unhook un_ = AlterCore.hook(b_method, new MethodReplacement() {
                @Override
                protected Object replace(HookParams params) throws Throwable {
                    return "Replace b";
                }
            });

            var test = new TestHook("Test");

            Log.d(TAG,"Hook : " +  test.a(" Hello"));

            un.unhook();

            Log.d(TAG,"Origin : " +  test.a(" Hello"));


            Log.d(TAG,"Hook : " +  b());

            un_.unhook();

            Log.d(TAG,"Origin : " +  b());

            var constructor = TestHook.class.getConstructor(String.class);

            var test1 = new TestHook("Test");

            Log.d(TAG,"Origin : " +  test1.mText);

            AlterCore.hook(constructor, new MethodHook(){
                @Override
                public void before(HookParams params) throws Throwable {
                    super.before(params);
                    params.args[0] = "Hook Success!";
                }
            });

            var test2 = new TestHook("Test");

            Log.d(TAG,"Hook : " +  test2.mText);


        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

}
