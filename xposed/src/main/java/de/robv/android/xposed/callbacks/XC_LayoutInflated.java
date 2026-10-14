/*
 * Shape and documentation follow LSPosed (GPL-3.0), so modules built against that API find the
 * same members here. Adapted by AlterCore: the callback registry is keyed by
 * package/type/entry instead of a resource directory, and the ordering comes from XCallback.
 */
package de.robv.android.xposed.callbacks;

import android.content.res.XResources;
import android.view.View;

import de.robv.android.xposed.XposedBridge.CopyOnWriteSortedSet;

/**
 * Callback for hooking layouts. Such callbacks can be passed to
 * {@link XResources#hookLayout(String, String, XC_LayoutInflated)} and its variants.
 */
public abstract class XC_LayoutInflated extends XCallback {
    /**
     * Creates a new callback with default priority.
     */
    @SuppressWarnings("deprecation")
    public XC_LayoutInflated() {
        super();
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * @param priority See {@link XCallback#priority}.
     */
    public XC_LayoutInflated(int priority) {
        super(priority);
    }

    /**
     * Wraps information about the inflated layout.
     */
    public static final class LayoutInflatedParam extends XCallback.Param {
        /**
         * @hide
         */
        public LayoutInflatedParam(CopyOnWriteSortedSet<XC_LayoutInflated> callbacks) {
            super(callbacks);
        }

        /**
         * The view that has been created from the layout.
         */
        public View view;

        /**
         * Container with the ID and name of the underlying resource.
         */
        public XResources.ResourceNames resNames;

        /**
         * Directory from which the layout was actually loaded (e.g. "layout-sw600dp").
         *
         * <p>AlterCore resolves layouts by their {@code package/type/entry} key and does not know
         * which configuration variant the platform picked, so this field stays {@code null}.</p>
         */
        public String variant;

        /**
         * Resources containing the layout.
         */
        public XResources res;
    }

    /** @hide */
    @Override
    protected void call(Param param) throws Throwable {
        if (param instanceof LayoutInflatedParam) {
            handleLayoutInflated((LayoutInflatedParam) param);
        }
    }

    /**
     * This method is called when the hooked layout has been inflated.
     *
     * @param liparam Information about the layout and the inflated view.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    public abstract void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable;

    /**
     * An object with which the callback can be removed.
     */
    public class Unhook implements IXUnhook<XC_LayoutInflated> {
        private final String packageName;
        private final String type;
        private final String entryName;

        /** @hide */
        public Unhook(String packageName, String type, String entryName) {
            this.packageName = packageName;
            this.type = type;
            this.entryName = entryName;
        }

        @Override
        public XC_LayoutInflated getCallback() {
            return XC_LayoutInflated.this;
        }

        @Override
        public void unhook() {
            de.robv.android.xposed.XposedBridge.unhookLayout(packageName, type, entryName,
                    getCallback());
        }
    }
}
