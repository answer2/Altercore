package dev.answer.altercore.xposed;

import android.content.res.Resources;
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
 * dimensions, integers and booleans fetched through {@code Resources}, plus layout inflation
 * callbacks. Not covered: theme attributes and {@code TypedArray} styling, which the framework
 * reads without a public getter, and resource ids belonging to another package.</p>
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

    private static final Set<String> sOfferedPackages = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> sInsideDispatch = ThreadLocal.withInitial(() -> false);

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
            XposedHelpers.findAndHookMethod(LayoutInflater.class, "inflate", int.class,
                    ViewGroup.class, boolean.class, new LayoutHook());
            sInstalled = true;
            Log.i(TAG, "Resource interception installed for " + RESOURCE_GETTERS.length
                    + " getters plus layout inflation");
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
                String[] substituteKey = keyOf(resources, substitute);
                if (substituteKey == null || !substituteKey[0].equals(key[0])) {
                    Log.w(TAG, "Ignoring cross-package id replacement for "
                            + XResources.describe(key[0], key[1], key[2])
                            + "; register the object itself instead of its id");
                    return;
                }
                param.args[0] = substitute;
                return;
            }
            param.setResult(replacement);
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
