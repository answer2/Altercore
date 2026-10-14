package dev.answer.altercore.xposed;

import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import de.robv.android.xposed.IXposedHookCmdInit;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedMod;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedBridge.CopyOnWriteSortedSet;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import dev.answer.altercore.xposed.modern.ModernModuleLoader;

public final class AlterXposed {
    public static final String TAG = "AlterXposed";
    public static boolean disableHooks = false;
    public static boolean disableZygoteInitCallbacks = false;
    private static ExtHandler sExtHandler;

    public static ExtHandler getExtHandler() {
        return sExtHandler;
    }

    public static void setExtHandler(ExtHandler n) {
        sExtHandler = n;
    }

    private static final CopyOnWriteSortedSet<XC_LoadPackage> sLoadedPackageCallbacks = new CopyOnWriteSortedSet<>();

    private AlterXposed() {
    }

    public static void loadModule(String module) {
        loadModule(new File(module));
    }

    public static void loadModule(File module) {
        loadModule(module, false);
    }

    public static void loadModule(File module, boolean startsSystemServer) {
        loadModule(module, null, startsSystemServer);
    }

    public static void loadModule(File module, String librarySearchPath, boolean startsSystemServer) {
        loadModule(module, librarySearchPath, startsSystemServer, null);
    }

    /**
     * @param moduleInfo the {@link ApplicationInfo} of the module, or {@code null} to synthesize a
     *                   descriptor that only carries the APK paths
     */
    public static void loadModule(File module, String librarySearchPath, boolean startsSystemServer,
                                  ApplicationInfo moduleInfo) {
        if (!module.exists()) {
            Log.e(TAG, "  File " + module + " does not exist");
            return;
        }
        ClassLoader initCl = AlterXposed.class.getClassLoader();
        String modulePath = module.getAbsolutePath();
        ModuleClassLoader mcl = new ModuleClassLoader(modulePath, librarySearchPath, initCl);
        loadOpenedModule(modulePath, mcl, startsSystemServer, moduleInfo, librarySearchPath);
    }

    public static void loadOpenedModule(String modulePath, ClassLoader mcl, boolean startsSystemServer) {
        loadOpenedModule(modulePath, mcl, startsSystemServer, null, null);
    }

    public static void loadOpenedModule(String modulePath, ClassLoader mcl, boolean startsSystemServer,
                                        ApplicationInfo moduleInfo) {
        loadOpenedModule(modulePath, mcl, startsSystemServer, moduleInfo, null);
    }

    /**
     * @param librarySearchPath the native library directory the module was loaded with, kept so a
     *                          later hot reload can rebuild its class loader
     */
    public static void loadOpenedModule(String modulePath, ClassLoader mcl, boolean startsSystemServer,
                                        ApplicationInfo moduleInfo, String librarySearchPath) {
        if (!checkModule(mcl)) return;
        // The modern descriptor is read first: a module that only declares META-INF/xposed/*
        // must not be reported as a broken legacy module afterwards.
        boolean modernEntry = ModernModuleLoader.load(mcl, modulePath, librarySearchPath,
                moduleInfo, startsSystemServer);
        loadLegacyModule(modulePath, mcl, startsSystemServer, modernEntry);
    }

