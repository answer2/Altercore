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

import dev.answer.altercore.AlterCore;
import dev.answer.altercore.callback.MethodHook;
import dev.answer.altercore.callback.MethodReplacement;
import dev.answer.altercore.core.HookParams;

/**
 * Sample target class used to exercise AlterCore's hooking capabilities:
 * instance method hooking, static method replacement, and constructor hooking.
 */
public class TestHook {

    private static final String TAG = "TestHook";

    private String fieldValue;

    public TestHook(String initialValue) {
        fieldValue = initialValue;
    }

    public String greet(String name) {
        return "Hello, " + name;
    }

    public static String staticGreeting() {
        return "Original static greeting";
    }

    /**
     * Runs every hook scenario and returns a list of results. Never throws — any
     * failure is captured as a {@link TestResult}.
     */
    public static List<TestResult> runTests() {
        List<TestResult> results = new ArrayList<>();
        results.add(testInstanceMethodHook());
        results.add(testStaticMethodReplacement());
        results.add(testConstructorHook());
        return results;
    }

    /**
     * Verifies that hooking an instance method and overriding its result in
     * {@code after()} takes effect, and that {@code unhook()} restores the
     * original behavior afterward.
     */
    private static TestResult testInstanceMethodHook() {
        String testName = "Instance method hook: greet(String)";
        try {
            Method method = TestHook.class.getMethod("greet", String.class);
            TestHook instance = new TestHook("initial");

            TestResult.Stage baseline = new TestResult.Stage(
                    "Before hook", "Hello, World", instance.greet("World"));

            MethodHook.Unhook unhook = AlterCore.hook(method, new MethodHook() {
                @Override
                public void after(HookParams params) throws Throwable {
                    super.after(params);
                    params.setResult("Hooked greeting");
                }
            });

            TestResult.Stage hooked = new TestResult.Stage(
                    "After hook", "Hooked greeting", instance.greet("World"));
            Log.d(TAG, "Hooked result: " + hooked.actual);

            unhook.unhook();

            TestResult.Stage restored = new TestResult.Stage(
                    "After unhook", "Hello, World", instance.greet("World"));
            Log.d(TAG, "Restored result: " + restored.actual);

            return new TestResult(testName, baseline, hooked, restored);
        } catch (Throwable e) {
            return TestResult.failure(testName, e);
        }
    }

    /**
     * Verifies that a static method can be fully replaced via {@link MethodReplacement},
     * and that {@code unhook()} restores the original implementation afterward.
     */
    private static TestResult testStaticMethodReplacement() {
        String testName = "Static method replacement: staticGreeting()";
        try {
            Method method = TestHook.class.getMethod("staticGreeting");

            TestResult.Stage baseline = new TestResult.Stage(
                    "Before hook", "Original static greeting", staticGreeting());

            MethodHook.Unhook unhook = AlterCore.hook(method, new MethodReplacement() {
                @Override
                protected Object replace(HookParams params) throws Throwable {
                    return "Replaced static greeting";
                }
            });

            TestResult.Stage hooked = new TestResult.Stage(
                    "After hook", "Replaced static greeting", staticGreeting());
            Log.d(TAG, "Hooked result: " + hooked.actual);

            unhook.unhook();

            TestResult.Stage restored = new TestResult.Stage(
                    "After unhook", "Original static greeting", staticGreeting());
            Log.d(TAG, "Restored result: " + restored.actual);

            return new TestResult(testName, baseline, hooked, restored);
        } catch (Throwable e) {
            return TestResult.failure(testName, e);
        }
    }

    /**
     * Verifies that a constructor can be hooked to mutate its incoming argument
     * in {@code before()}, and that {@code unhook()} restores the original
     * constructor behavior afterward.
     */
    private static TestResult testConstructorHook() {
        String testName = "Constructor hook: TestHook(String)";
        try {
            Constructor<TestHook> constructor = TestHook.class.getConstructor(String.class);

            TestHook baselineInstance = new TestHook("original");
            TestResult.Stage baseline = new TestResult.Stage(
                    "Before hook", "original", baselineInstance.fieldValue);
            Log.d(TAG, "Baseline value: " + baseline.actual);

            MethodHook.Unhook unhook = AlterCore.hook(constructor, new MethodHook() {
                @Override
                public void before(HookParams params) throws Throwable {
                    super.before(params);
                    params.args[0] = "Hooked constructor argument";
                }
            });

            TestHook hookedInstance = new TestHook("original");
            TestResult.Stage hooked = new TestResult.Stage(
                    "After hook", "Hooked constructor argument", hookedInstance.fieldValue);
            Log.d(TAG, "Hooked value: " + hooked.actual);

            unhook.unhook();

            TestHook restoredInstance = new TestHook("original");
            TestResult.Stage restored = new TestResult.Stage(
                    "After unhook", "original", restoredInstance.fieldValue);
            Log.d(TAG, "Restored value: " + restored.actual);

            return new TestResult(testName, baseline, hooked, restored);
        } catch (Throwable e) {
            return TestResult.failure(testName, e);
        }
    }
}