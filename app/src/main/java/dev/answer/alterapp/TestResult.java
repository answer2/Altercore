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

/**
 * Result of a single test case, broken into three stages so the effect of hooking
 * (and unhooking) is visible step by step:
 * <ol>
 *   <li>{@code baseline} — behavior before any hook is installed</li>
 *   <li>{@code hooked} — behavior while the hook is active</li>
 *   <li>{@code restored} — behavior after the hook is removed</li>
 * </ol>
 * Each stage is optional (null if not applicable to a given test).
 */
public class TestResult {

    /** A single expected-vs-actual comparison for one stage of a test. */
    public static class Stage {
        public final String label;
        public final String expected;
        public final String actual;
        public final boolean passed;

        public Stage(String label, String expected, String actual) {
            this.label = label;
            this.expected = expected;
            this.actual = actual;
            this.passed = expected == null ? actual == null : expected.equals(actual);
        }
    }

    public final String name;
    public final Stage baseline;
    public final Stage hooked;
    public final Stage restored;
    public final boolean passed;
    public final String error; // non-null if the test threw an exception

    public TestResult(String name, Stage baseline, Stage hooked, Stage restored) {
        this.name = name;
        this.baseline = baseline;
        this.hooked = hooked;
        this.restored = restored;
        this.passed = (baseline == null || baseline.passed)
                && (hooked == null || hooked.passed)
                && (restored == null || restored.passed);
        this.error = null;
    }

    private TestResult(String name, String error) {
        this.name = name;
        this.baseline = null;
        this.hooked = null;
        this.restored = null;
        this.passed = false;
        this.error = error;
    }

    public static TestResult failure(String name, Throwable cause) {
        return new TestResult(name, cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }
}