package dev.answer.altercore.callback;

import dev.answer.altercore.core.HookParams;

public interface AfterCallback {
    void afterHook(HookParams params) throws Throwable;
}