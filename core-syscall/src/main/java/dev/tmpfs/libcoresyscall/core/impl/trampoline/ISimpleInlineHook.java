package dev.tmpfs.libcoresyscall.core.impl.trampoline;

public interface ISimpleInlineHook {

    /**
     * Apply an inline hook to the specified function, it will jump to the hook address when the function is called.
     * This hook is extremely simple, it neither allows to call the original function nor supports multiple hooks.
     * If an address is hooked multiple times, the result is undefined.
     * If the address is not executable, the result is undefined.
     * If the address is not at the beginning of a function, the result is undefined.
     * If the hooked function has less than 24 bytes, the result is undefined.
     *
     * @param address the address to hook
     * @param hook    where to jump
     */
    void inlineHook(long address, long hook);

}
