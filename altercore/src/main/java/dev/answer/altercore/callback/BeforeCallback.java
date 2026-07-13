package dev.answer.altercore.callback;

import dev.answer.altercore.core.HookParams;

public interface BeforeCallback {
    void beforeHook(HookParams params) throws Throwable;
}