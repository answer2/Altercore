package dev.answer.altercore.xposed.modern;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Loads modules that declare themselves through {@code META-INF/xposed/*} instead of the legacy
 * {@code assets/xposed_init}, and drives their lifecycle callbacks.
 */
public final class ModernModuleLoader {
    private static final String TAG = "AlterXposed";
    private static final String DIR = "META-INF/xposed/";
    private static final String ENTRY_LIST = DIR + "java_init.list";
    private static final String MODULE_PROP = DIR + "module.prop";
    private static final String SCOPE_LIST = DIR + "scope.list";

    /** The API level of the {@code io.github.libxposed:api} artifact AlterCore is built against. */
    public static final int SUPPORTED_API = XposedInterface.API_102;

    private static final List<ModernModule> sLoadedModules = new ArrayList<>();

    private ModernModuleLoader() {
    }

    public static List<ModernModule> getLoadedModules() {
        synchronized (sLoadedModules) {
            return Collections.unmodifiableList(new ArrayList<>(sLoadedModules));
        }
    }

    /**
     * Reads the modern module descriptor of {@code mcl} and registers its entry classes.
     *
     * @param moduleInfo the module's {@link ApplicationInfo}; pass {@code null} when the host has
     *                   no {@code PackageManager}, in which case a descriptor carrying only the
     *                   APK paths is synthesized
     * @return {@code true} if the APK declares a modern entry, even if loading then failed
     */
    public static boolean load(ClassLoader mcl, String modulePath, String librarySearchPath,
                               ApplicationInfo moduleInfo, boolean startsSystemServer) {
        String entryList;
        try {
            entryList = readResource(mcl, modulePath, ENTRY_LIST);
        } catch (IOException e) {
            Log.e(TAG, "  Cannot read " + ENTRY_LIST + " in " + modulePath, e);
            return false;
        }
        if (entryList == null || entryList.trim().isEmpty()) {
            return false;
        }

        Properties prop = new Properties();
        try {
            String raw = readResource(mcl, modulePath, MODULE_PROP);
            if (raw != null) {
                prop.load(new java.io.StringReader(raw));
            }
        } catch (IOException e) {
            Log.e(TAG, "  Cannot parse " + MODULE_PROP + " in " + modulePath, e);
            return true;
        }

        int minApi = prop.getProperty("minApiVersion", "").trim().isEmpty()
                ? -1 : parseInt(prop.getProperty("minApiVersion"), -1);
        int targetApi = parseInt(prop.getProperty("targetApiVersion"), minApi);
        if (minApi < 0 || targetApi < 0) {
            Log.e(TAG, "  Failed to load module " + modulePath
                    + ": module.prop must declare minApiVersion and targetApiVersion");
            return true;
        }
        if (minApi > SUPPORTED_API) {
            Log.e(TAG, "  Failed to load module " + modulePath + ": it requires API " + minApi
                    + ", this framework provides " + SUPPORTED_API);
            return true;
        }

        boolean staticScope = Boolean.parseBoolean(prop.getProperty("staticScope", "false"));
        List<String> scope = new ArrayList<>();
        if (staticScope) {
            try {
                String raw = readResource(mcl, modulePath, SCOPE_LIST);
                if (raw != null) {
                    for (String line : raw.split("\\R")) {
                        String name = line.trim();
                        if (!name.isEmpty() && !name.startsWith("#")) {
                            scope.add(name);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "  Cannot read " + SCOPE_LIST + " in " + modulePath, e);
            }
        }

        XposedInterface.ExceptionMode globalMode = "passthrough"
                .equalsIgnoreCase(prop.getProperty("exceptionMode", "protective").trim())
                ? XposedInterface.ExceptionMode.PASSTHROUGH
                : XposedInterface.ExceptionMode.PROTECTIVE;

        ModernModule module = new ModernModule(modulePath, moduleInfo != null ? moduleInfo
                : synthesizeApplicationInfo(modulePath), minApi, targetApi, staticScope, scope, globalMode);
        module.systemServer = startsSystemServer;
        module.autoHotReload = Boolean.parseBoolean(prop.getProperty("autoHotReload", "false"));
        module.librarySearchPath = librarySearchPath;
        module.moduleClassLoader = mcl;
        module.entryClasses = parseLines(entryList);

        List<ModernModule.Entry> instantiated = instantiateEntries(module, mcl, 0);
        module.entries.addAll(instantiated);
        if (!instantiated.isEmpty()) {
            synchronized (sLoadedModules) {
                sLoadedModules.add(module);
            }
            Log.i(TAG, "  Loaded modern module " + modulePath + " (api " + minApi + "-" + targetApi
                    + ", " + instantiated.size() + " entries)");
        }
        return true;
    }

    /** Creates one attached entry instance per declared class, for the given generation. */
    private static List<ModernModule.Entry> instantiateEntries(ModernModule module, ClassLoader loader,
                                                               int generation) {
        List<ModernModule.Entry> created = new ArrayList<>();
        AlterXposedInterface xposed = new AlterXposedInterface(module, generation);
        for (String name : module.entryClasses) {
            try {
                Class<?> c = loader.loadClass(name);
                if (!XposedModule.class.isAssignableFrom(c)) {
                    Log.e(TAG, "    Cannot load entry class " + name + " in module "
                            + module.getModulePath()
                            + ": it does not extend io.github.libxposed.api.XposedModule, skipping it");
                    continue;
                }
                XposedModule entry = (XposedModule) c.getDeclaredConstructor().newInstance();
                final ModernModule.Entry handle = new ModernModule.Entry(entry, generation);
                // detach() is per entry: only the instance that calls it stops receiving
                // callbacks, the module's other entries keep going.
                entry.attachFramework(xposed, () -> handle.detached = true);
                created.add(handle);
            } catch (Throwable t) {
                Log.e(TAG, "    Failed to load entry class " + name + " from module "
                        + module.getModulePath(), t);
            }
        }
        return created;
    }

    private static List<String> parseLines(String raw) {
        List<String> lines = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String value = line.trim();
            if (!value.isEmpty() && !value.startsWith("#")) {
                lines.add(value);
            }
        }
        return lines;
    }

    /**
     * Notifies every loaded module about a package in the current process. {@code onModuleLoaded}
     * is emitted here the first time, because that is when the process name becomes known.
     */
    public static void onPackageLoad(String packageName, String processName,
                                     ApplicationInfo appInfo, boolean isFirstApplication,
                                     ClassLoader classLoader) {
        List<ModernModule> modules;
        synchronized (sLoadedModules) {
            modules = new ArrayList<>(sLoadedModules);
        }

        for (ModernModule module : modules) {
            module.lastProcessName = processName;
            module.lastIsSystemServer = module.systemServer;
            if (!module.moduleLoadedDispatched) {
                module.moduleLoadedDispatched = true;
                XposedModuleInterface.ModuleLoadedParam loadedParam =
                        new LifecycleParams.ModuleLoaded(processName, module.systemServer);
                for (ModernModule.Entry entry : module.liveEntries()) {
                    final XposedModule target = entry.module;
                    safe("onModuleLoaded", target, () -> target.onModuleLoaded(loadedParam));
                }
            }
            if (!module.isAllowlisted(packageName)) {
                continue;
            }
            LifecycleParams.PackageLoaded packageLoaded =
                    new LifecycleParams.PackageLoaded(packageName, appInfo, isFirstApplication, classLoader);
            LifecycleParams.PackageReady packageReady =
                    new LifecycleParams.PackageReady(packageName, appInfo, isFirstApplication, classLoader);
            for (ModernModule.Entry entry : module.liveEntries()) {
                final XposedModule target = entry.module;
                safe("onPackageLoaded", target, () -> target.onPackageLoaded(packageLoaded));
                // A detach() performed inside onPackageLoaded must not be followed by the
                // remaining callbacks of this same dispatch round.
                if (entry.detached) {
                    continue;
                }
                safe("onPackageReady", target, () -> target.onPackageReady(packageReady));
            }
        }
    }

    /** Outcome of a {@link #reload} request. */
    public enum ReloadResult {
        SUCCESS, NOT_FOUND, REJECTED_BY_MODULE, FAILED
    }

    /**
     * Hot reloads a module: asks the running generation for permission, then builds a new
     * generation and hands it the previous generation's hook handles.
     *
     * <p>{@code replacementLoader} is how a host supplies the updated code — pass a class loader
     * over the new APK to genuinely swap in new classes, or {@code null} to rebuild the entries
     * from the loader the module was first read with (same classes, new instances and new hook
     * registrations).</p>
     *
     * <p>The outgoing generation is frozen first, so further hook registrations from old code
     * fail, and its entries stop receiving lifecycle callbacks once the new generation exists.
     * In-flight calls keep using the hook chain snapshot they started with.</p>
     */
    public static ReloadResult reload(String modulePath, Bundle extras, ClassLoader replacementLoader) {
        ModernModule module = findByPath(modulePath);
        if (module == null) {
            Log.e(TAG, "  Cannot reload " + modulePath + ": no modern module is loaded from it");
            return ReloadResult.NOT_FOUND;
        }

        final int oldGeneration = module.currentGeneration;
        List<XposedInterface.HookHandle> oldHandles = HookRegistry.handlesOf(module, oldGeneration);
        module.frozen = true;

        Object[] carried = new Object[1];
        LifecycleParams.HotReloading reloadingParam =
                new LifecycleParams.HotReloading(extras, module, carried);
        boolean accepted = false;
        for (ModernModule.Entry entry : module.liveEntries()) {
            final XposedModule target = entry.module;
            try {
                if (target.onHotReloading(reloadingParam)) {
                    accepted = true;
                }
            } catch (Throwable t) {
                Log.e(TAG, "  " + target.getClass().getName() + ".onHotReloading threw", t);
            }
        }
        if (!accepted) {
            module.frozen = false;
            Log.i(TAG, "  Hot reload of " + modulePath + " refused by the module");
            return ReloadResult.REJECTED_BY_MODULE;
        }

        ClassLoader loader = replacementLoader != null ? replacementLoader : module.moduleClassLoader;
        List<ModernModule.Entry> fresh;
        try {
            module.currentGeneration = oldGeneration + 1;
            fresh = instantiateEntries(module, loader, module.currentGeneration);
        } catch (Throwable t) {
            Log.e(TAG, "  Failed to build a new generation for " + modulePath, t);
            module.currentGeneration = oldGeneration;
            module.frozen = false;
            return ReloadResult.FAILED;
        }
        if (fresh.isEmpty()) {
            Log.e(TAG, "  New generation of " + modulePath + " produced no entries, keeping the old one");
            module.currentGeneration = oldGeneration;
            module.frozen = false;
            return ReloadResult.FAILED;
        }

        // The old entries are retired only once the new code exists, and the framework drops its
        // references to them here; hooks they still hold stay installed until onHotReloaded says.
        for (ModernModule.Entry entry : module.entries) {
            if (entry.generation == oldGeneration) {
                entry.detached = true;
            }
        }
        module.entries.addAll(fresh);
        module.moduleClassLoader = loader;
        module.frozen = false;
        moduleReloaded(module, oldGeneration, oldHandles, carried[0], extras);
        return ReloadResult.SUCCESS;
    }

    private static void moduleReloaded(ModernModule module, int oldGeneration,
                                        List<XposedInterface.HookHandle> oldHandles,
                                        Object savedInstanceState, Bundle extras) {
        LifecycleParams.HotReloaded reloadedParam = new LifecycleParams.HotReloaded(
                module.lastProcessName, module.lastIsSystemServer, extras, savedInstanceState, oldHandles);
        for (ModernModule.Entry entry : module.liveEntries()) {
            final XposedModule target = entry.module;
            safe("onHotReloaded", target, () -> target.onHotReloaded(reloadedParam));
        }
        Log.i(TAG, "  Hot reloaded " + module.getModulePath() + " into generation "
                + module.currentGeneration + " (previous generation " + oldGeneration + " held "
                + oldHandles.size() + " hooks)");
    }

    private static ModernModule findByPath(String modulePath) {
        synchronized (sLoadedModules) {
            for (ModernModule module : sLoadedModules) {
                if (module.getModulePath().equals(modulePath)) {
                    return module;
                }
            }
        }
        return null;
    }

    private interface Callback {
        void run() throws Throwable;
    }

    private static void safe(String what, XposedModule entry, Callback callback) {
        try {
            callback.run();
        } catch (Throwable t) {
            Log.e(TAG, "  " + entry.getClass().getName() + "." + what + " threw", t);
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static ApplicationInfo synthesizeApplicationInfo(String modulePath) {
        ApplicationInfo info = new ApplicationInfo();
        info.sourceDir = modulePath;
        info.publicSourceDir = modulePath;
        File parent = new File(modulePath).getParentFile();
        info.nativeLibraryDir = parent != null ? parent.getAbsolutePath() : null;
        return info;
    }

    /**
     * Reads a resource out of the module APK. The class loader is tried first; the raw zip is used
     * as a fallback, because a {@code PathClassLoader} does not always surface {@code META-INF}
     * entries of an APK it was handed as a plain dex path.
     */
    /* package */ static String readResource(ClassLoader mcl, String modulePath, String name)
            throws IOException {
        InputStream in = mcl.getResourceAsStream(name);
        if (in != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return readAll(reader);
            }
        }

        File apk = new File(modulePath);
        if (!apk.isFile()) {
            return null;
        }
        try (ZipFile zip = new ZipFile(apk)) {
            ZipEntry entry = zip.getEntry(name);
            if (entry == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
                return readAll(reader);
            }
        }
    }

    private static String readAll(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
