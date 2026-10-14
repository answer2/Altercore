/*
 * Shape and documentation follow LSPosed (GPL-3.0). AlterCore implements resource replacement for
 * the public Resources getters only, and its XResources is not a Resources subclass.
 */
package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

/**
 * Get notified when the resources for an app are initialized.
 * In {@link #handleInitPackageResources}, resource replacements can be created.
 *
 * <p>This interface should be implemented by the module's main class. Xposed will take care of
 * registering it as a callback automatically.
 *
 * <p>AlterCore implements only Java method hooking: the callback is registered so that modules
 * declaring this interface still load, but {@link #handleInitPackageResources} is never called and
 * no resource replacement is possible.
 */
public interface IXposedHookInitPackageResources extends IXposedMod {
    /**
     * This method is called when resources for an app are being initialized.
     * Modules can call special methods of the {@code XResources} class in order to replace resources.
     *
     * @param resparam Information about the resources.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable;

    /** @hide */
    final class Wrapper extends XC_InitPackageResources {
        private final IXposedHookInitPackageResources instance;

        public Wrapper(IXposedHookInitPackageResources instance) {
            this.instance = instance;
        }

        @Override
        public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
            instance.handleInitPackageResources(resparam);
        }
    }
}
