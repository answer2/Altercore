/*
 * Shape and documentation follow LSPosed (GPL-3.0), so modules built against that API find the
 * same members here. AlterCore adds no resource-hooking of its own to this class; see
 * dev.answer.altercore.xposed.ResourcesHooks for where replacements are applied.
 */
package de.robv.android.xposed.callbacks;

import de.robv.android.xposed.IXposedHookInitPackageResources;

/**
 * This class is only used for internal purposes, except for the {@link InitPackageResourcesParam}
 * subclass.
 *
 * <p>AlterCore does not implement resource replacement, so the callback is registered and never
 * invoked, and the parameter carries no {@code res} reference: upstream types that field as
 * {@code android.content.res.XResources}, a class the framework owns and that cannot be shipped
 * with a hook library. Declaring the interface is still enough for a module's entry class to be
 * defined, which is what makes the rest of that module work.</p>
 */
public abstract class XC_InitPackageResources extends XCallback
        implements IXposedHookInitPackageResources {
    /**
     * Creates a new callback with default priority.
     *
     * @hide
     */
    @SuppressWarnings("deprecation")
    public XC_InitPackageResources() {
        super();
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * @param priority See {@link XCallback#priority}.
     * @hide
     */
    public XC_InitPackageResources(int priority) {
        super(priority);
    }

    /**
     * Wraps information about the resources being initialized.
     */
    public static final class InitPackageResourcesParam extends XCallback.Param {
        /**
         * @hide
         */
        public InitPackageResourcesParam(
                de.robv.android.xposed.XposedBridge.CopyOnWriteSortedSet<XC_InitPackageResources> callbacks) {
            super(callbacks);
        }

        /**
         * The name of the package for which resources are being loaded.
         */
        public String packageName;

        /**
         * Reference to the resources on which resource replacements can be registered.
         *
         * <p>AlterCore's handle is not a {@code Resources} subclass (see
         * {@link android.content.res.XResources}); calls to {@code setReplacement} and
         * {@code hookLayout} work as they do upstream.</p>
         */
        public android.content.res.XResources res;
    }

    /**
     * @hide
     */
    @Override
    protected void call(Param param) throws Throwable {
        if (param instanceof InitPackageResourcesParam) {
            handleInitPackageResources((InitPackageResourcesParam) param);
        }
    }
}
