package dev.answer.alterapp;

import android.util.Log;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dev.answer.altercore.AlterCore;
import dev.answer.altercore.callback.MethodHook;
import dev.answer.altercore.core.HookParams;

/**
 * Exercises AlterCore's "call the original implementation" capability
 * ({@code HookParams.invokeOriginalMethod()} / {@code AlterCore.invokeOriginalMethod()}).
 * This is AlterCore's own reimplementation of the pattern, not the real Xposed API —
 * see {@link TestXposed} for tests against the genuine {@code XposedBridge}.
 */
public class TestInvokeOriginal {

    private static final String TAG = "TestInvokeOriginal";

    public String multiply(int value) {
        return "Result: " + (value * 2);
    }

    public static String staticCombine(String left, String right) {
        return left + "-" + right;
    }

    public static List<TestResult> runTests() {
        List<TestResult> results = new ArrayList<>();
        results.add(testInvokeOriginalWhileHooked());
        results.add(testInvokeOriginalWithCustomArgs());
        results.add(testInvokeOriginalWithoutHook());
        return results;
    }

    private static TestResult testInvokeOriginalWhileHooked() {
        String testName = "invokeOriginalMethod() while hooked (instance method)";
        try {
            Method method = TestInvokeOriginal.class.getMethod("multiply", int.class);
            TestInvokeOriginal instance = new TestInvokeOriginal();

            TestResult.Stage baseline = new TestResult.Stage(
                    "Before hook (direct call)", "Result: 10", instance.multiply(5));

            String[] capturedOriginal = new String[1];

            MethodHook.Unhook unhook = AlterCore.hook(method, new MethodHook() {
                @Override
                public void before(HookParams params) throws Throwable {
                    super.before(params);
                    Object original = params.invokeOriginalMethod();
                    capturedOriginal[0] = String.valueOf(original);
                }

                @Override
                public void after(HookParams params) throws Throwable {
                    super.after(params);
                    params.setResult("Verified original: " + capturedOriginal[0]);
                }
            });

            TestResult.Stage hooked = new TestResult.Stage(
                    "After hook (result derived from original)",
                    "Verified original: Result: 10",
                    instance.multiply(5));
            Log.d(TAG, "Hooked result: " + hooked.actual);

            unhook.unhook();

            TestResult.Stage restored = new TestResult.Stage(
                    "After unhook (direct call)", "Result: 10", instance.multiply(5));
            Log.d(TAG, "Restored result: " + restored.actual);

            return new TestResult(testName, baseline, hooked, restored);
        } catch (Throwable e) {
            return TestResult.failure(testName, e);
        }
    }

    private static TestResult testInvokeOriginalWithCustomArgs() {
        String testName = "invokeOriginalMethod(Object, Object...) with custom args (static method)";
        try {
            Method method = TestInvokeOriginal.class.getMethod(
                    "staticCombine", String.class, String.class);

            TestResult.Stage baseline = new TestResult.Stage(
                    "Before hook (direct call)", "A-B", staticCombine("A", "B"));

            String[] capturedOriginal = new String[1];

            MethodHook.Unhook unhook = AlterCore.hook(method, new MethodHook() {
                @Override
                public void before(HookParams params) throws Throwable {
                    super.before(params);
                    Object original = params.invokeOriginalMethod(null, "X", "Y");
                    capturedOriginal[0] = String.valueOf(original);
                }

                @Override
                public void after(HookParams params) throws Throwable {
                    super.after(params);
                    params.setResult("Custom-arg original: " + capturedOriginal[0]);
                }
            });

            TestResult.Stage hooked = new TestResult.Stage(
                    "After hook (result derived from custom-arg call)",
                    "Custom-arg original: X-Y",
                    staticCombine("A", "B"));
            Log.d(TAG, "Hooked result: " + hooked.actual);

            unhook.unhook();

            TestResult.Stage restored = new TestResult.Stage(
                    "After unhook (direct call)", "A-B", staticCombine("A", "B"));
            Log.d(TAG, "Restored result: " + restored.actual);

            return new TestResult(testName, baseline, hooked, restored);
        } catch (Throwable e) {
            return TestResult.failure(testName, e);
        }
    }

    private static TestResult testInvokeOriginalWithoutHook() {
        String testName = "AlterCore.invokeOriginalMethod() on an un-hooked method";
        try {
            Method method = TestInvokeOriginal.class.getMethod("multiply", int.class);
            TestInvokeOriginal instance = new TestInvokeOriginal();

            Object result = AlterCore.invokeOriginalMethod(method, instance, 7);

            TestResult.Stage onlyStage = new TestResult.Stage(
                    "Direct invokeOriginalMethod() call", "Result: 14", String.valueOf(result));
            Log.d(TAG, "Un-hooked invokeOriginalMethod result: " + onlyStage.actual);

            return new TestResult(testName, onlyStage, null, null);
        } catch (Throwable e) {
            return TestResult.failure(testName, e);
        }
    }
}