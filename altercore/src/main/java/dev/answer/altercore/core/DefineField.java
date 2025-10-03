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

package dev.answer.altercore.core;

public class DefineField {
    public static final long TRUE = 1;
    public static final long FALSE = 0;

    public static final NativeObject TRUE_OBJECT = new NativeObject(1);
    public static final NativeObject FALSE_OBJECT = new NativeObject(0);
    public static final int JNI_OK = 0;
    public static final int JNI_ERR = -1;
    public static final int JNI_VERSION_1_2 = 0x00010002;
    public static final int JNI_VERSION_1_4 = 0x00010004;
    public static final int JNI_VERSION_1_6 = 0x00010006;
    private static final int SHELLCODE_SIZE = 0x200;
}
