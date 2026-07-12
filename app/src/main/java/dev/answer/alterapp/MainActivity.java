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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Material color roles, kept as explicit hex so the palette is easy to tweak in one place.
    private static final int COLOR_PASS = Color.parseColor("#2E7D32");
    private static final int COLOR_FAIL = Color.parseColor("#C62828");
    private static final int COLOR_PASS_CONTAINER = Color.parseColor("#E8F5E9");
    private static final int COLOR_FAIL_CONTAINER = Color.parseColor("#FFEBEE");
    private static final int COLOR_SURFACE = Color.WHITE;
    private static final int COLOR_SURFACE_VARIANT = Color.parseColor("#F5F5F5");
    private static final int COLOR_ON_SURFACE = Color.parseColor("#1B1B1B");
    private static final int COLOR_ON_SURFACE_VARIANT = Color.parseColor("#616161");
    private static final int COLOR_OUTLINE = Color.parseColor("#E0E0E0");
    private static final int COLOR_ARROW = Color.parseColor("#9E9E9E");
    private static final int COLOR_SECTION = Color.parseColor("#37474F");

    private LinearLayout resultsContainer;
    private Chip summaryChip;
    private LinearProgressIndicator summaryProgress;
    private MaterialButton runButton; // 提升为成员变量

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        View root = buildRootView();
        setContentView(root);

        // Defer the actual test run until after this Activity's first frame has been
        // submitted. AlterCore.hook() does heavy synchronous work per test (dex
        // generation, classloading, a global SuspendAll pass to patch ART entry
        // points), and running all of that inline in onCreate() blocks the main
        // thread while the first frame is already queued with SurfaceFlinger —
        // that's what produces the "Out of order buffers" warning at startup.
        root.post(this::runAndRenderTests);
    }

    private View buildRootView() {
        Context ctx = this;
        int pad = dp(16);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(48), pad, pad);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(COLOR_SURFACE_VARIANT);

        root.addView(buildHeaderCard());

        resultsContainer = new LinearLayout(ctx);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        resultsContainer.setPadding(0, dp(16), 0, 0);

        ScrollView scrollView = new ScrollView(ctx);
        scrollView.setClipToPadding(false);
        scrollView.addView(resultsContainer, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return root;
    }

    /**
     * Builds the sticky header: title, a chip showing the pass/fail summary, a progress
     * bar reflecting the pass ratio, and the re-run button — all inside a single elevated
     * MaterialCardView so it reads as one cohesive surface rather than loose widgets.
     */
    private View buildHeaderCard() {
        Context ctx = this;

        MaterialCardView headerCard = new MaterialCardView(ctx);
        headerCard.setRadius(dp(20));
        headerCard.setCardElevation(0);
        headerCard.setStrokeWidth(dp(1));
        headerCard.setStrokeColor(COLOR_OUTLINE);
        headerCard.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView title = new TextView(ctx);
        title.setText("AlterCore Hook Test");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_ON_SURFACE);
        content.addView(title);

        TextView subtitle = new TextView(ctx);
        subtitle.setText("Basic hooking, call-original, and real Xposed API coverage");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setTextColor(COLOR_ON_SURFACE_VARIANT);
        subtitle.setPadding(0, dp(2), 0, dp(14));
        content.addView(subtitle);

        LinearLayout summaryRow = new LinearLayout(ctx);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        summaryRow.setGravity(Gravity.CENTER_VERTICAL);

        summaryChip = new Chip(ctx);
        summaryChip.setClickable(false);
        summaryChip.setCheckable(false);
        summaryChip.setTextSize(13);
        summaryChip.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        chipParams.setMarginEnd( dp(10));
        summaryRow.addView(summaryChip, chipParams);

        runButton = new MaterialButton(ctx);
        runButton.setText("\u27F3  Run Again"); // ⟳ Run Again
        runButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        runButton.setCornerRadius(dp(12));
        runButton.setOnClickListener(v -> runAndRenderTests());
        summaryRow.addView(runButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(summaryRow);

        summaryProgress = new LinearProgressIndicator(ctx);
        summaryProgress.setMax(100);
        summaryProgress.setTrackThickness(dp(6));
        summaryProgress.setTrackCornerRadius(dp(3));
        summaryProgress.setTrackColor(COLOR_OUTLINE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progressParams.topMargin = dp(14);
        content.addView(summaryProgress, progressParams);

        headerCard.addView(content);
        return headerCard;
    }


    private void runAndRenderTests() {
        if (runButton != null) {
            runButton.setEnabled(false);
        }
        resultsContainer.removeAllViews();

        new Thread(() -> {
            List<TestResult> hookResults = TestHook.runTests();
            List<TestResult> invokeOriginalResults = TestInvokeOriginal.runTests();
            List<TestResult> xposedResults = TestXposed.runTests();

            runOnUiThread(() -> renderResults(hookResults, invokeOriginalResults, xposedResults));
        }, "AlterCoreTestRunner").start();
    }

    private void renderResults(List<TestResult> hookResults,
                               List<TestResult> invokeOriginalResults,
                               List<TestResult> xposedResults) {
        int total = hookResults.size() + invokeOriginalResults.size() + xposedResults.size();
        int passed = 0;

        passed += renderSection("Basic Hooking (AlterCore)", hookResults);
        passed += renderSection("Call-Original Capability (AlterCore)", invokeOriginalResults);
        passed += renderSection("Real Xposed API (XposedBridge)", xposedResults);

        boolean allPassed = passed == total;
        int summaryColor = allPassed ? COLOR_PASS : COLOR_FAIL;
        int summaryContainer = allPassed ? COLOR_PASS_CONTAINER : COLOR_FAIL_CONTAINER;

        summaryChip.setText(passed + " / " + total + " passed");
        summaryChip.setTextColor(summaryColor);
        summaryChip.setChipBackgroundColor(ColorStateList.valueOf(summaryContainer));

        summaryProgress.setIndicatorColor(summaryColor);
        summaryProgress.setProgress(total == 0 ? 0 : Math.round(100f * passed / total));

        if (runButton != null) {
            runButton.setEnabled(true);
        }
    }

    /**
     * Renders a titled section (label + divider) followed by one card per result.
     * Returns the number of passed results in this section, so the caller can
     * accumulate an overall total.
     */
    private int renderSection(String title, List<TestResult> results) {
        resultsContainer.addView(buildSectionHeader(title));

        int passed = 0;
        for (TestResult result : results) {
            if (result.passed) passed++;
            resultsContainer.addView(buildResultCard(result));
        }
        return passed;
    }

    private View buildSectionHeader(String title) {
        Context ctx = this;

        LinearLayout section = new LinearLayout(ctx);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sectionParams.topMargin = dp(8);
        sectionParams.bottomMargin = dp(10);
        section.setLayoutParams(sectionParams);

        TextView header = new TextView(ctx);
        header.setText(title);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(COLOR_SECTION);
        section.addView(header);

        MaterialDivider divider = new MaterialDivider(ctx);
        divider.setDividerColor(COLOR_OUTLINE);
        divider.setDividerThickness(dp(1));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dividerParams.topMargin = dp(6);
        section.addView(divider, dividerParams);

        return section;
    }

    /**
     * Builds one card per test case: a header with the test name and PASS/FAIL chip,
     * followed by up to three stage rows (Before hook -> After hook -> After unhook)
     * connected with arrows so the transition is visually obvious.
     */
    private View buildResultCard(TestResult result) {
        Context ctx = this;

        MaterialCardView card = new MaterialCardView(ctx);
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(result.passed ? COLOR_PASS : COLOR_FAIL);
        card.setCardBackgroundColor(result.passed ? COLOR_PASS_CONTAINER : COLOR_FAIL_CONTAINER);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(14));

        content.addView(buildCardHeader(result));

        if (result.error != null) {
            TextView errorView = new TextView(ctx);
            errorView.setText("Error: " + result.error);
            errorView.setTextColor(COLOR_FAIL);
            errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            errorView.setTypeface(Typeface.MONOSPACE);
            errorView.setPadding(0, dp(10), 0, 0);
            content.addView(errorView);
            card.addView(content);
            return card;
        }

        LinearLayout stageFlow = new LinearLayout(ctx);
        stageFlow.setOrientation(LinearLayout.VERTICAL);
        stageFlow.setPadding(0, dp(10), 0, 0);

        addStageWithArrow(stageFlow, result.baseline, true);
        addStageWithArrow(stageFlow, result.hooked, true);
        addStageWithArrow(stageFlow, result.restored, false);

        content.addView(stageFlow);
        card.addView(content);
        return card;
    }

    private View buildCardHeader(TestResult result) {
        Context ctx = this;

        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Chip badge = new Chip(ctx);
        badge.setClickable(false);
        badge.setCheckable(false);
        badge.setText(result.passed ? "PASS" : "FAIL");
        badge.setTextSize(11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(Color.WHITE);
        badge.setChipBackgroundColor(ColorStateList.valueOf(result.passed ? COLOR_PASS : COLOR_FAIL));
        badge.setChipMinHeight(dp(24));
        header.addView(badge);

        TextView nameView = new TextView(ctx);
        nameView.setText(result.name);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        nameView.setTextColor(COLOR_ON_SURFACE);
        nameView.setPadding(dp(8), 0, 0, 0);
        header.addView(nameView, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return header;
    }

    /**
     * Renders one stage (e.g. "Before hook") as a bordered surface showing expected vs.
     * actual value, optionally followed by a downward arrow leading into the next stage.
     */
    private void addStageWithArrow(LinearLayout parent, @Nullable TestResult.Stage stage, boolean withArrow) {
        if (stage == null) return;

        Context ctx = this;

        MaterialCardView stageCard = new MaterialCardView(ctx);
        stageCard.setRadius(dp(10));
        stageCard.setCardElevation(0);
        stageCard.setStrokeWidth(dp(1));
        stageCard.setStrokeColor(stage.passed ? COLOR_PASS : COLOR_FAIL);
        stageCard.setCardBackgroundColor(COLOR_SURFACE);

        LinearLayout stageBox = new LinearLayout(ctx);
        stageBox.setOrientation(LinearLayout.VERTICAL);
        stageBox.setPadding(dp(10), dp(8), dp(10), dp(8));

        LinearLayout labelRow = new LinearLayout(ctx);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelView = new TextView(ctx);
        labelView.setText(stage.label);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        labelView.setTextColor(COLOR_ON_SURFACE_VARIANT);
        labelRow.addView(labelView, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView checkView = new TextView(ctx);
        checkView.setText(stage.passed ? "\u2713" : "\u2717"); // ✓ / ✗
        checkView.setTextColor(stage.passed ? COLOR_PASS : COLOR_FAIL);
        checkView.setTypeface(Typeface.DEFAULT_BOLD);
        checkView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        labelRow.addView(checkView);

        stageBox.addView(labelRow);

        TextView valueView = new TextView(ctx);
        valueView.setText("\"" + stage.actual + "\"");
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        valueView.setTypeface(Typeface.MONOSPACE);
        valueView.setTextColor(COLOR_ON_SURFACE);
        valueView.setPadding(0, dp(4), 0, 0);
        stageBox.addView(valueView);

        if (!stage.passed) {
            TextView expectedView = new TextView(ctx);
            expectedView.setText("expected: \"" + stage.expected + "\"");
            expectedView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            expectedView.setTypeface(Typeface.MONOSPACE);
            expectedView.setTextColor(COLOR_FAIL);
            expectedView.setPadding(0, dp(2), 0, 0);
            stageBox.addView(expectedView);
        }

        stageCard.addView(stageBox);
        parent.addView(stageCard);

        if (withArrow) {
            TextView arrow = new TextView(ctx);
            arrow.setText("\u2193"); // ↓
            arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            arrow.setTextColor(COLOR_ARROW);
            arrow.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            arrowParams.topMargin = dp(2);
            arrowParams.bottomMargin = dp(2);
            arrow.setLayoutParams(arrowParams);
            parent.addView(arrow);
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}