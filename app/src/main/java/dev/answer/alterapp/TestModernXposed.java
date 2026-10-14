package dev.answer.alterapp;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import dev.answer.altercore.xposed.AlterXposed;
import dev.answer.altercore.xposed.modern.ModernModule;
import dev.answer.altercore.xposed.modern.ModernModuleLoader;

/**
 * Exercises AlterCore's modern libxposed path end to end: the app is handed to its own framework
 * as a module (its {@code META-INF/xposed/*} descriptors and {@link ModernChainModule} entry), the
 * framework attaches it and reports the package to it, and the chain hooks the entry installs are
 * then probed before, while, and after they are active.
 */
public class TestModernXposed {

    private static final String TAG = "TestModernXposed";
    private static final String MODULE_PACKAGE = "dev.answer.alterapp";

    private static boolean sModuleLoaded;

    public static List<TestResult> runTests(Context context) {
        List<TestResult> results = new ArrayList<>();
        try {
            Map<String, String> before = probe(false);

            DetachingModule.reset();
            LegacyCompatModule.beginRound();
            ensureModuleLoaded(context);
            dispatchPackageLoad(context);
            results.add(testDetachIsPerEntry());

            if (!ModernChainModule.isInstalled()) {
                results.add(TestResult.failure("Modern module loading",
                        new IllegalStateException("No chain hook was installed: the framework never "
                                + "reached ModernChainModule.installHooks (see logcat for AlterXposed)")));
                return results;
            }

            Map<String, String> during = probe(true);
            results.add(testLegacyBeforeModernChain());
            results.addAll(testHotReloads(context));
            Map<String, String> after = probe(false);

            for (Map.Entry<String, String> expectation : LABELS.entrySet()) {
                String key = expectation.getKey();
                results.add(new TestResult(expectation.getValue(),
                        stage(key, 0, before),
                        stage(key, 1, during),
                        stage(key, 2, after)));
            }
        } catch (Throwable t) {
            Log.e(TAG, "Modern chain tests failed", t);
            results.add(TestResult.failure("Modern chain tests", t));
        }
        return results;
    }

    /**
     * The second entry class detached itself during {@code onPackageLoaded}, so it must have seen
     * exactly one package callback and no ready callback, while the first entry keeps working.
     */
    private static TestResult testDetachIsPerEntry() {
        return new TestResult("API 102: detach() silences only the entry that called it",
                null,
                new TestResult.Stage("One dispatch round", "1,0|chainInstalled=true",
                        DetachingModule.counters() + "|chainInstalled="
                                + ModernChainModule.isInstalled()),
                null);
    }

    /**
     * Two reload rounds: the outgoing code must be frozen, the saved state and the trigger extras
     * must reach the new code, the previous generation's handles must be delivered and removed,
     * and the new generation must reinstall working hooks.
     */
    private static List<TestResult> testHotReloads(Context context) {
        List<TestResult> results = new ArrayList<>();
        String path = context.getApplicationInfo().sourceDir;

        Bundle extras = new Bundle();
        extras.putString("token", "abc");
        int genBefore = generationOf(path);
        ModernModuleLoader.ReloadResult first = ModernModuleLoader.reload(path, extras, null);
        String reportFirst = describeReload(first, path, genBefore);
        results.add(new TestResult("Hot reload: frozen old code, state hand-off, old hooks retired",
                null,
                new TestResult.Stage("First reload",
                        "SUCCESS|carried|abc|IllegalStateException|some|gen+1|shout=HI![outer]",
                        reportFirst),
                null));

        genBefore = generationOf(path);
        ModernModuleLoader.ReloadResult second = ModernModuleLoader.reload(path, extras, null);
        String reportSecond = describeReload(second, path, genBefore);
        results.add(new TestResult("Hot reload: generations keep advancing",
                null,
                new TestResult.Stage("Second reload",
                        "SUCCESS|carried|abc|IllegalStateException|some|gen+1|shout=HI![outer]",
                        reportSecond),
                null));

        // A module that was never loaded cannot be reloaded.
        ModernModuleLoader.ReloadResult unknown =
                ModernModuleLoader.reload("/data/nonexistent-module.apk", extras, null);
        results.add(new TestResult("Hot reload: unknown module path is refused",
                null,
                new TestResult.Stage("Unknown path", "NOT_FOUND", String.valueOf(unknown)),
                null));

        ModernChainModule.uninstallAll();
        return results;
    }

