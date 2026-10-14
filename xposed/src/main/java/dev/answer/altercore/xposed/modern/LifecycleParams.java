package dev.answer.altercore.xposed.modern;

import android.app.AppComponentFactory;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;

import java.util.Collections;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;
/** Lifecycle parameter objects handed to module entries. */
final class LifecycleParams {
    private LifecycleParams() {
    }

    static class ModuleLoaded implements XposedModuleInterface.ModuleLoadedParam {
        private final String processName;
        private final boolean systemServer;

        ModuleLoaded(String processName, boolean systemServer) {
            this.processName = processName;
            this.systemServer = systemServer;
        }

        @Override
        public boolean isSystemServer() {
            return systemServer;
        }

        @Override
        public String getProcessName() {
            return processName;
        }
    }

    static class PackageLoaded implements XposedModuleInterface.PackageLoadedParam {
        final String packageName;
        final ApplicationInfo applicationInfo;
        final boolean firstPackage;
        final ClassLoader defaultClassLoader;

        PackageLoaded(String packageName, ApplicationInfo applicationInfo, boolean firstPackage,
                      ClassLoader defaultClassLoader) {
            this.packageName = packageName;
            this.applicationInfo = applicationInfo;
            this.firstPackage = firstPackage;
            this.defaultClassLoader = defaultClassLoader;
        }

        @Override
        public String getPackageName() {
            return packageName;
        }

        @Override
        public ApplicationInfo getApplicationInfo() {
            return applicationInfo;
        }

        @Override
        public boolean isFirstPackage() {
            return firstPackage;
        }

        @Override
        public ClassLoader getDefaultClassLoader() {
            return defaultClassLoader;
        }
    }

    static final class PackageReady extends PackageLoaded
            implements XposedModuleInterface.PackageReadyParam {
        PackageReady(String packageName, ApplicationInfo applicationInfo, boolean firstPackage,
                     ClassLoader classLoader) {
            super(packageName, applicationInfo, firstPackage, classLoader);
        }

        @Override
        public ClassLoader getClassLoader() {
            return defaultClassLoader;
        }

        /**
         * AlterCore has no hook into {@code AppComponentFactory} instantiation, so the factory is
         * read back from the running application where the platform exposes it (API 31+); the
         * default factory stands in below that, and {@code null} below API 28, where the class
         * does not exist as a per-package object.
         */
        @Override
        public AppComponentFactory getAppComponentFactory() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return null;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    Class<?> activityThread = Class.forName("android.app.ActivityThread");
                    Object application = activityThread
                            .getDeclaredMethod("currentApplication").invoke(null);
                    if (application != null) {
                        // Called reflectively: the compile-time Context stub of this SDK does not
                        // expose the method.
                        Object factory = application.getClass()
                                .getMethod("getAppComponentFactory").invoke(application);
                        if (factory instanceof AppComponentFactory) {
                            return (AppComponentFactory) factory;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            return new AppComponentFactory();
        }
    }

    /**
     * Handed to the outgoing generation. {@code setSavedInstanceState} rejects values owned by the
     * module's own class loader, because keeping them alive would pin the code being retired.
     */
    static final class HotReloading implements XposedModuleInterface.HotReloadingParam {
        private final Bundle extras;
        private final ModernModule module;
        private final Object[] sink;

        HotReloading(Bundle extras, ModernModule module, Object[] sink) {
            this.extras = extras;
            this.module = module;
            this.sink = sink;
        }

        @Override
        public Bundle getExtras() {
            return extras;
        }

        @Override
        public void setSavedInstanceState(Object outState) {
            if (module.isModuleOwned(outState)) {
                throw new IllegalArgumentException("Refusing to carry "
                        + outState.getClass().getName()
                        + " across a hot reload: it was created by the module class loader");
            }
            sink[0] = outState;
        }
    }

    /** Handed to the new generation, with the previous generation's hook handles. */
    static final class HotReloaded extends ModuleLoaded
            implements XposedModuleInterface.HotReloadedParam {
        private final Bundle extras;
        private final Object savedInstanceState;
        private final List<XposedInterface.HookHandle> oldHookHandles;

        HotReloaded(String processName, boolean systemServer, Bundle extras,
                    Object savedInstanceState, List<XposedInterface.HookHandle> oldHookHandles) {
            super(processName, systemServer);
            this.extras = extras;
            this.savedInstanceState = savedInstanceState;
            this.oldHookHandles = oldHookHandles;
        }

        @Override
        public Bundle getExtras() {
            return extras;
        }

        @Override
        public Object getSavedInstanceState() {
            return savedInstanceState;
        }

        @Override
        public List<XposedInterface.HookHandle> getOldHookHandles() {
            return Collections.unmodifiableList(oldHookHandles);
        }
    }
}
