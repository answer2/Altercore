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
package dev.answer.altercore.monitoring;

import java.lang.reflect.Member;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance statistics for a single hooked method.
 * 
 * <p>Tracks invocation count and total execution time for before/after/replace
 * callbacks. This data is only collected when {@link PerformanceMonitor} is enabled.
 * 
 * <p>Thread-safe: all fields use atomic operations.
 */
public class HookStats {
    private final Member target;
    private final long hookTimestamp;
    
    // Invocation counters
    private final AtomicLong beforeCallCount = new AtomicLong(0);
    private final AtomicLong afterCallCount = new AtomicLong(0);
    private final AtomicLong replaceCallCount = new AtomicLong(0);
    private final AtomicLong totalCallCount = new AtomicLong(0);
    
    // Time tracking (nanoseconds)
    private final AtomicLong beforeTotalNanos = new AtomicLong(0);
    private final AtomicLong afterTotalNanos = new AtomicLong(0);
    private final AtomicLong replaceTotalNanos = new AtomicLong(0);
    
    // Error tracking
    private final AtomicLong beforeErrorCount = new AtomicLong(0);
    private final AtomicLong afterErrorCount = new AtomicLong(0);
    private final AtomicLong replaceErrorCount = new AtomicLong(0);
    
    public HookStats(Member target) {
        this.target = target;
        this.hookTimestamp = System.currentTimeMillis();
    }
    
    public Member getTarget() {
        return target;
    }
    
    public long getHookTimestamp() {
        return hookTimestamp;
    }
    
    // Call counting
    public void recordBeforeCall(long nanos) {
        beforeCallCount.incrementAndGet();
        totalCallCount.incrementAndGet();
        beforeTotalNanos.addAndGet(nanos);
    }
    
    public void recordAfterCall(long nanos) {
        afterCallCount.incrementAndGet();
        afterTotalNanos.addAndGet(nanos);
    }
    
    public void recordReplaceCall(long nanos) {
        replaceCallCount.incrementAndGet();
        totalCallCount.incrementAndGet();
        replaceTotalNanos.addAndGet(nanos);
    }
    
    // Error counting
    public void recordBeforeError() {
        beforeErrorCount.incrementAndGet();
    }
    
    public void recordAfterError() {
        afterErrorCount.incrementAndGet();
    }
    
    public void recordReplaceError() {
        replaceErrorCount.incrementAndGet();
    }
    
    // Getters
    public long getBeforeCallCount() {
        return beforeCallCount.get();
    }
    
    public long getAfterCallCount() {
        return afterCallCount.get();
    }
    
    public long getReplaceCallCount() {
        return replaceCallCount.get();
    }
    
    public long getTotalCallCount() {
        return totalCallCount.get();
    }
    
    public long getBeforeTotalNanos() {
        return beforeTotalNanos.get();
    }
    
    public long getAfterTotalNanos() {
        return afterTotalNanos.get();
    }
    
    public long getReplaceTotalNanos() {
        return replaceTotalNanos.get();
    }
    
    public long getBeforeErrorCount() {
        return beforeErrorCount.get();
    }
    
    public long getAfterErrorCount() {
        return afterErrorCount.get();
    }
    
    public long getReplaceErrorCount() {
        return replaceErrorCount.get();
    }
    
    // Computed metrics
    public double getBeforeAvgNanos() {
        long count = beforeCallCount.get();
        return count == 0 ? 0.0 : (double) beforeTotalNanos.get() / count;
    }
    
    public double getAfterAvgNanos() {
        long count = afterCallCount.get();
        return count == 0 ? 0.0 : (double) afterTotalNanos.get() / count;
    }
    
    public double getReplaceAvgNanos() {
        long count = replaceCallCount.get();
        return count == 0 ? 0.0 : (double) replaceTotalNanos.get() / count;
    }
    
    @Override
    public String toString() {
        return String.format(
            "HookStats{target=%s, totalCalls=%d, before=%d(avg=%.2fµs,err=%d), " +
            "after=%d(avg=%.2fµs,err=%d), replace=%d(avg=%.2fµs,err=%d)}",
            target,
            getTotalCallCount(),
            getBeforeCallCount(), getBeforeAvgNanos() / 1000.0, getBeforeErrorCount(),
            getAfterCallCount(), getAfterAvgNanos() / 1000.0, getAfterErrorCount(),
            getReplaceCallCount(), getReplaceAvgNanos() / 1000.0, getReplaceErrorCount()
        );
    }
}