    private static void loadLegacyModule(String modulePath, ClassLoader mcl,
                                         boolean startsSystemServer, boolean modernEntry) {
        InputStream initIs;
        try {
            final String filename = "assets/xposed_init";
            if (mcl instanceof ModuleClassLoader) {
                // Fast and provided more error info
                URL url = ((ModuleClassLoader) mcl).findResource(filename);
                initIs = url != null ? url.openStream() : null;
            } else {
                initIs = mcl.getResourceAsStream(filename);
            }
            if (initIs == null) {
                if (modernEntry) {
                    Log.i(TAG, "  " + modulePath + " declares no assets/xposed_init, "
                            + "using its modern entry only");
                } else {
                    Log.e(TAG, "  Failed to load module " + modulePath);
                    Log.e(TAG, "  assets/xposed_init not found in the module APK");
                }
                return;
            }
        } catch (IOException e) {
            if (!modernEntry) {
                Log.e(TAG, "  Failed to load module " + modulePath);
                Log.e(TAG, "  Cannot open assets/xposed_init in the module APK", e);
            }
            return;
        }

        BufferedReader xposedInitReader = new BufferedReader(new InputStreamReader(initIs));
        try {
            String className;
            while ((className = xposedInitReader.readLine()) != null) {
                className = className.trim();
                if (className.isEmpty() || className.startsWith("#"))
                    continue;

                try {
                    Class<?> c = mcl.loadClass(className);

                    if (!IXposedMod.class.isAssignableFrom(c)) {
                        Log.e(TAG, "    Cannot load callback class " + className + " in module " + modulePath + " :");
                        Log.e(TAG, "    This class doesn't implement any sub-interface of IXposedMod, skipping it");
                        continue;
                    }

                    IXposedMod callback = (IXposedMod) c.newInstance();

                    if (callback instanceof IXposedHookZygoteInit && !disableZygoteInitCallbacks) {
                        IXposedHookZygoteInit.StartupParam param = new IXposedHookZygoteInit.StartupParam();
                        param.modulePath = modulePath;
                        param.startsSystemServer = startsSystemServer;
                        ((IXposedHookZygoteInit) callback).initZygote(param);
                    }

                    if (callback instanceof IXposedHookLoadPackage)
                        hookLoadPackage((IXposedHookLoadPackage) callback);

                    // The interface exists so the declaring entry class can be defined at all:
                    // AlterCore does not hook command-line tools.
                    if (callback instanceof IXposedHookCmdInit) {
                        Log.i(TAG, "    " + className + " declares initCmdApp; AlterCore does not"
                                + " hook command-line tools, so it will never be called");
                    }
                    if (callback instanceof IXposedHookInitPackageResources) {
                        XposedBridge.hookInitPackageResources(
                                (IXposedHookInitPackageResources) callback);
                        ResourcesHooks.install();
                    }

                    ExtHandler extHandler = sExtHandler;
                    if (extHandler != null)
                        extHandler.handle(callback);
                } catch (Throwable e) {
                    Log.e(TAG, "    Failed to load class " + className + " from module " + modulePath + " :", e);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "  Failed to load module " + modulePath);
            Log.e(TAG, "  Cannot read assets/xposed_init in the module APK", e);
        } finally {
            closeQuietly(xposedInitReader);
        }
    }

    public static boolean checkModule(ClassLoader mcl) {
        boolean fastPath = mcl instanceof ModuleClassLoader;
        try {
            String name = "com.android.tools.fd.runtime.BootstrapApplication";
            Class<?> cls = fastPath ? ((ModuleClassLoader) mcl).findClass(name) : mcl.loadClass(name);
            if (cls != null) {
                Log.e(TAG, "  Cannot load module, please disable \"Instant Run\" in Android Studio.");
                return false;
            }
        } catch (ClassNotFoundException ignored) {
        }

        // The API classes must resolve to this framework's copies: a module shipping its own
        // de.robv classes gets incompatible callback objects, and a module shipping its own
        // io.github.libxposed classes would never be attached to this framework.
        if (bundlesClass(mcl, fastPath, XposedBridge.class.getName())
                || bundlesClass(mcl, fastPath, "io.github.libxposed.api.XposedModule")) {
            Log.e(TAG, "  Cannot load module:");
            Log.e(TAG, "  The Xposed API classes are compiled into the module's APK.");
            Log.e(TAG, "  This may cause strange issues and must be fixed by the module developer.");
            Log.e(TAG, "  For details, see: http://api.xposed.info/using.html");
            return false;
        }
        return true;
    }

    /** @hide Whether {@code mcl} carries its own copy of {@code className}. */
    private static boolean bundlesClass(ClassLoader mcl, boolean fastPath, String className) {
        if (fastPath) {
            try {
                return ((ModuleClassLoader) mcl).findClass(className) != null;
            } catch (ClassNotFoundException ignored) {
                return false;
            }
        }
        ClassLoader frameworkLoader = XposedBridge.class.getClassLoader();
        Class<?> frameworkCopy;
        try {
            frameworkCopy = Class.forName(className, false, frameworkLoader);
        } catch (ClassNotFoundException e) {
            // This framework does not provide that API at all, so the module cannot conflict with it.
            return false;
        }
        try {
            return mcl.loadClass(className) != frameworkCopy;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "  Cannot load module, " + className + " is not available on the class loader", e);
            Log.e(TAG, "  Make sure you have set parent of the class loader");
            return true;
        }
    }

    public static void hookLoadPackage(IXposedHookLoadPackage callback) {
        sLoadedPackageCallbacks.add(new XC_LoadPackage.Wrapper(callback));
    }

    public static void onPackageLoad(String packageName, String processName, ApplicationInfo appInfo,
                                     boolean isFirstApp, ClassLoader classLoader) {
        XC_LoadPackage.LoadPackageParam param = new XC_LoadPackage.LoadPackageParam(sLoadedPackageCallbacks);
        param.packageName = packageName;
        param.processName = processName;
        param.appInfo = appInfo;
        param.isFirstApplication = isFirstApp;
        param.classLoader = classLoader;
        XC_LoadPackage.callAll(param);
        ModernModuleLoader.onPackageLoad(packageName, processName, appInfo, isFirstApp, classLoader);
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
    }

    public interface ExtHandler {
        void handle(IXposedMod callback);
    }
}