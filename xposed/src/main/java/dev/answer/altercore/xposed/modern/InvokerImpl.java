package dev.answer.altercore.xposed.modern;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dev.answer.altercore.AlterCore;

import io.github.libxposed.api.XposedInterface;

/**
 * {@link XposedInterface.Invoker} for methods. The call honours the selected {@link Type}, and
 * access checks are bypassed because the implementation works on the target's backup method.
 */
final class InvokerImpl implements XposedInterface.Invoker<InvokerImpl, Method> {
    private final Method target;
    private Type type = XposedInterface.Invoker.Type.Chain.FULL;

    InvokerImpl(Method target) {
        this.target = target;
    }

    @Override
    public InvokerImpl setType(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("type == null");
        }
        this.type = type;
        return this;
    }

    @Override
    public Object invoke(Object thisObject, Object... args) throws InvocationTargetException,
            IllegalArgumentException, IllegalAccessException {
        try {
            return HookRegistry.callByInvokerType(target, type, thisObject, args);
        } catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
            throw e;
        } catch (Throwable t) {
            // A hooker or the target itself threw something the reflective contract has no
            // slot for; InvocationTargetException is the one that can carry it.
            throw new InvocationTargetException(t);
        }
    }

    @Override
    public Object invokeSpecial(Object thisObject, Object... args) throws InvocationTargetException,
            IllegalArgumentException, IllegalAccessException {
        // The backup method carries the implementation of exactly this declaring class's method,
        // so invoking it is a non-virtual call even when the receiver's class overrides it.
        return AlterCore.invokeOriginalMethod(target, thisObject, args);
    }
}
