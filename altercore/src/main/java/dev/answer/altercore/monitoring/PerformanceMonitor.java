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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global performance monitoring for AlterCore hooks.
 * 
 * <p>Collects invocation counts and timing statistics for each hooked method.
 * Monitoring is disabled by default to avoid performance overhead in production;
 * enable it via {@link #setEnabled(boolean)} for profiling and debugging.
 * 
 * <p>Thread-safe: all operations are safe for concurrent use.
 * 
 * @see HookStats
 */
public class PerformanceMonitor {
    
    private static volatile boolean enabled = false;
    private static final Map<Member, HookStats> statsMap = new ConcurrentHashMap<>();
    
    /**
     * Enables or disables performance monitoring globally.
     * 
     * <p>When disabled, hook invocations incur no additional overhead.
     * When enabled, each callback invocation records timing and error statistics.
     * 
     * @param enable {@code true} to enable monitoring, {@code false} to disable
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }
    
    /**
     * Returns whether performance monitoring is currently enabled.
     * 
     * @return {@code true} if monitoring is active
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Retrieves or creates the statistics object for the given target.
     * 
     * @param target the hooked method or constructor
     * @return the stats object for this target
     */
    public static HookStats getOrCreateStats(Member target) {
        return statsMap.computeIfAbsent(target, HookStats::new);
    }
    
    /**
     * Retrieves the statistics for the given target, or {@code null} if
     * the target has never been hooked or monitoring was not enabled when
     * it was hooked.
     * 
     * @param target the hooked method or constructor
     * @return the stats object, or {@code null} if unavailable
     */
    public static HookStats getStats(Member target) {
        return statsMap.get(target);
    }
    
    /**
     * Returns a snapshot of all collected statistics, sorted by total call count
     * (descending).
     * 
     * @return an immutable list of all {@link HookStats} objects, sorted by activity
     */
    public static List<HookStats> getAllStats() {
        List<HookStats> result = new ArrayList<>(statsMap.values());
        result.sort(Comparator.comparingLong(HookStats::getTotalCallCount).reversed());
        return Collections.unmodifiableList(result);
    }
    
    /**
     * Removes statistics for the given target (typically called when the hook
     * is uninstalled).
     * 
     * @param target the target whose stats should be removed
     */
    public static void removeStats(Member target) {
        statsMap.remove(target);
    }
    
    /**
     * Clears all collected statistics.
     */
    public static void reset() {
        statsMap.clear();
    }
    
    /**
     * Returns the number of methods currently being monitored.
     * 
     * @return the count of tracked methods
     */
    public static int getMonitoredCount() {
        return statsMap.size();
    }
    
    /**
     * Dumps a human-readable summary of all statistics to a string.
     * 
     * @return a multi-line summary of hook performance
     */
    public static String dumpStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AlterCore Performance Statistics ===\n");
        sb.append("Monitored methods: ").append(getMonitoredCount()).append("\n\n");
        
        List<HookStats> allStats = getAllStats();
        if (allStats.isEmpty()) {
            sb.append("No statistics collected.\n");
        } else {
            for (HookStats stats : allStats) {
                sb.append(stats.toString()).append("\n");
            }
        }
        
        return sb.toString();
    }
}
