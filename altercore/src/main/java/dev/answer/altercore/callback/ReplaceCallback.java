package dev.answer.altercore.callback;

import dev.answer.altercore.core.HookParams;

public interface ReplaceCallback {
    Object replaceHook(HookParams params) throws Throwable;
}