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

import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.unsafe.Reflection.getArtMethod;
import static dev.answer.altercore.AlterConfig.isDebug;
import static dev.answer.altercore.utils._Utils.rawMethodTypeOf;

import android.util.Log;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.MethodId;
import com.v7878.dex.immutable.ProtoId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.Reflection;
import com.v7878.unsafe.invoke.EmulatedStackFrame;

import dev.answer.altercore.callback.AfterCallback;
import dev.answer.altercore.callback.BeforeCallback;
import dev.answer.altercore.callback.MethodReplacement;
import dev.answer.altercore.callback.ReplaceCallback;
import dev.answer.altercore.core.HookTransformer;
import dev.answer.altercore.core.Hooks;
import dev.answer.altercore.exception.HookException;
import dev.answer.altercore.exception.HookInstallationException;
import dev.answer.altercore.exception.InvalidTargetException;
import dev.answer.altercore.monitoring.HookStats;
import dev.answer.altercore.monitoring.PerformanceMonitor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dev.answer.altercore.callback.MethodHook;
import dev.answer.altercore.core.HookParams;
import dev.answer.altercore.core.HookRecord;

/**
 * AlterCore: Pure-Java ART hook library for Android 8.0-17.
 * 
 * <p>Provides Xposed-style hook API with support for before/after/replace callbacks.
 * Multiple callbacks can coexist on the same method, and all operations are thread-safe.
 * 
 * <p><b>Basic usage:</b>
 * <pre>{@code
 * // Before hook
 * CallbackToken token = AlterCore.before(targetMethod, params -> {
 *     Log.d("Hook", "Method called with args: " + Arrays.toString(params.args));
 * });
 * 
 * // After hook
 * AlterCore.after(targetMethod, params -> {
 *     Log.d("Hook", "Method returned: " + params.getResult());
 * });
 * 
 * // Replace hook
 * AlterCore.replace(targetMethod, params -> {
 *     return "custom result";
 * });
 * 
 * // Remove hook
 * token.remove();
 * }</pre>
 * 
 * <p><b>Performance monitoring:</b>
 * <pre>{@code
 * PerformanceMonitor.setEnabled(true);
 * // ... run your hooked code ...
 * HookStats stats = PerformanceMonitor.getStats(targetMethod);
 * Log.d("Perf", "Avg execution time: " + stats.getBeforeAvgNanos() + "ns");
 * }</pre>
 * 
 * <p><b>Query hook state:</b>
 * <pre>{@code
 * if (AlterCore.isHooked(targetMethod)) {
 *     HookInfo info = AlterCore.getHookInfo(targetMethod);
 *     Log.d("Hook", "Callbacks: " + info.getTotalCallbackCount());
 * }
 * }</pre>
 * 
 * @see CallbackToken
 * @see HookInfo
 * @see PerformanceMonitor
 */
public class AlterCore {
    private static final String TAG = "AlterCore";
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    
    // Core hook storage: artMethod pointer -> HookRecord
    // Memory leak fix: cleaned up in unHookLocked()
    private static final Map<Long, HookRecord> sHookRecords = new ConcurrentHashMap<>();
    private static final Object sHookLock = new Object();

    // Callback registry: Member -> HookData
    // Fine-grained locking: per-member locks in HookData instead of global sRegistryLock
    private static final Map<Member, HookData> methodMaps = new ConcurrentHashMap<>();
    private static final Map<Member, MethodHook.Unhook> unhookMaps = new ConcurrentHashMap<>();
    
    // Registry lock: protects methodMaps and unhookMaps structure changes
    // Individual HookData instances have their own locks for callback list mutations
    private static final ReadWriteLock sRegistryLock = new ReentrantReadWriteLock();
    private static final AtomicLong sTokenSeq = new AtomicLong();

    /** A replace callback that does nothing and returns null. */
    public static final ReplaceCallback DO_NOTHING = params -> null;

    /**
     * Adds an after hook to the specified method. Multiple after callbacks can coexist
     * on the same method (from unrelated call sites) and all run, in registration order,
     * on a single underlying hook.
     *
     * @param member   The method to hook.
     * @param callback The callback to execute after the method is called.
     * @return a token that can be used to remove exactly this registration via
     * {@link CallbackToken#remove()}, or {@code null} if registration failed.
     * @throws InvalidTargetException if member is null or invalid
     * @throws HookException if callback is null
     */
    public static CallbackToken after(Member member, AfterCallback callback) {
        if (member == null) {
            throw new InvalidTargetException(HookException.ErrorCode.NULL_TARGET, "member can't be null");
        }
        if (callback == null) {
            throw new HookException(HookException.ErrorCode.NULL_CALLBACK, "callback can't be null");
        }

        sRegistryLock.writeLock().lock();
        try {
            HookData info = methodMaps.computeIfAbsent(member, k -> new HookData(member));
            info.addAfter(callback);
            if (!ensureHooked(member, info)) {
                info.removeAfter(callback);
                return null;
            }
            return new CallbackToken(member, CallbackToken.Type.AFTER, callback);
        } finally {
            sRegistryLock.writeLock().unlock();
        }
    }

