package dev.answer.alterapp;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Second entry class of this app's module descriptor, used to verify that {@code detach()} stops
 * callbacks for the entry that called it and for nobody else: {@link ModernChainModule} keeps
 * receiving package events while this entry goes silent.
 */
public class DetachingModule extends XposedModule {

    public static volatile int packageLoadedCalls;
    public static volatile int packageReadyCalls;

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!"dev.answer.alterapp".equals(param.getPackageName())) {
            return;
        }
        packageLoadedCalls++;
        detach();
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        // Unreachable: this entry detached itself one callback earlier.
        packageReadyCalls++;
    }

    public static String counters() {
        return packageLoadedCalls + "," + packageReadyCalls;
    }

    /** Called right before a dispatch round so the counters describe exactly that one round. */
    public static void reset() {
        packageLoadedCalls = 0;
        packageReadyCalls = 0;
    }
}
