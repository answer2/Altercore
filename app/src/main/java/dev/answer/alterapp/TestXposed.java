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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

/**
 * Exercises the real LSPosed/Xposed {@link XposedBridge} API directly, self-hooking
 * methods within this same process. This requires the app to be enabled as an active
 * Xposed module with itself included in scope; see {@code XposedEntry} and
 * {@code assets/xposed_init}.
 */
public class TestXposed {

    private static final String TAG = "TestXposed";

    public String computeValue(int value) {
        return "Computed: " + (value * 3);
    }

    public static String staticJoin(String left, String right) {
        return left + "+" + right;
    }

    /** Simple target class used only to exercise constructor hooking. */
    public static class Widget {
        public final String label;

        public Widget(String label) {
            this.label = label;
        }
    }

    /**
     * Runs every real-Xposed scenario. If {@link XposedBridge} is not injected into this
     * process (i.e. the app is not an active, in-scope Xposed module), a single explanatory
     * {@link TestResult} is returned instead of letting every subsequent test fail with a
     * confusing {@link NoClassDefFoundError}.
     */
    public static List<TestResult> runTests() {
        List<TestResult> results = new ArrayList<>();

        TestResult environment = checkXposedEnvironment();
        results.add(environment);
        if (!environment.passed) {
            return results;
        }

        results.add(testInstanceMethodHookWithOriginalCall());
        results.add(testStaticMethodReplacement());
        results.add(testConstructorArgumentHook());
        return results;
    }

    /**
     * Verifies that {@link XposedBridge} has actually been injected into this process by
     * an active Xposed framework. Without this, every hook call below would fail with a
     * low-level classloading error rather than a clear message.
     */
    private static TestResult checkXposedEnvironment() {
        String testName = "Xposed environment check";
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            TestResult.Stage stage = new TestResult.Stage(
                    "XposedBridge injected into process", "available", "available");
            return new TestResult(testName, stage, null, null);
        } catch (Throwable e) {
            return TestResult.failure(testName, new IllegalStateException(
                    "XposedBridge is not available in this process. Enable this app as a "
                            + "module in LSPosed, add it to its own scope, then restart the app.", e));
        }
    }

    /**
     * Hooks an instance method whose {@code beforeHookedMethod()} calls
     *  to fetch the real
     * (un-hooked) result, then folds it into the final result via {@code setResult()}.
     * <p>
     * Note: {@code setResult()} is called inside {@code beforeHookedMethod()} itself.
     * Xposed only auto-invokes the original implementation if the result is still unset
     * once {@code beforeHookedMethod()} returns — setting it here avoids invoking the
     * original a second time.
     */
    private static TestResult testInstanceMethodHookWithOriginalCall() {
        String testName = "XposedBridge.hookMethod (instance) + invokeOriginalMethod()";
        try {
            Method method = TestXposed.class.getMethod("computeValue", int.class);
            TestXposed instance = new TestXposed();

            TestResult.Stage baseline = new TestResult.Stage(
                    "Before hook (direct call)", "Computed: 30", instance.computeValue(10));

            XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object original = XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    param.setResult("Verified original: " + original);
                }
            });

            TestResult.Stage hooked = new TestResult.Stage(
                    "After hook (derived from original)",
                    "Verified original: Computed: 30",
                    instance.computeValue(10));
            Log.d(TAG, "Hooked result: " + hooked.actual);

            unhook.unhook();

            TestResult.Stage restored = new TestResult.Stage(
                    "After unhook (direct call)", "Computed: 30", instance.computeValue(10));
            Log.d(TAG, "Restored result: " + restored.actual);

            return new TestResult(testName, baseline, hooked, restored);
        } catch (Throwable e) {
            return TestResult.failure(testName, e);
        }
    }

    /**
     * Hooks a static method with {@link XC_MethodReplacement}, fully replacing its
     * behavior instead of running before/after around the original.
     */
    private static TestResult testStaticMethodReplacement() {
        String testName = "XposedBridge.hookMethod (static) + XC_MethodReplacement";
        try {
            Method method = TestXposed.class.getMethod("staticJoin", String.class, String.class);

            TestResult.Stage baseline = new TestResult.Stage(
                    "Before hook (direct call)", "A+B", staticJoin("A", "B"));

            XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return "Replaced: " + param.args[0] + "|" + param.args[1];
                }
            });

            TestResult.Stage hooked = new TestResult.Stage(
                    "After hook (fully replaced)", "Replaced: A|B", staticJoin("A", "B"));
            Log.d(TAG, "Hooked result: " + hooked.actual);

            unhook.unhook();

            TestResult.Stage restored = new TestResult.Stage(
                    "After unhook (direct call)", "A+B", staticJoin("A", "B"));
            Log.d(TAG, "Restored result: " + restored.actual);

            return new TestResult(testName, baseline, hooked, restored);
        } catch (Throwable e) {
            return TestResult.failure(testName, e);
        }
    }

    /**
     * Hooks a constructor's {@code beforeHookedMethod()} to mutate an incoming argument
     * before the real constructor body runs.
     */
    private static TestResult testConstructorArgumentHook() {
        String testName = "XposedBridge.hookMethod (constructor)";
        try {
            Constructor<Widget> constructor = Widget.class.getConstructor(String.class);

            TestResult.Stage baseline = new TestResult.Stage(
                    "Before hook", "original", new Widget("original").label);

            XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = "Hooked constructor argument";
                }
            });

            TestResult.Stage hooked = new TestResult.Stage(
                    "After hook", "Hooked constructor argument", new Widget("original").label);
            Log.d(TAG, "Hooked value: " + hooked.actual);

            unhook.unhook();

            TestResult.Stage restored = new TestResult.Stage(
                    "After unhook", "original", new Widget("original").label);
            Log.d(TAG, "Restored value: " + restored.actual);

            return new TestResult(testName, baseline, hooked, restored);
        } catch (Throwable e) {
            return TestResult.failure(testName, e);
        }
    }
}