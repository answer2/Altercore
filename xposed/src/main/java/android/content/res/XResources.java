package android.content.res;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource replacement handle, as legacy modules know it: the object handed to
 * {@link de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam#res}
 * and the receiver of {@code setReplacement(...)} and {@code hookLayout(...)} calls.
 *
 * <p>Replacements are applied by dev.answer.altercore.xposed.ResourcesHooks, which hooks the public
 * {@code Resources} getters, so no instance is replaced and no hidden member is needed. Two limits
 * follow: this class does not extend {@code Resources} (the constructors a subclass needs are
 * absent from {@code android.jar}), and a replacement given as a resource id must belong to the
 * package being served, since we cannot resolve ids from another package's table.</p>
 */
public class XResources {

    /** Keyed by {@code packageName + ":" + type + "/" + entryName}. */
    private static final Map<String, Object> REPLACEMENTS = new ConcurrentHashMap<>();

    private final ClassLoader loader;

    public XResources(ClassLoader loader) {
        this.loader = loader;
    }

    /** @hide The loader the module's own classes came from, kept for diagnostics. */
    public ClassLoader getClassLoaderForResources() {
        return loader;
    }

    /** Replaces a resource in the package this handle was created for. */
    public void setReplacement(String type, String entryName, Object replacement) {
        setReplacement(null, type, entryName, replacement);
    }

    public void setReplacement(String packageName, String type, String entryName,
                               Object replacement) {
        if (type == null || entryName == null) {
            throw new IllegalArgumentException("type and entryName must not be null");
        }
        if (replacement == null) {
            REPLACEMENTS.remove(key(packageName, type, entryName));
        } else {
            REPLACEMENTS.put(key(packageName, type, entryName), replacement);
        }
    }

    public Object getReplacement(String type, String entryName) {
        return getReplacement(null, type, entryName);
    }

    public Object getReplacement(String packageName, String type, String entryName) {
        return REPLACEMENTS.get(key(packageName, type, entryName));
    }

    /** @hide Lookup by the decoded identity of a resource id; {@code null} when nothing is registered. */
    public static Object replacementFor(String packageName, String type, String entryName) {
        Object exact = REPLACEMENTS.get(key(packageName, type, entryName));
        return exact != null ? exact : REPLACEMENTS.get(key(null, type, entryName));
    }

    /** @hide Drop every replacement registered for {@code packageName}. */
    public static void clearReplacements(String packageName) {
        REPLACEMENTS.keySet().removeIf(k -> k.startsWith(packageName + ":"));
    }

    /**
     * Registers a callback that is invoked once the named layout has been inflated.
     *
     * @see de.robv.android.xposed.callbacks.XC_LayoutInflated
     */
    public void hookLayout(String type, String entryName,
                           de.robv.android.xposed.callbacks.XC_LayoutInflated callback) {
        hookLayout(null, type, entryName, callback);
    }

    public void hookLayout(String packageName, String type, String entryName,
                           de.robv.android.xposed.callbacks.XC_LayoutInflated callback) {
        de.robv.android.xposed.XposedBridge.hookLayout(packageName, type, entryName, callback);
    }

    public void unhookLayout(String type, String entryName,
                             de.robv.android.xposed.callbacks.XC_LayoutInflated callback) {
        de.robv.android.xposed.XposedBridge.unhookLayout(null, type, entryName, callback);
    }

    public void unhookLayout(String packageName, String type, String entryName,
                             de.robv.android.xposed.callbacks.XC_LayoutInflated callback) {
        de.robv.android.xposed.XposedBridge.unhookLayout(packageName, type, entryName, callback);
    }

    /**
     * Container for the name of a layout resource, as carried by
     * {@link de.robv.android.xposed.callbacks.XC_LayoutInflated.LayoutInflatedParam#resNames}.
     */
    public static class ResourceNames {
        /** Name of the resource without its type, e.g. {@code activity_main}. */
        public String name;

        /** Type of the resource, e.g. {@code layout}. */
        public String defType;

        public Integer id = null;
    }

    private static String key(String packageName, String type, String entryName) {
        return (packageName == null ? "" : packageName) + ":" + type + "/" + entryName;
    }

    /** @hide Formatted for the log lines that report a substitution. */
    public static String describe(String packageName, String type, String entryName) {
        return String.format(Locale.ROOT, "%s:%s/%s", packageName, type, entryName);
    }
}
