package dev.answer.altercore.xposed;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XResources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedBridge.CopyOnWriteSortedSet;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XCallback;

/**
 * Resource hooking for legacy modules, without native code or hidden API.
 *
 * <p>Upstream replaces the host's {@code Resources} objects with {@code XResources} clones, which
 * needs hidden constructors, per-version {@code ResourcesManager} surgery and a native helper. We
 * instead hook the public {@code Resources} getters and decode each id into its
 * {@code package/type/entry} key with the public {@code getResource*Name} methods.</p>
 *
 * <p>Covered: strings, char sequences, drawables, colors, raw and XML streams, layouts, arrays,
 * dimensions, integers and booleans fetched through {@code Resources}; the same kinds again when
 * they reach a view through a style or theme attribute; and layout inflation callbacks. Not
 * covered: style values written as literals rather than resource references, which the asset
 * manager would have to rewrite, and resources of a package the hooked app cannot resolve itself.</p>
 */
public final class ResourcesHooks {
    private static final String TAG = "AlterCore/Res";

    /** Resources getters whose first argument is a resource id. */
    private static final String[] RESOURCE_GETTERS = {
            "getString", "getText", "getDrawable", "getDrawableForDensity", "getColor",
            "getColorStateList", "openRawResource", "openRawResourceFd", "getXml", "getLayout",
            "getAnimation", "getStringArray", "getTextArray", "getIntArray", "getFloatArray",
            "getBoolean", "getInteger", "getDimension", "getDimensionPixelSize",
            "getDimensionPixelOffset", "getFraction", "getQuantityString",
    };

    /**
     * TypedArray getters whose first argument is an index, with the {@code Resources} method used to
     * materialize a replacement given as a resource id. Style and theme values reach views through
     * these getters, so a replaced resource also shows up where a style referenced it.
     *
     * <p>{@code getResourceId} is deliberately absent: the hook itself reads the resolved id through
     * that method, so hooking it would recurse.</p>
     */
    private static final String[][] TYPED_ARRAY_GETTERS = {
            {"getString", "getString"},
            {"getText", "getText"},
            {"getDrawable", "getDrawable"},
            {"getColorStateList", "getColorStateList"},
            {"getInteger", null},
    };

    private static final Set<String> sOfferedPackages = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> sInsideDispatch = ThreadLocal.withInitial(() -> false);
    /** Set while a replacement is materialized through Resources, to keep the two layers apart. */
    private static final ThreadLocal<Boolean> sMaterializing = ThreadLocal.withInitial(() -> false);
    /** Set while the TypedArray layer inspects an array, so its own reads are not intercepted. */
    private static final ThreadLocal<Boolean> sInspecting = ThreadLocal.withInitial(() -> false);

    /** Sentinel for "no replacement", since {@code null} is a legitimate absence. */
    private static final Object NONE = new Object();

    private static volatile boolean sInstalled;
    private static volatile XResources sHandle;

    private ResourcesHooks() {
    }

    /** Installs the interception layer once per process. */
    public static synchronized void install() {
        if (sInstalled) {
            return;
        }
        try {
            for (String getter : RESOURCE_GETTERS) {
                XposedBridge.hookAllMethods(Resources.class, getter, new GetterHook());
            }
            for (String[] entry : TYPED_ARRAY_GETTERS) {
                XposedBridge.hookAllMethods(TypedArray.class, entry[0], new TypedArrayHook(entry[1]));
            }
            XposedHelpers.findAndHookMethod(LayoutInflater.class, "inflate", int.class,
                    ViewGroup.class, boolean.class, new LayoutHook());
            sInstalled = true;
            Log.i(TAG, "Resource interception installed for " + RESOURCE_GETTERS.length
                    + " getters, " + TYPED_ARRAY_GETTERS.length + " TypedArray getters and"
                    + " layout inflation");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to install resource interception", t);
        }
    }

    /** @hide Handle passed to modules as {@code InitPackageResourcesParam.res}. */
    private static XResources handle() {
        XResources handle = sHandle;
        if (handle == null) {
            handle = new XResources(ResourcesHooks.class.getClassLoader());
            sHandle = handle;
        }
        return handle;
    }

    /** Offers the package to every module that declared {@link de.robv.android.xposed.IXposedHookInitPackageResources}. */
    private static void offerPackage(Resources resources, String packageName) {
        if (sInsideDispatch.get() || !sOfferedPackages.add(packageName)) {
            return;
        }
        XC_InitPackageResources.InitPackageResourcesParam resparam =
                new XC_InitPackageResources.InitPackageResourcesParam(
                        XposedBridge.initPackageResourcesCallbacks());
        resparam.packageName = packageName;
        resparam.res = handle();

        sInsideDispatch.set(true);
        try {
            XCallback.callAll(resparam);
        } finally {
            sInsideDispatch.set(false);
        }
        Log.i(TAG, "handleInitPackageResources dispatched for " + packageName);
    }

