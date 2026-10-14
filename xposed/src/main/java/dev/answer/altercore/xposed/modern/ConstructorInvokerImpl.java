package dev.answer.altercore.xposed.modern;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.github.libxposed.api.XposedInterface;

/**
 * {@link XposedInterface.CtorInvoker} implementation.
 *
 * <p>{@code ORIGIN} cannot be honoured for constructors: creating an instance without running any
 * hook requires allocating the object first, which AlterCore exposes no entry point for, so a
 * plain {@link Constructor#newInstance} is used and the installed chain runs as usual.</p>
 */
final class ConstructorInvokerImpl<T> implements XposedInterface.CtorInvoker<T> {
    private final Constructor<T> target;
    private Type type = XposedInterface.Invoker.Type.Chain.FULL;

    ConstructorInvokerImpl(Constructor<T> target) {
        this.target = target;
    }

    @Override
    public XposedInterface.CtorInvoker<T> setType(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("type == null");
        }
        this.type = type;
        return this;
    }

    @Override
    public T newInstance(Object... args) throws InvocationTargetException, IllegalArgumentException,
            IllegalAccessException, InstantiationException {
        target.setAccessible(true);
        return target.newInstance(args);
    }

    @Override
    public <U> U newInstanceSpecial(Class<U> subClass, Object... args)
            throws InvocationTargetException, IllegalArgumentException, IllegalAccessException,
            InstantiationException {
        throw new UnsupportedOperationException(
                "AlterCore cannot allocate " + subClass.getName() + " and initialize it with " + target);
    }

    @Override
    public Object invoke(Object thisObject, Object... args) throws InvocationTargetException,
            IllegalArgumentException, IllegalAccessException {
        // Running an <init> body against an existing instance is the backup method's job, which
        // only exists once the constructor is hooked.
        try {
            return HookRegistry.callByInvokerType(target, type, thisObject, args);
        } catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
            throw e;
        } catch (Throwable t) {
            throw new InvocationTargetException(t);
        }
    }

    @Override
    public Object invokeSpecial(Object thisObject, Object... args) throws InvocationTargetException,
            IllegalArgumentException, IllegalAccessException {
        return invoke(thisObject, args);
    }
}
