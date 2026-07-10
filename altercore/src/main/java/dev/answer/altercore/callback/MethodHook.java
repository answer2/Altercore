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

import dev.answer.altercore.core.Hooks;

import java.lang.reflect.Member;

import dev.answer.altercore.core.HookParams;
import dev.answer.altercore.core.HookRecord;

public class MethodHook {
    public void before(HookParams params) throws Throwable {
    }

    public void after(HookParams params) throws Throwable {
    }

    public class Unhook {
        private final HookRecord hookRecord;

        public Unhook(HookRecord hookRecord) {
            this.hookRecord = hookRecord;
        }

        public Member getTarget() {
            return hookRecord.target;
        }

        public MethodHook getCallback() {
            return MethodHook.this;
        }

        public void unhook() {
            Hooks.hook(hookRecord.target, hookRecord.backup, Hooks.EntryPointType.CURRENT);
        }
    }
}
