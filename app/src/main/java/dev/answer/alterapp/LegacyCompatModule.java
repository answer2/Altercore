package dev.answer.alterapp;

import android.content.res.XResources;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookCmdInit;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Legacy module used by {@link TestModernXposed} to verify AlterCore's compatibility surface:
 *
 * <ul>
 *   <li>it declares {@link IXposedHookCmdInit} and {@link IXposedHookInitPackageResources}, the two
 *       interfaces AlterCore cannot service, and must still load and run its Java hooks;</li>
 *   <li>it reads {@code param.callbacks}, whose declared type the API fixes as {@code XCallback[]};</li>
 *   <li>it overrides {@code callBeforeHookedMethod} instead of {@code beforeHookedMethod}, and
 *       writes the {@code result}/{@code returnEarly} fields directly, as modules built against the
 *       official API do.</li>
 * </ul>
 */
public class LegacyCompatModule implements IXposedHookLoadPackage, IXposedHookCmdInit,
        IXposedHookInitPackageResources {

    private static final String PACKAGE = "dev.answer.alterapp";

    private static boolean installed;
    private static int roundCalls;
    private static int loadPackageCallbacksSeen;
    private static boolean callBeforeDispatched;

    /** Resource-callback observations, read by {@link TestResourceHooks}. */
    private static volatile int resourceCallbacks;
    private static volatile int layoutsSeen;
    private static volatile String lastLayoutName = "none";
    private static volatile String lastLayoutView = "none";
    private static volatile XResources handle;
    private static volatile boolean layoutHookRegistered;

    /** Called before a dispatch round so {@link #observed()} describes that one round. */
    public static void beginRound() {
        roundCalls = 0;
    }

    public static String observed() {
        String echo;
        try {
            echo = String.valueOf(LegacyTarget.class.getDeclaredMethod("echo", String.class)
                    .invoke(null, "x"));
        } catch (ReflectiveOperationException e) {
            echo = "reflective-failure";
        }
        return roundCalls + "|" + loadPackageCallbacksSeen + "|" + echo + "|" + callBeforeDispatched;
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpp) throws Throwable {
        if (!"dev.answer.alterapp".equals(lpp.packageName)) {
            return;
        }
        roundCalls++;
        loadPackageCallbacksSeen = lpp.callbacks.length;
        if (installed) {
            return;
        }
        installed = true;

        Method echo = LegacyTarget.class.getDeclaredMethod("echo", String.class);
        XposedBridge.hookMethod(echo, new XC_MethodHook() {
            @Override
            public void callBeforeHookedMethod(MethodHookParam param) throws Throwable {
                callBeforeDispatched = true;
                param.result = "direct:" + param.args[0];
                param.returnEarly = true;
            }
        });
    }

    @Override
    public void initCmdApp(IXposedHookCmdInit.StartupParam startupParam) throws Throwable {
        // Never invoked: AlterCore does not hook command-line tools.
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if (!PACKAGE.equals(resparam.packageName)) {
            return;
        }
        resourceCallbacks++;
        handle = resparam.res;
        registerReplacements();
    }

    /**
     * Registers this module's replacements. The framework offers a package exactly once per
     * process, so the test suite calls this again after it has cleared the table.
     */
    public static void registerReplacements() {
        XResources resources = handle;
        if (resources == null) {
            return;
        }
        resources.setReplacement(PACKAGE, "string", "probe_string", "replaced-by-module");
        resources.setReplacement(PACKAGE, "layout", "probe_layout", R.layout.probe_layout_alt);
        if (layoutHookRegistered) {
            return;
        }
        layoutHookRegistered = true;
        resources.hookLayout(PACKAGE, "layout", "probe_layout", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                layoutsSeen++;
                lastLayoutName = liparam.resNames == null ? "no-names" : liparam.resNames.name;
                lastLayoutView = liparam.view == null ? "null" : "view";
            }
        });
    }

    public static String resourceState() {
        return "callbacks=" + resourceCallbacks + ",layouts=" + layoutsSeen
                + ",last=" + lastLayoutName + "/" + lastLayoutView;
    }
}
