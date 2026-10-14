/*
 * Shape and documentation follow LSPosed (GPL-3.0). AlterCore never invokes initCmdApp; the
 * interface is present so that modules declaring it can still be defined and loaded.
 */
package de.robv.android.xposed;

/**
 * Hook the initialization of Java-based command-line tools (like pm).
 *
 * @hide Xposed no longer hooks command-line tools, therefore this interface shouldn't be
 * implemented anymore. It is kept here so that existing modules declaring it can still be loaded:
 * the interface must be resolvable when their entry class is defined. AlterCore never calls
 * {@link #initCmdApp}.
 */
public interface IXposedHookCmdInit extends IXposedMod {
    /**
     * Called very early during startup of a command-line tool.
     *
     * @param startupParam Details about the module itself and the started process.
     * @throws Throwable Everything is caught, but it will prevent further initialization of the module.
     */
    void initCmdApp(StartupParam startupParam) throws Throwable;

    /**
     * Data holder for {@link #initCmdApp}.
     */
    final class StartupParam {
        /*package*/ StartupParam() {
        }

        /**
         * The path to the module's APK.
         */
        public String modulePath;

        /**
         * The class name of the tools that the hook was invoked for.
         */
        public String startClassName;
    }
}
