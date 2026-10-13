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
package dev.answer.altercore.exception;

/**
 * Thrown when hook installation fails at the native level.
 * 
 * <p>This typically indicates a low-level ART or DEX manipulation failure
 * that cannot be recovered from within the current operation. The client
 * should log the error and avoid retrying the same hook unless the underlying
 * condition (e.g., method signature, runtime state) has changed.
 */
public class HookInstallationException extends HookException {
    
    public HookInstallationException(String message) {
        super(ErrorCode.NATIVE_HOOK_FAILED, message);
    }
    
    public HookInstallationException(String message, Throwable cause) {
        super(ErrorCode.NATIVE_HOOK_FAILED, message, cause);
    }
}
