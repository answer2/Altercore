package dev.answer.altercore.xposed.modern;

import android.content.pm.ApplicationInfo;

import java.util.Collections;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * A module loaded through the modern libxposed entry ({@code META-INF/xposed/java_init.list}).
 */
public final class ModernModule {
    private final String modulePath;
    private final ApplicationInfo applicationInfo;
    private final int minApiVersion;
    private final int targetApiVersion;
    private final boolean staticScope;
    private final List<String> scope;
    private final XposedInterface.ExceptionMode globalExceptionMode;

    /** Set when the module is loaded; needed to build a fresh generation on reload. */
    String librarySearchPath;
    ClassLoader moduleClassLoader;

    /** The class names listed in {@code java_init.list}, replayed when a generation is built. */
    List<String> entryClasses = Collections.emptyList();

    /** {@code autoHotReload} from module.prop; only records intent, a service would act on it. */
    boolean autoHotReload;

    /** Index of the entry generation that currently receives lifecycle callbacks. */
    int currentGeneration;

    /**
     * While set, hook registrations coming from an older generation are refused. The reload
     * sequence freezes the outgoing code before capturing its hook handles.
     */
    volatile boolean frozen;

    String lastProcessName;
    boolean lastIsSystemServer;

    final List<Entry> entries = new java.util.ArrayList<>();
    boolean moduleLoadedDispatched;
    boolean systemServer;

    /**
     * Whether a hook registration coming from {@code generation} must be refused: only the current
     * generation may register, and during a reload everything is frozen until the new generation
     * exists, so the outgoing code cannot add hooks while it is being retired.
     */
    boolean rejectsRegistration(int generation) {
        return frozen || generation != currentGeneration;
    }

    /** The entry instances of the current generation, still accepting callbacks. */
    List<Entry> liveEntries() {
        List<Entry> live = new java.util.ArrayList<>();
        for (Entry entry : entries) {
            if (!entry.detached && entry.generation == currentGeneration) {
                live.add(entry);
            }
        }
        return live;
    }

    /** @hide Whether this module's code owns {@code value}'s class, i.e. comes from this APK. */
    boolean isModuleOwned(Object value) {
        if (value == null) {
            return false;
        }
        ClassLoader owner = value.getClass().getClassLoader();
        return owner != null && owner == moduleClassLoader;
    }

    /**
     * One instantiated entry class. {@link #detached} mirrors the API's {@code detach()}: once the
     * entry calls it, the framework stops delivering lifecycle callbacks to that instance.
     */
    static final class Entry {
        final XposedModule module;
        int generation;
        boolean detached;

        Entry(XposedModule module, int generation) {
            this.module = module;
            this.generation = generation;
        }
    }

    ModernModule(String modulePath, ApplicationInfo applicationInfo, int minApiVersion,
                 int targetApiVersion, boolean staticScope, List<String> scope,
                 XposedInterface.ExceptionMode globalExceptionMode) {
        this.modulePath = modulePath;
        this.applicationInfo = applicationInfo;
        this.minApiVersion = minApiVersion;
        this.targetApiVersion = targetApiVersion;
        this.staticScope = staticScope;
        this.scope = scope;
        this.globalExceptionMode = globalExceptionMode;
    }

    public String getModulePath() {
        return modulePath;
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public int getMinApiVersion() {
        return minApiVersion;
    }

    public int getTargetApiVersion() {
        return targetApiVersion;
    }

    /** Index of the entry generation currently receiving callbacks. */
    public int getCurrentGeneration() {
        return currentGeneration;
    }

    public boolean isStaticScope() {
        return staticScope;
    }

    /** The parsed {@code scope.list}, empty when the module does not declare a static scope. */
    public List<String> getScope() {
        return Collections.unmodifiableList(scope);
    }

    XposedInterface.ExceptionMode getGlobalExceptionMode() {
        return globalExceptionMode;
    }

    /** The instantiated entry classes; detached entries keep receiving nothing but stay listed. */
    public List<XposedModule> getEntries() {
        List<XposedModule> modules = new java.util.ArrayList<>();
        for (Entry entry : entries) {
            modules.add(entry.module);
        }
        return Collections.unmodifiableList(modules);
    }

    /**
     * Whether {@code packageName} is inside the module's declared scope. Packages outside a
     * static scope should not be injected at all; a dynamic scope is only a hint, so it is
     * reported as in-scope and left to the module to filter.
     */
    public boolean isAllowlisted(String packageName) {
        if (!staticScope || scope.isEmpty()) {
            return true;
        }
        return scope.contains(packageName);
    }
}
