package dev.answer.altercore.core;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import dev.answer.altercore.callback.MethodHook;

public final class HookRecord {
    public final Executable target;
    public final long artMethod;
    public Method bridge;
    public Method backup;
    public long trampoline;
    public boolean isStatic;
    public int paramNumber;
    public Class<?>[] paramTypes;
    private Set<MethodHook> callbacks = new HashSet<>();
    public volatile Object paramTypesCache;
    public boolean skipUpdateDeclaringClass;

    public HookRecord(Executable target, long artMethod) {
        this.target = target;
        this.artMethod = artMethod;
    }

    public synchronized void addCallback(MethodHook callback) {
        callbacks.add(callback);
    }

    public synchronized void removeCallback(MethodHook callback) {
        callbacks.remove(callback);
    }

    public synchronized boolean emptyCallbacks() {
        return callbacks.isEmpty();
    }

    public synchronized MethodHook[] getCallbacks() {
        return callbacks.toArray(new MethodHook[callbacks.size()]);
    }

    public boolean isPending() {
        return backup == null;
    }

    public Object callBackup(Object thisObject, Object... args) throws InvocationTargetException, IllegalAccessException {
        return backup.invoke(thisObject, args);
    }

}