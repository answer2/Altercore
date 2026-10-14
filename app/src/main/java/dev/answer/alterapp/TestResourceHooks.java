package dev.answer.alterapp;

import android.content.Context;
import android.content.res.XResources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifies AlterCore's resource hooking: replacements registered by a legacy module in
 * {@code handleInitPackageResources} take effect for the whole app, layouts can be substituted by
 * id, inflation callbacks are delivered, and the dispatch happens exactly once per package.
 *
 * <p>The module used is {@link LegacyCompatModule}, which replaces
 * {@code string/probe_string} with a literal, swaps {@code layout/probe_layout} for
 * {@code layout/probe_layout_alt} and hooks the inflation of the former.</p>
 */
public class TestResourceHooks {

    private static final String PACKAGE = "dev.answer.alterapp";
    private static final String TAG = "TestResourceHooks";

    public static List<TestResult> runTests(Context context) {
        List<TestResult> results = new ArrayList<>();
        try {
            int probeString = context.getResources().getIdentifier("probe_string", "string", PACKAGE);

            // First touch of any resource is what offers the package to the module, so the
            // replacement is already in place by the time this returns.
            String replaced = context.getString(probeString);
            String replacedAgain = context.getString(probeString);
            Log.i(TAG, "observed probe_string id=" + probeString + " first=" + replaced
                    + " repeat=" + replacedAgain + " " + LegacyCompatModule.resourceState());
            results.add(new TestResult("String replacement applies app-wide",
                    null,
                    new TestResult.Stage("Replaced on first lookup", "replaced-by-module", replaced),
                    new TestResult.Stage("Same value on repeat", "replaced-by-module", replacedAgain)));

            LayoutInflater inflater = LayoutInflater.from(context);
            String inflated = firstText(inflater.inflate(R.layout.probe_layout, null, false));
            results.add(one("Layout replaced by resource id", "from-alt-layout", inflated));

            String layoutState = LegacyCompatModule.resourceState();
            results.add(new TestResult("handleInitPackageResources / XC_LayoutInflated delivered",
                    null,
                    new TestResult.Stage("Package offered once", "callbacks=1",
                            offeredCount(layoutState)),
                    new TestResult.Stage("Inflation reported", "probe_layout/view",
                            lastLayout(layoutState))));

            XResources.clearReplacements(PACKAGE);
            String restored = context.getString(probeString);
            String inflatedAfterClear = firstText(inflater.inflate(R.layout.probe_layout, null, false));
            Log.i(TAG, "observed after clear: string=" + restored + " layout=" + inflatedAfterClear
                    + " " + LegacyCompatModule.resourceState());
            results.add(new TestResult("Clearing replacements restores the originals",
                    null,
                    new TestResult.Stage("String after clear", "platform", restored),
                    new TestResult.Stage("Layout after clear", "from-default-layout", inflatedAfterClear)));

            // Clearing must not reopen the package: the callback runs once, ever. Inflation
            // callbacks keep firing for every inflation, so only the offer count is checked here.
            results.add(one("Package is offered to modules exactly once",
                    "callbacks=1", offeredCount(LegacyCompatModule.resourceState())));

            // Put the replacements back: the framework will not offer this package a second time,
            // so a re-run of the suite needs the module to register them itself.
            LegacyCompatModule.registerReplacements();
        } catch (Throwable t) {
            results.add(TestResult.failure("Resource hook tests", t));
        }
        return results;
    }

    private static TestResult one(String name, String expected, String actual) {
        return new TestResult(name, null, new TestResult.Stage("Observed", expected, actual), null);
    }

    /** Pulls {@code callbacks=N} out of the state string so repeat inflations don't skew it. */
    private static String offeredCount(String state) {
        int start = state.indexOf("callbacks=");
        if (start < 0) {
            return "unreadable";
        }
        int end = state.indexOf(',', start);
        return state.substring(start, end < 0 ? state.length() : end);
    }

    /** The {@code name/view} pair of the most recent inflation callback. */
    private static String lastLayout(String state) {
        int start = state.indexOf("last=");
        return start < 0 ? "unreadable" : state.substring(start + "last=".length());
    }

    private static String firstText(View root) {
        if (root instanceof TextView) {
            return String.valueOf(((TextView) root).getText());
        }
        if (root instanceof android.view.ViewGroup && ((android.view.ViewGroup) root).getChildCount() > 0) {
            return firstText(((android.view.ViewGroup) root).getChildAt(0));
        }
        return "no-text-view";
    }
}
