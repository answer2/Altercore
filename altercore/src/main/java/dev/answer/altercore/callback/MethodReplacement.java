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
package dev.answer.altercore.callback;

import dev.answer.altercore.core.HookParams;

public abstract class MethodReplacement extends MethodHook {
    public static final MethodReplacement DO_NOTHING = new MethodReplacement() {
        @Override protected Object replace(HookParams params) {
            return null;
        }
    };

    protected abstract Object replace(HookParams params) throws Throwable;

    @Override public final void before(HookParams params) {
        try {
            params.setResult(replace(params));
        } catch (Throwable e) {
            params.setThrowable(e);
        }
    }

    @Override public final void after(HookParams params) {
    }

    public static MethodReplacement returnConstant(final Object result) {
        return new MethodReplacement() {
            @Override protected Object replace(HookParams params) {
                return result;
            }
        };
    }
}