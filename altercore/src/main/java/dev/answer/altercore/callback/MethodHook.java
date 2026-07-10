package dev.answer.altercore.callback;

import com.v7878.vmtools.Hooks;

import java.lang.reflect.Member;

import dev.answer.altercore.core.HookParams;
import dev.answer.altercore.core.HookRecord;

public class MethodHook {
    public void before(HookParams params) throws Throwable {
    }

    public void after(HookParams params) throws Throwable {
    }

    public class Unhook {
        private final HookRecord hookRecord;

        public Unhook(HookRecord hookRecord) {
            this.hookRecord = hookRecord;
        }

        public Member getTarget() {
            return hookRecord.target;
        }

        public MethodHook getCallback() {
            return MethodHook.this;
        }

        public void unhook() {
            Hooks.hook(hookRecord.target, hookRecord.backup, Hooks.EntryPointType.CURRENT);
        }
    }
}