    /**
     * Adds a before hook to the specified method. Multiple before callbacks can coexist
     * on the same method (from unrelated call sites) and all run, in registration order,
     * on a single underlying hook.
     *
     * @param member   The method to hook.
     * @param callback The callback to execute before the method is called.
     * @return a token that can be used to remove exactly this registration via
     * {@link CallbackToken#remove()}, or {@code null} if registration failed.
     * @throws InvalidTargetException if member is null or invalid
     * @throws HookException if callback is null
     */
    public static CallbackToken before(Member member, BeforeCallback callback) {
        if (member == null) {
            throw new InvalidTargetException(HookException.ErrorCode.NULL_TARGET, "member can't be null");
        }
        if (callback == null) {
            throw new HookException(HookException.ErrorCode.NULL_CALLBACK, "callback can't be null");
        }

        sRegistryLock.writeLock().lock();
        try {
            HookData info = methodMaps.computeIfAbsent(member, k -> new HookData(member));
            info.addBefore(callback);
            if (!ensureHooked(member, info)) {
                info.removeBefore(callback);
                return null;
            }
            return new CallbackToken(member, CallbackToken.Type.BEFORE, callback);
        } finally {
            sRegistryLock.writeLock().unlock();
        }
    }

    /**
     * Replaces the specified method with the provided callback. Only one replace
     * callback is active per method at a time; registering a new one supersedes the
     * previous one (before/after callbacks on the same method are left untouched and
     * keep running around whichever replace callback is currently active, or around the
     * original implementation if none is).
     *
     * @param member   The method to replace.
     * @param callback The callback that replaces the method.
     * @return a token that can be used to remove exactly this registration via
     * {@link CallbackToken#remove()}, or {@code null} if registration failed.
     * @throws InvalidTargetException if member is null or invalid
     * @throws HookException if callback is null
     */
    public static CallbackToken replace(Member member, ReplaceCallback callback) {
        if (member == null) {
            throw new InvalidTargetException(HookException.ErrorCode.NULL_TARGET, "member can't be null");
        }
        if (callback == null) {
            throw new HookException(HookException.ErrorCode.NULL_CALLBACK, "callback can't be null");
        }

        sRegistryLock.writeLock().lock();
        try {
            HookData info = methodMaps.computeIfAbsent(member, k -> new HookData(member));
            ReplaceCallback previous = info.getReplace();
            info.setReplace(callback);
            if (!ensureHooked(member, info)) {
                info.setReplace(previous);
                return null;
            }
            return new CallbackToken(member, CallbackToken.Type.REPLACE, callback);
        } finally {
            sRegistryLock.writeLock().unlock();
        }
    }

    /**
     * Replaces the implementation of the provided `member` with a "do nothing" method.
     *
     * <p>This is a convenience wrapper around {@link #replace(Member, ReplaceCallback)}
     * using {@link #DO_NOTHING} as the replacement, so it shares the same "single active
     * replace callback" and coexistence-with-before/after semantics.
     *
     * @param member The method or constructor to be replaced with a "do nothing" implementation.
     * @return a token that can be used to remove exactly this registration, or
     * {@code null} if registration failed.
     */
    public static CallbackToken doNothing(Member member) {
        return replace(member, DO_NOTHING);
    }

