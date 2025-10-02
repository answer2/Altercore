/*
 * Copyright (C) 2025 AnswerDev. All rights reserved.
 *
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
package dev.answer.altercore.utils;

import android.util.Log;
import static dev.answer.altercore.AlterConfig.debug;

public class AlterLog {
        public static final String TAG = "AlterCore-Framework";

        public static int v(String s) {
            return debug ? Log.v(TAG, s) : 0;
        }

        public static int i(String s) {
            return debug ? Log.i(TAG, s) : 0;
        }

        public static int d(String s) {
            return debug ? Log.d(TAG, s) : 0;
        }

        public static int w(String s) {
            return Log.w(TAG, s);
        }

        public static int e(String s) {
            return Log.e(TAG, s);
        }

        public static int e(String s, Throwable t) {
            return Log.e(TAG, s, t);
        }

}
