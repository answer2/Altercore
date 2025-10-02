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

import java.lang.reflect.Field;

import dev.answer.altercore.AlterConfig;
import sun.misc.Unsafe;

public class UnsafeWrapper {

    private static Unsafe unsafe;

    public static Unsafe getUnsafe() {
        try {
            if (unsafe == null) {
                unsafe = (Unsafe) Unsafe.class.getMethod("getUnsafe").invoke(null);

                if (unsafe == null) {
                    Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                    try {
                        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
                        theUnsafe.setAccessible(true);
                        unsafe =  (Unsafe) theUnsafe.get(null);
                    } catch (Exception e) {
                        try {
                            final Field theUnsafe = unsafeClass.getDeclaredField("THE_ONE");
                            theUnsafe.setAccessible(true);
                            unsafe = (Unsafe) theUnsafe.get(null);
                        } catch (Exception e2) {
                            AlterConfig.supportUnsafe = false;
                            AlterLog.w("Unsafe not found, Sorry, your device will not be able to use this framework");
                            throw new AlterException("Unsafe couldn't ues, please retry");
                        }
                    }
                }
            }
        } catch (Exception e) {
            AlterLog.e("UnsafeWrapper", e);
        }

        return unsafe;
    }

}
