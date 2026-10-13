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
package dev.answer.altercore;

import java.lang.reflect.Member;

/**
 * Immutable snapshot of a method's current hook state.
 * 
 * <p>Provides read-only access to the number and types of callbacks currently
 * registered on a hooked method, without exposing the internal callback objects
 * themselves.
 * 
 * @see AlterCore#getHookInfo(Member)
 */
public final class HookInfo {
    
    private final Member target;
    private final int beforeCount;
    private final int afterCount;
    private final boolean hasReplace;
    private final long hookTimestamp;
    
    public HookInfo(Member target, int beforeCount, int afterCount, boolean hasReplace, long hookTimestamp) {
        this.target = target;
        this.beforeCount = beforeCount;
        this.afterCount = afterCount;
        this.hasReplace = hasReplace;
        this.hookTimestamp = hookTimestamp;
    }
    
    /**
     * The hooked method or constructor.
     * 
     * @return the target member
     */
    public Member getTarget() {
        return target;
    }
    
    /**
     * The number of before-callbacks currently registered.
     * 
     * @return the count of before callbacks
     */
    public int getBeforeCount() {
        return beforeCount;
    }
    
    /**
     * The number of after-callbacks currently registered.
     * 
     * @return the count of after callbacks
     */
    public int getAfterCount() {
        return afterCount;
    }
    
    /**
     * Whether a replace-callback is currently active.
     * 
     * @return {@code true} if a replace callback is installed
     */
    public boolean hasReplace() {
        return hasReplace;
    }
    
    /**
     * The total number of callbacks (before + after + replace).
     * 
     * @return the total callback count
     */
    public int getTotalCallbackCount() {
        return beforeCount + afterCount + (hasReplace ? 1 : 0);
    }
    
    /**
     * Timestamp (milliseconds since epoch) when the hook was first installed.
     * 
     * @return the hook installation time
     */
    public long getHookTimestamp() {
        return hookTimestamp;
    }
    
    @Override
    public String toString() {
        return String.format(
            "HookInfo{target=%s, before=%d, after=%d, replace=%s, age=%dms}",
            target,
            beforeCount,
            afterCount,
            hasReplace,
            System.currentTimeMillis() - hookTimestamp
        );
    }
}