    /**
     * Batch hook: adds the same after callback to multiple members.
     * 
     * @param members the methods/constructors to hook
     * @param callback the callback to execute after each method is called
     * @return a list of tokens for successful hooks (failed hooks are omitted)
     */
    public static List<CallbackToken> afterBatch(Collection<? extends Member> members, AfterCallback callback) {
        List<CallbackToken> tokens = new ArrayList<>(members.size());
        for (Member member : members) {
            try {
                CallbackToken token = after(member, callback);
                if (token != null) {
                    tokens.add(token);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to hook " + member + " in batch", e);
            }
        }
        return tokens;
    }

    /**
     * Batch hook: adds the same before callback to multiple members.
     * 
     * @param members the methods/constructors to hook
     * @param callback the callback to execute before each method is called
     * @return a list of tokens for successful hooks (failed hooks are omitted)
     */
    public static List<CallbackToken> beforeBatch(Collection<? extends Member> members, BeforeCallback callback) {
        List<CallbackToken> tokens = new ArrayList<>(members.size());
        for (Member member : members) {
            try {
                CallbackToken token = before(member, callback);
                if (token != null) {
                    tokens.add(token);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to hook " + member + " in batch", e);
            }
        }
        return tokens;
    }

    /**
     * Batch hook: replaces multiple members with the same callback.
     * 
     * @param members the methods/constructors to replace
     * @param callback the replacement callback
     * @return a list of tokens for successful hooks (failed hooks are omitted)
     */
    public static List<CallbackToken> replaceBatch(Collection<? extends Member> members, ReplaceCallback callback) {
        List<CallbackToken> tokens = new ArrayList<>(members.size());
        for (Member member : members) {
            try {
                CallbackToken token = replace(member, callback);
                if (token != null) {
                    tokens.add(token);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to hook " + member + " in batch", e);
            }
        }
        return tokens;
    }

    /**
     * Removes a previously registered after callback. If this was the last remaining
     * callback (before/after/replace) on the method, the underlying hook is released
     * automatically.
     *
     * @param member   The hooked method.
     * @param callback The exact callback instance passed to {@link #after(Member, AfterCallback)}.
     */
    public static void removeAfter(Member member, AfterCallback callback) {
        removeCallback(member, info -> info.removeAfter(callback));
    }

    /**
     * Removes a previously registered before callback. If this was the last remaining
     * callback (before/after/replace) on the method, the underlying hook is released
     * automatically.
     *
     * @param member   The hooked method.
     * @param callback The exact callback instance passed to {@link #before(Member, BeforeCallback)}.
     */
    public static void removeBefore(Member member, BeforeCallback callback) {
        removeCallback(member, info -> info.removeBefore(callback));
    }

    /**
     * Removes the active replace callback, but only if {@code callback} is still the
     * one currently installed (this guards against one caller accidentally clearing a
     * replacement callback that another caller has since installed). If this was the last
     * remaining callback (before/after/replace) on the method, the underlying hook is
     * released automatically.
     *
     * @param member   The hooked method.
     * @param callback The exact callback instance passed to {@link #replace(Member, ReplaceCallback)}.
     */
    public static void removeReplace(Member member, ReplaceCallback callback) {
        removeCallback(member, info -> {
            if (info.getReplace() == callback) {
                info.setReplace(null);
            }
        });
    }

    /**
     * Shared removal path for {@link #removeBefore}, {@link #removeAfter} and
     * {@link #removeReplace}: mutates this method's {@link HookData} under
     * {@link #sRegistryLock}, then tears the underlying hook down (and drops the
     * bookkeeping entry) once nothing is registered on it anymore.
     */
    private static void removeCallback(Member member, java.util.function.Consumer<HookData> mutator) {
        if (member == null) {
            Log.e(TAG, "member can't be null");
            return;
        }

        sRegistryLock.writeLock().lock();
        try {
            HookData info = methodMaps.get(member);
            if (info == null) {
                return;
            }
            mutator.accept(info);
            if (info.isEmpty()) {
                unHookLocked(member);
                methodMaps.remove(member);
            }
        } finally {
            sRegistryLock.writeLock().unlock();
        }
    }

    /**
     * Unhooks a method entirely: removes every registered before/after/replace callback
     * and releases the underlying hook. Prefer {@link #removeBefore}, {@link #removeAfter}
     * or {@link #removeReplace} when only one specific callback should go away.
     *
     * @param member The method or constructor to unhook.
     */
    public static void unHook(Member member) {
        if (member == null) {
            return;
        }
        sRegistryLock.writeLock().lock();
        try {
            unHookLocked(member);
            methodMaps.remove(member);
        } finally {
            sRegistryLock.writeLock().unlock();
        }
    }

    /** 
     * Must be called while holding {@link #sRegistryLock} write lock.
     * Fixed memory leak: now cleans up sHookRecords entry.
     */
    private static void unHookLocked(Member member) {
        MethodHook.Unhook unhook = unhookMaps.remove(member);
        if (unhook != null) {
            unhook.unhook();
            // Memory leak fix: remove from sHookRecords
            if (member instanceof Executable) {
                long artMethod = getArtMethod((Executable) member);
                sHookRecords.remove(artMethod);
            }
            // Clean up performance monitoring stats
            PerformanceMonitor.removeStats(member);
        }
    }

    /**
     * Checks whether a method is currently hooked.
     * 
     * @param member the method or constructor to check
     * @return {@code true} if the member has at least one active callback
     */
    public static boolean isHooked(Member member) {
        if (member == null) {
            return false;
        }
        sRegistryLock.readLock().lock();
        try {
            return methodMaps.containsKey(member);
        } finally {
            sRegistryLock.readLock().unlock();
        }
    }

    /**
     * Retrieves the current hook state for a method.
     * 
     * @param member the method or constructor to query
     * @return a {@link HookInfo} snapshot, or {@code null} if not hooked
     */
    public static HookInfo getHookInfo(Member member) {
        if (member == null) {
            return null;
        }
        sRegistryLock.readLock().lock();
        try {
            HookData info = methodMaps.get(member);
            if (info == null) {
                return null;
            }
            return info.getSnapshot();
        } finally {
            sRegistryLock.readLock().unlock();
        }
    }

    /**
     * Returns all currently hooked members.
     * 
     * @return an immutable set of hooked methods and constructors
     */
    public static Set<Member> getHookedMembers() {
        sRegistryLock.readLock().lock();
        try {
            return Collections.unmodifiableSet(methodMaps.keySet());
        } finally {
            sRegistryLock.readLock().unlock();
        }
    }

    /**
     * Returns the count of currently hooked members.
     * 
     * @return the number of methods/constructors with active hooks
     */
    public static int getHookedCount() {
        return methodMaps.size();
    }

    /**
     * Installs the single underlying {@link UnifiedCallback} hook for {@code member} if
     * one isn't already installed. Safe to call repeatedly; a second call while a hook
     * is already active is a no-op that returns {@code true}. Must be called while
     * holding {@link #sRegistryLock} write lock.
     *
     * @return {@code true} if a hook is installed (either just now or already), {@code
     * false} if installation was attempted and failed.
     */
    private static boolean ensureHooked(Member member, HookData info) {
        if (unhookMaps.containsKey(member)) {
            return true;
        }
        UnifiedCallback unified = new UnifiedCallback(member, info);
        MethodHook.Unhook unhook = hook((Executable) member, unified);
        if (unhook == null) {
            return false;
        }
        unhookMaps.put(member, unhook);
        return true;
    }

    /**
     * Installs a hook on the given executable (method or constructor).
     *
     * @param executable the target method or constructor to hook
     * @param callback   the callback that will be invoked before and after the original call
     * @return an {@link MethodHook.Unhook} object that can be used to remove the hook,
     * or {@code null} if an error occurred
     * @throws InvalidTargetException if the target is invalid (null, abstract, class initializer)
     * @throws HookInstallationException if native hook installation fails
     */
    public static MethodHook.Unhook hook(Executable executable, MethodHook callback) {
        try {
            if (isDebug) {
                Log.d(TAG, "Hooking method " + executable + " with callback " + callback);
            }

            if (executable == null) {
                throw new InvalidTargetException(HookException.ErrorCode.NULL_TARGET, "method == null");
            }
            if (callback == null) {
                throw new HookException(HookException.ErrorCode.NULL_CALLBACK, "callback == null");
            }

            int modifiers = executable.getModifiers();
            boolean isStatic = Modifier.isStatic(modifiers);
            boolean isConstructor = executable instanceof Constructor;

            // Return type: always void for constructors, the real return type for methods.
            Class<?> returnType = isConstructor ? void.class : ((Method) executable).getReturnType();

            if (executable instanceof Method) {
                if (Modifier.isAbstract(modifiers)) {
                    throw new InvalidTargetException(
                        HookException.ErrorCode.ABSTRACT_METHOD,
                        "Cannot hook abstract methods: " + executable
                    );
                }
                ((Method) executable).setAccessible(true);
            } else if (executable instanceof Constructor) {
                if (isStatic) {
                    throw new InvalidTargetException(
                        HookException.ErrorCode.CLASS_INITIALIZER,
                        "Cannot hook class initializer: " + executable
                    );
                }
                ((Constructor<?>) executable).setAccessible(true);
            } else {
                throw new InvalidTargetException(
                    HookException.ErrorCode.INVALID_TARGET_TYPE,
                    "Only methods and constructors can be hooked: " + executable
                );
            }

            Method backupMethod;
            try {
                backupMethod = buildBackup(executable);
            } catch (Exception e) {
                throw new HookInstallationException(
                    "Failed to build backup method for " + executable,
                    e
                );
            }
            
            try {
                Hooks.hook(backupMethod, executable, Hooks.EntryPointType.DIRECT);
            } catch (Exception e) {
                throw new HookInstallationException(
                    "Failed to hook backup method for " + executable,
                    e
                );
            }

            long artMethod = getArtMethod(executable);
            HookRecord hookRecord;
            boolean newMethod = false;

            // Set isStatic / backup while still holding the lock, so no thread can ever
            // observe a partially-initialized HookRecord through sHookRecords.
            synchronized (sHookLock) {
                hookRecord = sHookRecords.get(artMethod);
                if (hookRecord == null) {
                    newMethod = true;
                    hookRecord = new HookRecord(executable, artMethod);
                    hookRecord.isStatic = isStatic;
                    hookRecord.backup = backupMethod;
                    sHookRecords.put(artMethod, hookRecord);
                } else {
                    // Update the existing record inside the lock too, to avoid racing
                    // with concurrent readers of sHookRecords.
                    hookRecord.isStatic = isStatic;
                    hookRecord.backup = backupMethod;
                }
            }

            HookRecord finalHookRecord = hookRecord;
            HookTransformer hookTransformer = new HookTransformer() {
                @Override
                public void transform(MethodHandle original, EmulatedStackFrame frame) throws Throwable {
                    EmulatedStackFrame.StackFrameAccessor accessor = frame.accessor();

                    // Walk original.type()'s parameters and read each one with the accessor method
                    // matching its actual type, instead of slicing frame.references() (which silently
                    // drops primitive parameters — see readArgumentAt()).
                    Class<?>[] paramTypes = original.type().parameterArray();
                    int paramCount = paramTypes.length;
                    int argsStart = isStatic ? 0 : 1;

                    Object thisObject = isStatic ? null : readArgumentAt(accessor, paramTypes[0], 0);

                    Object[] argsData = paramCount == argsStart
                            ? EMPTY_OBJECT_ARRAY
                            : new Object[paramCount - argsStart];
                    for (int i = argsStart; i < paramCount; i++) {
                        argsData[i - argsStart] = readArgumentAt(accessor, paramTypes[i], i);
                    }

                    HookParams params = new HookParams(finalHookRecord, thisObject, argsData);

                    // Performance monitoring: track before callback
                    long beforeStart = 0;
                    if (PerformanceMonitor.isEnabled()) {
                        beforeStart = System.nanoTime();
                    }

                    try {
                        callback.before(params);
                        if (PerformanceMonitor.isEnabled()) {
                            HookStats stats = PerformanceMonitor.getOrCreateStats(executable);
                            stats.recordBeforeCall(System.nanoTime() - beforeStart);
                        }
                    } catch (Throwable e) {
                        if (PerformanceMonitor.isEnabled()) {
                            HookStats stats = PerformanceMonitor.getOrCreateStats(executable);
                            stats.recordBeforeCall(System.nanoTime() - beforeStart);
                            stats.recordBeforeError();
                        }
                        Log.e(TAG, "Unexpected exception occurred when calling "
                                + callback.getClass().getName() + ".beforeCall()", e);
                        params.resetResult();
                        params.setThrowable(e);
                    }

                    // Skipping the original body of a <init> target leaves `this`
                    // uninitialized, so it is only honored for callbacks that opt in.
                    if (isConstructor && params.isSkipped && !callback.allowsConstructorSkip()) {
                        Log.w(TAG, "Skipping a constructor is not allowed for "
                                + callback.getClass().getName() + ", ignoring: " + executable);
                        params.isSkipped = false;
                    }

                    Object[] methodArgs = isStatic ? params.args : addFirst(params.args, params.thisObject);

                    Object originalResult = null;
                    if (!params.isSkipped) {
                        try {
                            originalResult = original.invokeWithArguments(methodArgs);
                            params.setResult(originalResult);
                        } catch (Throwable e) {
                            params.setThrowable(e);
                        }
                    }

                    // Performance monitoring: track after callback
                    long afterStart = 0;
                    if (PerformanceMonitor.isEnabled()) {
                        afterStart = System.nanoTime();
                    }

                    try {
                        callback.after(params);
                        if (PerformanceMonitor.isEnabled()) {
                            HookStats stats = PerformanceMonitor.getOrCreateStats(executable);
                            stats.recordAfterCall(System.nanoTime() - afterStart);
                        }
                    } catch (Throwable e) {
                        if (PerformanceMonitor.isEnabled()) {
                            HookStats stats = PerformanceMonitor.getOrCreateStats(executable);
                            stats.recordAfterCall(System.nanoTime() - afterStart);
                            stats.recordAfterError();
                        }
                        Log.e(TAG, "Unexpected exception occurred when calling "
                                + callback.getClass().getName() + ".after()", e);
                        params.setThrowable(e);
                    }

                    // Propagate the failure first: HookTransformer#transform
                    // declares "throws Throwable" precisely so ART can turn it
                    // into a pending exception. Swallowing it here used to hand
                    // the caller null instead of, say, FileNotFoundException.
                    if (params.hasThrowable()) {
                        throw params.getThrowable();
                    }

                    // The backup call returns into a Java local, it does NOT
                    // write the caller's frame, so the return slot must always
                    // be filled for a non-void target. The previous
                    // "getResult() != originalResult" guard skipped exactly the
                    // common case (a before/after hook that left the result
                    // alone) and made every such call return null.
                    if (!isConstructor) {
                        writeReturnValue(accessor, returnType, params.getResult());
                    }
                }
            };

            try {
                Hooks.hook(executable, Hooks.EntryPointType.DIRECT,
                        hookTransformer, Hooks.EntryPointType.DIRECT);
            } catch (Exception e) {
                throw new HookInstallationException(
                    "Failed to install hook transformer for " + executable,
                    e
                );
            }

            return callback.new Unhook(hookRecord);
        } catch (HookException e) {
            // Re-throw our own exceptions
            throw e;
        } catch (Throwable e) {
            // Wrap unexpected errors
            Log.e(TAG, "Failed to hook " + executable, e);
            throw new HookInstallationException("Failed to hook " + executable, e);
        }
    }

    /**
     * Writes the hook result back into the stack frame according to the executable's
     * actual return type. Using {@code setReference} unconditionally is wrong for
     * primitive return types (int/long/boolean/float/double/...), as it can corrupt
     * the emulated stack frame.
     *
     * @param accessor   the stack frame accessor to write into
     * @param returnType the executable's declared return type
     * @param result     the value to write back
     */
    private static void writeReturnValue(EmulatedStackFrame.StackFrameAccessor accessor,
                                         Class<?> returnType, Object result) {
        if (returnType == void.class) {
            return;
        } else if (returnType == boolean.class) {
            accessor.setBoolean(EmulatedStackFrame.RETURN_VALUE_IDX, (Boolean) result);
        } else if (returnType == byte.class) {
            accessor.setByte(EmulatedStackFrame.RETURN_VALUE_IDX, (Byte) result);
        } else if (returnType == char.class) {
            accessor.setChar(EmulatedStackFrame.RETURN_VALUE_IDX, (Character) result);
        } else if (returnType == short.class) {
            accessor.setShort(EmulatedStackFrame.RETURN_VALUE_IDX, (Short) result);
        } else if (returnType == int.class) {
            accessor.setInt(EmulatedStackFrame.RETURN_VALUE_IDX, (Integer) result);
        } else if (returnType == long.class) {
            accessor.setLong(EmulatedStackFrame.RETURN_VALUE_IDX, (Long) result);
        } else if (returnType == float.class) {
            accessor.setFloat(EmulatedStackFrame.RETURN_VALUE_IDX, (Float) result);
        } else if (returnType == double.class) {
            accessor.setDouble(EmulatedStackFrame.RETURN_VALUE_IDX, (Double) result);
        } else {
            accessor.setReference(EmulatedStackFrame.RETURN_VALUE_IDX, result);
        }
    }

    /**
     * Reads a single argument out of the stack frame using the accessor method that
     * matches its declared type.
     * <p>
     * {@code frame.references()} only contains the frame's reference-type slots
     * (receiver, reference parameters, and a reference return slot if applicable) —
     * primitive parameters (int/long/boolean/...) are stored elsewhere and never show
     * up in it. Building args by slicing {@code references()} silently drops every
     * primitive argument, which is why reads must be dispatched by the parameter's
     * actual type instead.
     *
     * @param accessor  the stack frame accessor to read from
     * @param paramType the declared type of the parameter at {@code index}
     * @param index     the parameter's position within the frame's MethodType
     * @return the (boxed, if primitive) argument value
     */
    private static Object readArgumentAt(EmulatedStackFrame.StackFrameAccessor accessor,
                                         Class<?> paramType, int index) {
        if (!paramType.isPrimitive()) {
            return accessor.getReference(index);
        } else if (paramType == boolean.class) {
            return accessor.getBoolean(index);
        } else if (paramType == byte.class) {
            return accessor.getByte(index);
        } else if (paramType == char.class) {
            return accessor.getChar(index);
        } else if (paramType == short.class) {
            return accessor.getShort(index);
        } else if (paramType == int.class) {
            return accessor.getInt(index);
        } else if (paramType == long.class) {
            return accessor.getLong(index);
        } else if (paramType == float.class) {
            return accessor.getFloat(index);
        } else if (paramType == double.class) {
            return accessor.getDouble(index);
        } else {
            throw new IllegalArgumentException("Unsupported parameter type: " + paramType);
        }
    }

    /**
     * Inserts an element at the front of an object array.
     * <p>
     * Example: {@code addFirst(new Object[]{"B", 2}, "A")} returns {@code ["A", "B", 2]}.
     *
     * @param arr     the original array, may be {@code null}
     * @param element the element to prepend
     * @return a new array with the element inserted at index 0
     */
    private static Object[] addFirst(Object[] arr, Object element) {
        if (arr == null) {
            return new Object[]{element};
        }
        Object[] result = new Object[arr.length + 1];
        result[0] = element;
        System.arraycopy(arr, 0, result, 1, arr.length);
        return result;
    }

    /**
     * Builds a backup method that will be used as the replacement target when hooking.
     * The backup class is dynamically generated with a minimal method that returns null/0/void,
     * and is loaded via a new {@link ClassLoader}.
     * <p>
     * Supports both {@link Method} and {@link Constructor}. Since a constructor's
     * {@link Executable#getName()} returns the fully-qualified class name (which is not a
     * valid, non-constructor dex method name), a synthetic name is used instead. The generated
     * backup for a constructor is an ordinary instance method (not flagged {@code ACC_CONSTRUCTOR})
     * with a {@code void} return, matching the constructor's calling shape.
     *
     * @param executable the original method or constructor
     * @return the backup {@link Method} instance
     * @throws NoSuchMethodException if the backup method cannot be found in the generated class
     */
    private static Method buildBackup(Executable executable) throws NoSuchMethodException {
        boolean isConstructor = executable instanceof Constructor;
        boolean isStatic = Modifier.isStatic(executable.getModifiers());
        MethodType methodType = rawMethodTypeOf(executable); // constructor -> return type is void

        ProtoId protoId = ProtoId.of(methodType);

        String originName = executable.getDeclaringClass().getName();
        String backupName = originName + "$Backup";

        // Constructor#getName() returns the FQCN, which is not usable as a plain
        // (non-<init>) dex method name, so constructors get a distinct synthetic name.
        String backupMethodName = isConstructor ? "backupInit" : executable.getName();

        TypeId backupId = TypeId.ofName(backupName);
        TypeId backupOriginId = TypeId.ofName(originName);
        MethodId backupMethodId = MethodId.of(backupId, backupMethodName, protoId);

        ClassDef backupDef = ClassBuilder.build(backupId, cb -> cb
                .withSuperClass(TypeId.OBJECT)
                .withFlags(ACC_PUBLIC)
                .withMethod(mb -> mb
                        .of(backupMethodId)
                        .withFlags((ACC_PUBLIC | ACC_STATIC))
                        .withCode(2, ib -> {
                            // The body of a backup never runs: its entry point is
                            // overwritten with the original implementation by
                            // Hooks.hookBackup. It still has to pass the dex
                            // verifier, and the verifier rejects a return
                            // instruction whose register type does not match the
                            // declared return type. Emitting return-object for
                            // everything therefore failed for every primitive
                            // target with
                            //   VerifyError: ... return-object not expected
                            // so pick the instruction from the return type.
                            Class<?> returnType = methodType.returnType();
                            if (isConstructor || returnType == void.class) {
                                ib.return_void();
                            } else {
                                char shorty = shortyOf(returnType);
                                if (returnType == long.class || returnType == double.class) {
                                    ib.const_wide(ib.v(0), 0L)
                                            .return_shorty(shorty, ib.v(0));
                                } else {
                                    ib.const_(ib.v(0), 0)
                                            .return_shorty(shorty, ib.v(0));
                                }
                            }
                        })
                )
        );

        // The backup is declared in the target's own package and its descriptor mentions
        // the target's parameter and return types, so it can only be resolved by a loader
        // that can see the target class. AlterCore's own loader is right for classes the
        // app loaded itself, but a target defined by another loader -- a plugin loader, or
        // the DexClassLoader of a virtualised package -- makes hooking fail with
        //   NoClassDefFoundError: Failed resolution of: L<target>;
        // and the caller only sees a null hook token. Use the declaring class's loader
        // whenever there is one (null means the boot loader, where AlterCore's loader is
        // the correct parent).
        ClassLoader backupParent = executable.getDeclaringClass().getClassLoader();
        if (backupParent == null) {
            backupParent = AlterCore.class.getClassLoader();
        }

        ClassLoader loader = ClassUtils.newLoader(
                backupParent,
                DexIO.write(Dex.of(backupDef))
        );

        Class<?> backupClass = ClassUtils.forName(backupName, loader);
        return backupClass.getDeclaredMethod(backupMethodName, methodType.parameterArray());
    }

    /**
     * DEX shorty character for a return type, as needed by
     * {@link com.v7878.dex.builder.CodeBuilder#return_shorty(char, int)}.
     */
    private static char shortyOf(Class<?> type) {
        if (type == void.class)    return 'V';
        if (type == boolean.class) return 'Z';
        if (type == byte.class)    return 'B';
        if (type == short.class)   return 'S';
        if (type == char.class)    return 'C';
        if (type == int.class)     return 'I';
        if (type == long.class)    return 'J';
        if (type == float.class)   return 'F';
        if (type == double.class)  return 'D';
        return 'L';
    }

    /**
     * Logs a formatted message when debug mode is enabled.
     *
     * @param fmt  the format string (as in {@link String#format})
     * @param args the arguments referenced by the format specifiers
     */
    private static void print(String fmt, Object... args) {
        if (isDebug) {
            Log.i(TAG, String.format(fmt, args));
        }
    }

    public static void deoptimize(Executable ex) {
        Hooks.deoptimize(ex);
    }

    /**
     * Invokes the original implementation of the given method or constructor, bypassing
     * any active hook so the un-hooked behavior can still be observed from within a
     * callback (e.g. to call through to the real implementation).
     * <p>
     * If the target is not currently hooked, this falls back to a direct
     * {@link Method#invoke} / {@link Constructor#newInstance} call. That fallback has a
     * known race: if another thread installs a hook on the same target between the
     * lookup above and the actual invocation, the call will go through the newly
     * installed hook instead of the original implementation. Do not rely on this
     * fallback behavior being un-hooked.
     *
     * @param method     the method or constructor whose original implementation should run
     * @param thisObject the receiver, or {@code null} for static methods / constructors
     * @param args       the arguments for the call
     * @return the result of the original call ({@code null} for constructors)
     * @throws NullPointerException      if {@code method} is {@code null}
     * @throws IllegalAccessException    should never happen, since accessibility is forced
     * @throws InvocationTargetException if the underlying call throws
     * @throws IllegalArgumentException  if the arguments don't match the target's signature,
     *                                   or a non-null receiver is given for an un-hooked constructor
     */
    public static Object invokeOriginalMethod(Member method, Object thisObject, Object... args)
            throws IllegalAccessException, InvocationTargetException {

        if (method == null) throw new NullPointerException("method == null");
        if (method instanceof Method) {
            ((Method) method).setAccessible(true);
        } else if (method instanceof Constructor) {
            ((Constructor<?>) method).setAccessible(true);
        } else {
            throw new IllegalArgumentException("method must be of type Method or Constructor");
        }

        HookRecord hookRecord = sHookRecords.get(Reflection.getArtMethod((Executable) method));
        if (hookRecord == null) {
            if (isDebug) {
                Log.w(TAG, "Attempting to invoke the original implementation of a method that is "
                                + "not hooked: " + method + ". This falls back to a direct call; if another "
                                + "thread hooks this method concurrently, the call may go through the "
                                + "newly installed hook instead of the real implementation.",
                        new Throwable("here"));
            }
            if (method instanceof Constructor) {
                if (thisObject != null) {
                    throw new IllegalArgumentException(
                            "Cannot invoke a not-hooked Constructor with a non-null receiver");
                }
                try {
                    return ((Constructor<?>) method).newInstance(args);
                } catch (InstantiationException e) {
                    throw new IllegalArgumentException("Invalid constructor: " + method, e);
                }
            } else {
                return ((Method) method).invoke(thisObject, args);
            }
        }

        // Unlike Pine, AlterCore builds the backup synchronously inside hook() and
        // publishes the HookRecord to sHookRecords only after `backup` is assigned
        // (see AlterCore.hook()). This branch should therefore be unreachable; it is
        // kept only as an explicit guard so a broken invariant fails loudly instead
        // of silently misbehaving.
        if (hookRecord.backup == null) {
            throw new IllegalStateException(
                    "hookRecord.backup is null for " + method + ". A HookRecord must never be "
                            + "published before its backup method is assigned.");
        }

        return callBackupMethod(hookRecord, thisObject, args);
    }

    /**
     * Invokes the generated backup method that carries the original implementation of
     * {@code hookRecord.target}.
     * <p>
     * The backup method is always a static trampoline (see {@link #buildBackup}): for
     * non-static targets the receiver is passed as the first element of the argument
     * array rather than as the reflective receiver, since the backup's declaring class
     * (a synthetic "$Backup" class) is unrelated to the original's declaring class and
     * would fail ART's receiver-type check otherwise.
     *
     * @param hookRecord the hook record describing the target and its backup
     * @param thisObject the receiver for non-static targets, ignored for static targets
     * @param args       the arguments for the call
     * @return the result of the original call
     */
    public static Object callBackupMethod(HookRecord hookRecord, Object thisObject, Object[] args)
            throws InvocationTargetException, IllegalAccessException {
        // java.lang.Class objects are movable and may cause crashes when invoking the backup
        // method; native JNI entry points can also be changed by RegisterNatives/
        // UnregisterNatives, so keep a reference alive across the call.
        Member origin = hookRecord.target;
        Method backup = hookRecord.backup;
        Class<?> declaring = origin.getDeclaringClass();

        // FIXME: a args error
        Object[] backupArgs = hookRecord.isStatic ? args : addFirst(args, thisObject);

        // FIXME: a GC happening exactly here (try Runtime.getRuntime().gc() to reproduce)
        // can crash the backup call; see the FIXME on the original Pine implementation.
        Object result = backup.invoke(null, backupArgs);

        // Explicit use of declaring_class to keep a reference on the stack and avoid it
        // being moved/collected by a concurrent GC while `backup` is executing.
        declaring.getClass();
        return result;
    }

    /**
     * Stores every callback currently registered for one {@link Member}: zero or more
     * before/after callbacks (each may run alongside unrelated registrations from other
     * call sites) and at most one active replace callback.
     * <p>
     * A single {@link HookData} instance is reused for the entire lifetime of a
     * member's underlying hook — it is created once (in {@code computeIfAbsent}) and
     * only ever mutated in place, never replaced, so the {@link UnifiedCallback} built
     * on top of it always observes the current state without needing to re-look it up.
     * 
     * <p>Optimized with CopyOnWriteArrayList for read-heavy workload (hook invocations
     * are far more frequent than registration/removal).
     */
    private static class HookData {
        private final Member target;
        private final long hookTimestamp;
        private final List<BeforeCallback> beforeList = new CopyOnWriteArrayList<>();
        private final List<AfterCallback> afterList = new CopyOnWriteArrayList<>();
        private volatile ReplaceCallback replace;

        HookData(Member target) {
            this.target = target;
            this.hookTimestamp = System.currentTimeMillis();
        }

        void addBefore(BeforeCallback cb) { beforeList.add(cb); }
        void removeBefore(BeforeCallback cb) { beforeList.remove(cb); }
        void addAfter(AfterCallback cb) { afterList.add(cb); }
        void removeAfter(AfterCallback cb) { afterList.remove(cb); }
        void setReplace(ReplaceCallback cb) { replace = cb; }
        ReplaceCallback getReplace() { return replace; }

        boolean isEmpty() {
            return beforeList.isEmpty() && afterList.isEmpty() && replace == null;
        }

        HookInfo getSnapshot() {
            return new HookInfo(
                target,
                beforeList.size(),
                afterList.size(),
                replace != null,
                hookTimestamp
            );
        }
    }

    /**
     * A handle returned by {@link #after}, {@link #before}, {@link #replace} and
     * {@link #doNothing} that identifies exactly one registration, so it can be removed
     * later without needing to keep the original {@link Member} and callback reference
     * around separately (though {@link #removeBefore}/{@link #removeAfter}/
     * {@link #removeReplace} remain available for callers who'd rather use the callback
     * object itself as the identifier).
     * <p>
     * {@link #remove()} is idempotent: calling it more than once, or after the
     * underlying hook has already been torn down some other way, is a harmless no-op.
     */
    public static final class CallbackToken {
        private enum Type { BEFORE, AFTER, REPLACE }

        private final long id = sTokenSeq.incrementAndGet();
        private final Member member;
        private final Type type;
        private final Object callback;
        private volatile boolean removed = false;

        private CallbackToken(Member member, Type type, Object callback) {
            this.member = member;
            this.type = type;
            this.callback = callback;
        }

        /** A stable, unique id for this registration (monotonically increasing). */
        public long id() {
            return id;
        }

        /**
         * Removes this specific callback. If it was the last callback registered on the
         * method, the underlying hook is released automatically. Safe to call more than
         * once.
         */
        public synchronized void remove() {
            if (removed) {
                return;
            }
            removed = true;
            switch (type) {
                case BEFORE:
                    removeBefore(member, (BeforeCallback) callback);
                    break;
                case AFTER:
                    removeAfter(member, (AfterCallback) callback);
                    break;
                case REPLACE:
                    removeReplace(member, (ReplaceCallback) callback);
                    break;
            }
        }
    }

    /**
     * The single {@link MethodHook} implementation ever installed per member. Fans a
     * method's low-level before/after/replace calls out to every currently-registered
     * {@link BeforeCallback}/{@link AfterCallback}, and to the currently-active
     * {@link ReplaceCallback} (or through to the original implementation if none is
     * set), by reading straight from the member's shared {@link HookData} — so
     * callbacks can be added or removed at any time without reinstalling the hook.
     * 
     * <p>Performance monitoring is integrated at this level to measure callback overhead.
     */
    private static class UnifiedCallback extends MethodReplacement {
        private final Member target;
        private final HookData info;

        UnifiedCallback(Member target, HookData info) {
            this.target = target;
            this.info = info;
        }

        @Override
        public void before(HookParams params) {
            for (BeforeCallback cb : info.beforeList) {
                long start = 0;
                if (PerformanceMonitor.isEnabled()) {
                    start = System.nanoTime();
                }
                try {
                    cb.beforeHook(params);
                    if (PerformanceMonitor.isEnabled()) {
                        HookStats stats = PerformanceMonitor.getOrCreateStats(target);
                        stats.recordBeforeCall(System.nanoTime() - start);
                    }
                } catch (Throwable t) {
                    if (PerformanceMonitor.isEnabled()) {
                        HookStats stats = PerformanceMonitor.getOrCreateStats(target);
                        stats.recordBeforeCall(System.nanoTime() - start);
                        stats.recordBeforeError();
                    }
                    Log.e(TAG, "Unexpected exception occurred when calling "
                            + cb.getClass().getName() + ".beforeHook()", t);
                }
            }
        }

        @Override
        public void after(HookParams params) {
            for (AfterCallback cb : info.afterList) {
                long start = 0;
                if (PerformanceMonitor.isEnabled()) {
                    start = System.nanoTime();
                }
                try {
                    cb.afterHook(params);
                    if (PerformanceMonitor.isEnabled()) {
                        HookStats stats = PerformanceMonitor.getOrCreateStats(target);
                        stats.recordAfterCall(System.nanoTime() - start);
                    }
                } catch (Throwable t) {
                    if (PerformanceMonitor.isEnabled()) {
                        HookStats stats = PerformanceMonitor.getOrCreateStats(target);
                        stats.recordAfterCall(System.nanoTime() - start);
                        stats.recordAfterError();
                    }
                    Log.e(TAG, "Unexpected exception occurred when calling "
                            + cb.getClass().getName() + ".afterHook()", t);
                }
            }
        }

        @Override
        protected Object replace(HookParams params) throws Throwable {
            ReplaceCallback cb = info.getReplace();
            if (cb != null) {
                long start = 0;
                if (PerformanceMonitor.isEnabled()) {
                    start = System.nanoTime();
                }
                try {
                    Object result = cb.replaceHook(params);
                    if (PerformanceMonitor.isEnabled()) {
                        HookStats stats = PerformanceMonitor.getOrCreateStats(target);
                        stats.recordReplaceCall(System.nanoTime() - start);
                    }
                    return result;
                } catch (Throwable t) {
                    if (PerformanceMonitor.isEnabled()) {
                        HookStats stats = PerformanceMonitor.getOrCreateStats(target);
                        stats.recordReplaceCall(System.nanoTime() - start);
                        stats.recordReplaceError();
                    }
                    Log.e(TAG, "Unexpected exception occurred when calling "
                            + cb.getClass().getName() + ".replaceHook()", t);
                    return params.invokeOriginalMethod();
                }
            }
            return params.invokeOriginalMethod();
        }
    }
}