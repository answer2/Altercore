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