    /** @hide {@code package/type/entry} of an id, or {@code null} when this table cannot resolve it. */
    private static String[] keyOf(Resources resources, int id) {
        try {
            return new String[] {
                    resources.getResourcePackageName(id),
                    resources.getResourceTypeName(id),
                    resources.getResourceEntryName(id),
            };
        } catch (Resources.NotFoundException e) {
            // An id from another package's table: nothing this hook can serve, and upstream
            // could not either without the owning Resources object.
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Intercepts every public resource lookup and applies the registered replacement. */
    private static final class GetterHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (sMaterializing.get()) {
                return;
            }
            if (!(param.thisObject instanceof Resources) || param.args.length == 0
                    || !(param.args[0] instanceof Integer)) {
                return;
            }
            Resources resources = (Resources) param.thisObject;
            int id = (Integer) param.args[0];
            String[] key = keyOf(resources, id);
            if (key == null) {
                return;
            }

            // Before consulting the table, give modules their one chance to fill it.
            offerPackage(resources, key[0]);

            Object replacement = XResources.replacementFor(key[0], key[1], key[2]);
            if (replacement == null) {
                return;
            }
            if (replacement instanceof Integer) {
                int substitute = (Integer) replacement;
                // Any id the target can resolve works, since we just hand it to the original
                // getter: its own resources, and the framework's (android.R.*).
                if (keyOf(resources, substitute) == null) {
                    Log.w(TAG, "Ignoring replacement " + substitute + " for "
                            + XResources.describe(key[0], key[1], key[2]) + ", the target cannot"
                            + " resolve that id; register the object itself instead");
                    return;
                }
                param.args[0] = substitute;
                return;
            }
            param.setResult(replacement);
        }
    }

    /** Intercepts a style or theme value read and applies the replacement of the resource it names. */
    private static final class TypedArrayHook extends XC_MethodHook {
        private final String materializer;

        TypedArrayHook(String materializer) {
            this.materializer = materializer;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (sMaterializing.get() || sInspecting.get() || XResources.isEmpty()) {
                return;
            }
            if (!(param.thisObject instanceof TypedArray) || param.args.length == 0
                    || !(param.args[0] instanceof Integer)) {
                return;
            }
            TypedArray array = (TypedArray) param.thisObject;
            int index = (Integer) param.args[0];
            if (index < 0 || index >= array.getIndexCount()) {
                return;
            }

            Object applied;
            sInspecting.set(true);
            try {
                applied = replacementForIndex(array, index);
            } finally {
                sInspecting.set(false);
            }
            if (applied != NONE) {
                param.setResult(applied);
            }
        }

        /** The value to return for an index, already converted to this getter's own kind. */
        private Object replacementForIndex(TypedArray array, int index) throws Throwable {
            int id = array.getResourceId(index, 0);
            if (id == 0) {
                // A literal value: there is no resource to name it by, and patching those would
                // have to happen in the asset manager itself.
                return NONE;
            }
            Resources resources = array.getResources();
            String[] key = keyOf(resources, id);
            if (key == null) {
                return NONE;
            }
            offerPackage(resources, key[0]);
            Object replacement = XResources.replacementFor(key[0], key[1], key[2]);
            if (replacement == null) {
                return NONE;
            }
            if (!(replacement instanceof Integer)) {
                return materializer != null ? replacement : NONE;
            }
            int substitute = (Integer) replacement;
            if (keyOf(resources, substitute) == null) {
                Log.w(TAG, "Ignoring replacement " + substitute + " referenced through "
                        + XResources.describe(key[0], key[1], key[2]));
                return NONE;
            }
            if (materializer == null) {
                return substitute;
            }
            sMaterializing.set(true);
            try {
                return Resources.class.getMethod(materializer, int.class)
                        .invoke(resources, substitute);
            } finally {
                sMaterializing.set(false);
            }
        }
    }

    /**
     * Reports inflated layouts to modules. Layout replacement happens earlier, in the
     * {@code Resources.getLayout} hook, so the callback keeps describing the layout the caller
     * asked for even when a different one ends up inflated.
     */
    private static final class LayoutHook extends XC_MethodHook {
        @Override
        @SuppressWarnings("unchecked")
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!(param.thisObject instanceof LayoutInflater)) {
                return;
            }
            if (param.args.length == 0 || !(param.args[0] instanceof Integer)) {
                return;
            }
            Object result = param.getResult();
            if (!(result instanceof View)) {
                return;
            }

            Resources resources;
            try {
                resources = ((LayoutInflater) param.thisObject).getContext().getResources();
            } catch (Throwable t) {
                return;
            }
            int id = (Integer) param.args[0];
            String[] key = keyOf(resources, id);
            if (key == null) {
                return;
            }
            offerPackage(resources, key[0]);

            CopyOnWriteSortedSet<XC_LayoutInflated> callbacks =
                    XposedBridge.layoutCallbacks(
                            XposedBridge.layoutKey(key[0], key[1], key[2]));
            if (callbacks == null || callbacks.getSnapshot().length == 0) {
                return;
            }

            XC_LayoutInflated.LayoutInflatedParam liparam =
                    new XC_LayoutInflated.LayoutInflatedParam(callbacks);
            liparam.view = (View) result;
            XResources.ResourceNames names = new XResources.ResourceNames();
            names.id = id;
            names.name = key[2];
            names.defType = key[1];
            liparam.resNames = names;
            liparam.res = handle();
            XCallback.callAll(liparam);
        }
    }
}
