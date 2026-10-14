package dev.answer.altercore.xposed.modern;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import dev.answer.altercore.AlterCore;

import io.github.libxposed.api.XposedInterface;

/**
 * The framework side of the modern libxposed API, one instance per loaded module.
 *
 * <p>AlterCore is an in-process hook engine without a manager app, so the framework properties
 * report no {@code PROP_CAP_SYSTEM} (no system_server injection) and no {@code PROP_CAP_REMOTE}
 * (no cross-process preference or file sharing); the remote APIs fail loudly instead of
 * silently returning empty data.</p>
 */
public final class AlterXposedInterface implements XposedInterface {
    private static final String DEFAULT_TAG = "AlterCore";

    // Hosts may override these to report their own identity to modules.
    private static volatile String sFrameworkName = "AlterCore";
    private static volatile String sFrameworkVersion = "0.0.2";
    private static volatile long sFrameworkVersionCode = 2L;

    private final ModernModule module;
    private final int generation;

    AlterXposedInterface(ModernModule module, int generation) {
        this.module = module;
        this.generation = generation;
    }

    public static void setFrameworkName(String name) {
        sFrameworkName = name;
    }

    public static void setFrameworkVersion(String version, long versionCode) {
        sFrameworkVersion = version;
        sFrameworkVersionCode = versionCode;
    }

    /**
     * {@code getApiVersion()} is deliberately not overridden: the API defines it as the value of
     * {@code LIB_API}, which is the version of the {@code io.github.libxposed:api} artifact this
     * framework was built against.
     */
    @Override
    public String getFrameworkName() {
        return sFrameworkName;
    }

    @Override
    public String getFrameworkVersion() {
        return sFrameworkVersion;
    }

    @Override
    public long getFrameworkVersionCode() {
        return sFrameworkVersionCode;
    }

    @Override
    public long getFrameworkProperties() {
        return 0L;
    }

    @Override
    public HookBuilder hook(Executable origin) {
        return new BuilderImpl(origin, module, generation);
    }

    @Override
    public HookBuilder hookClassInitializer(Class<?> origin) {
        throw new UnsupportedOperationException(
                "AlterCore cannot intercept the class initializer of " + origin.getName());
    }

    @Override
    public boolean deoptimize(Executable executable) {
        try {
            AlterCore.deoptimize(executable);
            return true;
        } catch (Throwable t) {
            Log.e(DEFAULT_TAG, "Failed to deoptimize " + executable, t);
            return false;
        }
    }

    @Override
    public Invoker<?, Method> getInvoker(Method method) {
        return new InvokerImpl(method);
    }

    @Override
    public <T> CtorInvoker<T> getInvoker(Constructor<T> constructor) {
        return new ConstructorInvokerImpl<>(constructor);
    }

    @Override
    public void log(int priority, String tag, String msg) {
        log(priority, tag, msg, null);
    }

    @Override
    public void log(int priority, String tag, String msg, Throwable tr) {
        String realTag = tag == null ? DEFAULT_TAG : tag;
        switch (priority) {
            case Log.VERBOSE:
                Log.v(realTag, msg, tr);
                break;
            case Log.DEBUG:
                Log.d(realTag, msg, tr);
                break;
            case Log.WARN:
                Log.w(realTag, msg, tr);
                break;
            case Log.ERROR:
                Log.e(realTag, msg, tr);
                break;
            case Log.ASSERT:
                Log.wtf(realTag, msg, tr);
                break;
            default:
                Log.i(realTag, msg, tr);
                break;
        }
    }

    @Override
    public ApplicationInfo getModuleApplicationInfo() {
        return module.getApplicationInfo();
    }

    @Override
    public SharedPreferences getRemotePreferences(String group) {
        throw new UnsupportedOperationException(
                "This framework does not provide remote preferences (PROP_CAP_REMOTE is unset)");
    }

    @Override
    public String[] listRemoteFiles() {
        throw new UnsupportedOperationException(
                "This framework does not provide remote files (PROP_CAP_REMOTE is unset)");
    }

    @Override
    public ParcelFileDescriptor openRemoteFile(String name) {
        throw new UnsupportedOperationException(
                "This framework does not provide remote files (PROP_CAP_REMOTE is unset)");
    }

    static final class BuilderImpl implements HookBuilder {
        private final Executable origin;
        private final ModernModule owner;
        private final int generation;
        private final ExceptionMode moduleDefault;

        private int priority = PRIORITY_DEFAULT;
        private ExceptionMode exceptionMode = ExceptionMode.DEFAULT;
        private String id;

        BuilderImpl(Executable origin, ModernModule owner, int generation) {
            this.origin = origin;
            this.owner = owner;
            this.generation = generation;
            this.moduleDefault = owner.getGlobalExceptionMode();
        }

        @Override
        public HookBuilder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public HookBuilder setExceptionMode(ExceptionMode mode) {
            this.exceptionMode = mode == null ? ExceptionMode.DEFAULT : mode;
            return this;
        }

        @Override
        public HookBuilder setId(String id) {
            this.id = id;
            return this;
        }

        @Override
        public HookHandle intercept(Hooker hooker) {
            if (hooker == null) {
                throw new IllegalArgumentException("hooker == null");
            }
            ExceptionMode effective = exceptionMode == ExceptionMode.DEFAULT
                    ? moduleDefault : exceptionMode;
            return HookRegistry.hook(owner, origin, priority, effective, id, generation, hooker);
        }
    }
}