    private static String describeReload(ModernModuleLoader.ReloadResult result, String path,
                                         int generationBefore) {
        return result + "|" + ModernChainModule.carriedState + "|" + ModernChainModule.extrasSeen
                + "|" + ModernChainModule.frozenProbe + "|"
                + (ModernChainModule.oldHandlesAtReload > 0 ? "some" : "none")
                + "|gen+" + (generationOf(path) - generationBefore)
                + "|shout=" + ChainTarget.shout("hi");
    }

    private static int generationOf(String path) {
        for (ModernModule module : ModernModuleLoader.getLoadedModules()) {
            if (module.getModulePath().equals(path)) {
                return module.getCurrentGeneration();
            }
        }
        return -1;
    }

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("api", "attachFramework + getApiVersion");
        LABELS.put("shout", "Nested chain: priority order + proceed(Object[])");
        LABELS.put("blocked", "Hooker without proceed() replaces the result");
        LABELS.put("ctorProceed", "Constructor: proceed() initializes before post-proceed code");
        LABELS.put("ctorBlock", "Constructor: no proceed() leaves the object uninitialized");
        LABELS.put("mutate", "Void method: work after proceed() runs on the argument");
        LABELS.put("add", "Primitive return value passes through the chain");
        LABELS.put("boom", "Exception from proceed() propagates to the hooker");
        LABELS.put("protective", "ExceptionMode.PROTECTIVE drops a faulting hooker");
        LABELS.put("passthrough", "ExceptionMode.PASSTHROUGH forwards the fault to the caller");
        LABELS.put("invoker", "Invoker ORIGIN bypasses the chain, Chain.FULL runs it");
        LABELS.put("hookId", "API 102: a repeated hook id replaces the previous hook");
        LABELS.put("replaceHook", "API 102: replaceHook swaps the hooker, old handle is inert");
        LABELS.put("legacyCompat", "Legacy module with unsupported interfaces still loads and hooks");
    }

    private static TestResult.Stage stage(String key, int phase, Map<String, String> probe) {
        String expected = EXPECTED.get(key)[phase];
        if (expected == null) {
            // The module instance survives a re-run of the suite, so the "before any hook"
            // snapshot cannot assert a pre-attach value.
            return null;
        }
        return new TestResult.Stage(PHASES.get(phase), expected, probe.get(key));
    }

    private static final List<String> PHASES =
            java.util.Arrays.asList("Before hook", "Chain active", "Restored");

    /** Expected observation per phase: before any hook, while the chain is installed, after unhook. */
    private static final Map<String, String[]> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("api", new String[] { null, "102", "102" });
        EXPECTED.put("shout", new String[] {
                "HI||1",
                "HI![outer]|outer:enter,inner:enter,inner:exit,outer:exit|1",
                "HI||1" });
        EXPECTED.put("blocked", new String[] { "original|1", "replaced|0", "original|1" });
        EXPECTED.put("ctorProceed", new String[] { "tag|1", "tag[inited]|1", "tag|1" });
        EXPECTED.put("ctorBlock", new String[] { "int:7|1", "null|0", "int:7|1" });
        EXPECTED.put("mutate", new String[] { "-body", "-body[after]", "-body" });
        EXPECTED.put("add", new String[] { "3", "6", "3" });
        EXPECTED.put("boom", new String[] {
                "IllegalStateException:boom|0",
                "IllegalStateException:boom|1",
                "IllegalStateException:boom|0" });
        EXPECTED.put("protective", new String[] { "orig:x", "orig:x", "orig:x" });
        EXPECTED.put("passthrough", new String[] {
                "orig:x", "IllegalStateException:hooker-fault", "orig:x" });
        EXPECTED.put("hookId", new String[] { "orig:x", "second", "orig:x" });
        EXPECTED.put("replaceHook", new String[] {
                "orig:x",
                "before-replace|after-replace|IllegalStateException|after-replace|null",
                "orig:x" });
        // The legacy hook is never uninstalled by this suite, so only the steady state is asserted.
        EXPECTED.put("legacyCompat", new String[] {
                null, "1|1|direct:x|true", "1|1|direct:x|true" });
        EXPECTED.put("invoker", new String[] {
                "callShout=HI,origin=HI",
                "callShout=HI![outer],origin=HI",
                "callShout=HI,origin=HI" });
    }

    /**
     * Runs every scenario once and returns the observed strings. {@code faultMode} turns on the
     * hookers that throw on purpose, which only matters while the chain is installed.
     */
    private static Map<String, String> probe(boolean faultMode) throws Throwable {
        Map<String, String> observed = new LinkedHashMap<>();
        ModernChainModule module = ModernChainModule.getInstance();

        observed.put("api", module == null ? "n/a" : String.valueOf(module.getApiVersion()));

        ChainTarget target = new ChainTarget();
        ChainTarget.originCalls = 0;
        ModernChainModule.clearTrace();
        String shout = ChainTarget.shout("hi");
        observed.put("shout", shout + "|" + ModernChainModule.describe()
                + "|" + ChainTarget.originCalls);

        ChainTarget.originCalls = 0;
        observed.put("blocked", target.blocked() + "|" + ChainTarget.originCalls);

        ChainTarget.originCalls = 0;
        ChainTarget tagged = new ChainTarget("tag");
        observed.put("ctorProceed", show(tagged.label) + "|" + ChainTarget.originCalls);

        ChainTarget.originCalls = 0;
        ChainTarget marker = new ChainTarget(7);
        observed.put("ctorBlock", show(marker.label) + "|" + ChainTarget.originCalls);

        StringBuilder builder = new StringBuilder();
        target.mutate(builder);
        observed.put("mutate", builder.toString());

        observed.put("add", String.valueOf(target.add(1, 2)));

        ModernChainModule.clearTrace();
        observed.put("boom", describeThrow(ChainTarget::boom)
                + "|" + ModernChainModule.countTrace("boom:caught:"));

        ModernChainModule.throwInProtective = faultMode;
        ModernChainModule.throwInPassthrough = faultMode;
        observed.put("protective", describeThrow(() -> ChainTarget.protective("x")));
        observed.put("passthrough", describeThrow(() -> ChainTarget.passthrough("x")));
        ModernChainModule.throwInProtective = false;
        ModernChainModule.throwInPassthrough = false;

        if (module != null) {
            observed.put("invoker", "callShout=" + describeThrow(() -> module.callShout("hi"))
                    + ",origin=" + describeThrow(() -> module.callShoutOrigin("hi")));
        } else {
            observed.put("invoker", "callShout=" + ChainTarget.shout("hi") + ",origin=" + ChainTarget.shout("hi"));
        }

        observed.put("hookId", ModernChainModule.exerciseHookId());
        observed.put("replaceHook", ModernChainModule.exerciseReplaceHook());
        observed.put("legacyCompat", LegacyCompatModule.observed());
        return observed;
    }

    private static String show(String value) {
        return value == null ? "null" : value;
    }

    private interface Probe {
        Object get() throws Throwable;
    }

    private static String describeThrow(Probe probe) {
        try {
            return String.valueOf(probe.get());
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + ":" + t.getMessage();
        }
    }

    /**
     * A legacy {@code XC_MethodHook} rewrites the arguments of a method whose return value is
     * folded by a modern chain hook. Verifies the two dispatch layers nest instead of replacing
     * each other: the legacy callback sees the call first, the chain runs at the lowest legacy
     * priority slot.
     */
    private static TestResult testLegacyBeforeModernChain() {
        String testName = "Legacy XC_MethodHook + modern chain on the same method";
        try {
            Method add = ChainTarget.class.getDeclaredMethod("add", int.class, int.class);
            ChainTarget target = new ChainTarget();

            // The modern hook already doubles the result; the legacy hook adds one to the first
            // argument, so ((1 + 1) + 2) * 2 == 8.
            XC_MethodHook legacy = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.args[0] = (Integer) param.args[0] + 1;
                }
            };
            XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(add, legacy);
            int both = target.add(1, 2);
            unhook.unhook();
            int modernOnly = target.add(1, 2);

            return new TestResult(testName,
                    new TestResult.Stage("Modern chain alone", "6", String.valueOf(modernOnly)),
                    new TestResult.Stage("Legacy + modern", "8", String.valueOf(both)),
                    null);
        } catch (Throwable t) {
            return TestResult.failure(testName, t);
        }
    }

    /**
     * Feeds this app's own APK to AlterCore as a module. The application's class loader is used
     * directly so that module classes and target classes stay one and the same copy; a separate
     * {@code ModuleClassLoader} over this APK would define duplicates instead.
     */
    private static synchronized void ensureModuleLoaded(Context context) {
        if (sModuleLoaded) {
            return;
        }
        ApplicationInfo info = context.getApplicationInfo();
        AlterXposed.loadOpenedModule(info.sourceDir, context.getClassLoader(), false, info);
        sModuleLoaded = true;
    }

    private static void dispatchPackageLoad(Context context) {
        ApplicationInfo info = context.getApplicationInfo();
        String processName = currentProcessName(info);
        AlterXposed.onPackageLoad(info.packageName, processName, info, true, context.getClassLoader());
    }

    private static String currentProcessName(ApplicationInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        }
        return info.processName != null ? info.processName : MODULE_PACKAGE;
    }
}
