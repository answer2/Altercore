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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int COLOR_PASS = Color.parseColor("#2E7D32");
    private static final int COLOR_FAIL = Color.parseColor("#C62828");
    private static final int COLOR_PASS_BG = Color.parseColor("#E8F5E9");
    private static final int COLOR_FAIL_BG = Color.parseColor("#FFEBEE");
    private static final int COLOR_STAGE_BG = Color.parseColor("#F5F5F5");
    private static final int COLOR_ARROW = Color.parseColor("#9E9E9E");

    private LinearLayout resultsContainer;
    private TextView summaryView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildRootView());
        runAndRenderTests();
    }

    private View buildRootView() {
        Context ctx = this;
        int pad = dp(16);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(32), pad, pad);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(ctx);
        title.setText("AlterCore Hook Test");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        summaryView = new TextView(ctx);
        summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        summaryView.setPadding(0, dp(4), 0, dp(12));
        root.addView(summaryView);

        Button runButton = new Button(ctx);
        runButton.setText("Run Tests Again");
        runButton.setOnClickListener(v -> runAndRenderTests());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.bottomMargin = dp(12);
        root.addView(runButton, buttonParams);

        resultsContainer = new LinearLayout(ctx);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(ctx);
        scrollView.addView(resultsContainer, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return root;
    }

    private void runAndRenderTests() {
        resultsContainer.removeAllViews();

        List<TestResult> results = TestHook.runTests();

        int passed = 0;
        for (TestResult result : results) {
            if (result.passed) passed++;
            resultsContainer.addView(buildResultCard(result));
        }

        boolean allPassed = passed == results.size();
        summaryView.setText(passed + " / " + results.size() + " test cases passed");
        summaryView.setTextColor(allPassed ? COLOR_PASS : COLOR_FAIL);
        summaryView.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /**
     * Builds one card per test case: a header with the test name and PASS/FAIL badge,
     * followed by up to three stage rows (Before hook -> After hook -> After unhook)
     * connected with arrows so the transition is visually obvious.
     */
    private View buildResultCard(TestResult result) {
        Context ctx = this;

        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(8));
        cardBg.setColor(result.passed ? COLOR_PASS_BG : COLOR_FAIL_BG);
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        card.addView(buildCardHeader(result));

        if (result.error != null) {
            TextView errorView = new TextView(ctx);
            errorView.setText("Error: " + result.error);
            errorView.setTextColor(COLOR_FAIL);
            errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            errorView.setPadding(0, dp(8), 0, 0);
            card.addView(errorView);
            return card;
        }

        LinearLayout stageFlow = new LinearLayout(ctx);
        stageFlow.setOrientation(LinearLayout.VERTICAL);
        stageFlow.setPadding(0, dp(10), 0, 0);

        addStageWithArrow(stageFlow, result.baseline, true);
        addStageWithArrow(stageFlow, result.hooked, true);
        addStageWithArrow(stageFlow, result.restored, false);

        card.addView(stageFlow);
        return card;
    }

    private View buildCardHeader(TestResult result) {
        Context ctx = this;

        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(ctx);
        badge.setText(result.passed ? "PASS" : "FAIL");
        badge.setTextColor(Color.WHITE);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        badge.setPadding(dp(8), dp(2), dp(8), dp(2));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(4));
        badgeBg.setColor(result.passed ? COLOR_PASS : COLOR_FAIL);
        badge.setBackground(badgeBg);
        header.addView(badge);

        TextView nameView = new TextView(ctx);
        nameView.setText(result.name);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        nameView.setTextColor(Color.BLACK);
        nameView.setPadding(dp(8), 0, 0, 0);
        header.addView(nameView);

        return header;
    }

    /**
     * Renders one stage (e.g. "Before hook") as a labeled box showing expected vs.
     * actual value, optionally followed by a downward arrow leading into the next stage.
     */
    private void addStageWithArrow(LinearLayout parent, @Nullable TestResult.Stage stage, boolean withArrow) {
        if (stage == null) return;

        Context ctx = this;

        LinearLayout stageBox = new LinearLayout(ctx);
        stageBox.setOrientation(LinearLayout.VERTICAL);
        stageBox.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable stageBg = new GradientDrawable();
        stageBg.setCornerRadius(dp(6));
        stageBg.setColor(COLOR_STAGE_BG);
        stageBg.setStroke(dp(1), stage.passed ? COLOR_PASS : COLOR_FAIL);
        stageBox.setBackground(stageBg);

        LinearLayout labelRow = new LinearLayout(ctx);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelView = new TextView(ctx);
        labelView.setText(stage.label);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        labelView.setTextColor(Color.parseColor("#616161"));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelRow.addView(labelView, labelParams);

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
        valueView.setTextColor(Color.BLACK);
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

        parent.addView(stageBox);

        if (withArrow) {
            TextView arrow = new TextView(ctx);
            arrow.setText("\u2193"); // ↓
            arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            arrow.setTextColor(COLOR_ARROW);
            arrow.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            arrow.setLayoutParams(arrowParams);
            parent.addView(arrow);
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}