# AlterCore

![](https://img.shields.io/badge/license-LGPL--3.0-orange.svg)
![](https://img.shields.io/badge/Android-8.0%20--%2017-blue.svg)
![](https://img.shields.io/badge/arch-armeabi--v7a%20%7C%20arm64--v8a%20%7C%20x86%20%7C%20x86--64%20%7C%20riscv64-brightgreen.svg)
![](https://img.shields.io/badge/Mode-Debug-red.svg)

AlterCore is a pure-Java Android ART hook library, requiring no native code.

Please note that this project is currently under active development – its API and behavior are subject to change. It is not yet recommended for production use.

## Features

+ Support Android 8.0 - 17 (API level 26 - 37)
+ Support armeabi-v7a, arm64-v8a, x86, x86-64, riscv64
+ Hook methods and constructors, with before / after / replace callbacks
+ Load and run third-party Xposed modules: the legacy `de.robv.android.xposed` API and the modern
  libxposed API (101 and 102: interceptor chain, hook ids, `replaceHook()`, `detach()`, hot reload)
+ Replace app resources and observe layout inflation — in pure Java, with no native code and no
  hidden API access

## Usage
[![Download](https://img.shields.io/maven-central/v/io.github.answer2.altercore/core.svg)](https://repo1.maven.org/maven2/io/github/answer2/altercore/core/)

Groovy
```groovy
dependencies {
    implementation 'io.github.answer2.altercore:core:<version>'
}
```

Kotlin
```groovy
dependencies {
    implementation("io.github.answer2.altercore:core:<version>")
}
```

### Xposed Support
[![Download](https://img.shields.io/maven-central/v/io.github.answer2.altercore/xposed.svg)](https://repo1.maven.org/maven2/io/github/answer2/altercore/xposed/)

`AlterCore:xposed` turns your process into an Xposed framework: it provides both API surfaces to
third-party modules and dispatches their hooks onto the AlterCore engine. Because modules declare
the API as `compileOnly`, this artifact ships `io.github.libxposed:api` as a **runtime**
dependency — you are the framework that has to provide those classes.

```java
// Load a module APK, then tell the framework about each package that starts.
AlterXposed.loadModule(new File("/data/app/~~xxxx/com.example.module-xxxx/base.apk"),
        /* librarySearchPath = */ null, /* startsSystemServer = */ false, moduleInfo);

AlterXposed.onPackageLoad(packageName, processName, appInfo, isFirstApplication, classLoader);

// Hot reload a loaded module (API 102). Pass a class loader over the updated APK to swap in new
// code, or null to rebuild the module's entries from the loader it was first read with.
ModernModuleLoader.ReloadResult result =
        ModernModuleLoader.reload(modulePath, extras, /* replacementLoader = */ null);
```

Both module flavours are loaded: legacy ones declared through `assets/xposed_init`, and modern ones
declared through `META-INF/xposed/java_init.list`, `module.prop` and `scope.list`. A module that
ships both entries is loaded through both paths, which is how dual-flavour modules work elsewhere;
it is the module's job to avoid installing its hooks twice.

Reported versions: `XposedBridge.getXposedVersion()` returns `93`, `XposedInterface.getApiVersion()`
returns `102`.

| Legacy API (`de.robv.android.xposed`) | Status |
| --- | --- |
| `XC_MethodHook`, `XC_MethodReplacement`, `XposedHelpers`, `hookAllMethods/Constructors` | ✅ |
| Constructor hooking, incl. skipping the original `<init>` (per-callback opt-in) | ✅ |
| `XSharedPreferences` | ⚠️ read-only; `edit()` and listeners throw |
| `IXposedHookZygoteInit.initZygote` | ⚠️ called when the module is loaded into the process, not before a zygote fork |
| `IXposedHookInitPackageResources`, `XC_LayoutInflated` | ✅ within the limits below |
| `IXposedHookCmdInit.initCmdApp` | ❌ interface exists so modules still load; never invoked |
| system_server / zygote injection, resource-hooking of theme attributes | ❌ |

| Modern API (libxposed 101 / 102) | Status |
| --- | --- |
| `XposedModule` lifecycle: `onModuleLoaded`, `onPackageLoaded`, `onPackageReady` | ✅ (the latter two share one phase and one class loader: there is no `AppComponentFactory` stage to separate them) |
| `hook(Executable).intercept(Hooker)`, `Chain`, priorities, `ExceptionMode` | ✅ |
| Hook ids (`setId`), `HookHandle.getId()`, `replaceHook()`, `detach()` | ✅ (102) |
| Hot reload: `onHotReloading` handshake, saved-instance hand-off, `getOldHookHandles()` | ✅ (102) |
| `deoptimize()`, `getInvoker()` with `ORIGIN` / `Chain(maxPriority)`, `invokeSpecial()` | ✅ methods; constructors can only be invoked through the chain, and `newInstanceSpecial()` is unsupported |
| `hookClassInitializer()` | ❌ a `<clinit>` cannot be re-entered or intercepted here |
| `getRemotePreferences()`, remote files, `onSystemServerStarting()` | ❌ there is no manager process to talk to; `getFrameworkProperties()` deliberately reports neither capability |

#### Resource hooking

Implemented without native code and without hidden API: AlterCore hooks the **public** `Resources`
getters (`getString`, `getText`, `getDrawable`, `getXml`, `getLayout`, `openRawResource`, arrays,
dimensions, …) and resolves each requested id into its `package/type/entry` key, so
`handleInitPackageResources`, `setReplacement` and `XC_LayoutInflated` take effect app-wide without
replacing any `Resources` instance. A package is offered to modules the first time one of its
resources is looked up, so even that first read can already be replaced.

Limits that follow from the design:

+ theme attributes and `TypedArray` styling cannot be replaced — framework internals read them
  without going through a public getter;
+ a replacement whose value is a resource id must belong to the same package; a foreign id is
  rejected with a log line rather than misresolved, so register the object itself instead;
+ `android.content.res.XResources` does not extend `Resources` — the constructors a subclass needs
  are not visible to a library compiled against `android.jar`. Modules that only call
  `setReplacement` / `hookLayout` are unaffected; one that passes `param.res` where a `Resources` is
  expected gets a `ClassCastException`.

Groovy
```groovy
implementation 'io.github.answer2.altercore:xposed:<version>'
```

Kotlin:
```groovy
implementation("io.github.answer2.altercore:xposed:<version>")
```

## Quick Start

#### Before and After Hook
```java
AlterCore.hook(Activity.class.getDeclaredMethod("onCreate", Bundle.class), new MethodHook() {
                @Override
                public void before(HookParams params) throws Throwable {
                    super.after(params);
                }

                @Override
                public void after(HookParams params) throws Throwable {
                    super.before(params);
                }
            });
```

#### Replace Hook
```java
AlterCore.hook(Activity.class.getDeclaredMethod("onCreate", Bundle.class), new MethodReplacement() {
    @Override
    protected Object replace(HookParams params) throws Throwable {
        return null;
    }
});
```

#### Lambda Style Hook

> **If you use ReplacementHook, you can't use after and before hooks simultaneously**

```java
AlterCore.before(
    Activity.class.getDeclaredMethod("onCreate", Bundle.class),
    params -> {
        Log.d(TAG, "onCreate Before");
});

AlterCore.after(
    Activity.class.getDeclaredMethod("onCreate", Bundle.class),
    params -> {
        Log.d(TAG, "onCreate After");
});

AlterCore.replace(
    Activity.class.getDeclaredMethod("onCreate", Bundle.class),
    params -> {
        Log.d(TAG, "onCreate Replacement");
        return null; // Return null as the method's result
    });
```

## License

This project is licensed under the GNU General Public License v3.0.

## Acknowledgments
- [Pine](https://github.com/canyie/pine) **[996.ICU License]** Dynamic java method hook framework on ART.

- [LSPlant](https://github.com/LSPosed/LSPlant/) **[LGPL-3.0 License]** A hook framework for Android Runtime (ART)

- [epic](https://github.com/tiann/epic/) **[Apache License]** Dynamic java method AOP hook for Android(continution of Dexposed on ART), Supporting 5.0~11

- [PanamaPort](https://github.com/vova7878/PanamaPort) **[MIT License]** Implementation of FFM API for Android 8.0+

- [AndroidVMTools](https://github.com/vova7878/AndroidVMTools) **[MIT License]** Android VM Tools

- [R8Annotations](https://github.com/vova7878/R8Annotations) **[MIT License]** Annotations to specify information for R8 in the code instead of proguard-rules.pro

- [SunCleanerStub](https://github.com/vova7878/SunCleanerStub) **[MIT License]** Wrapper over sun.misc.Cleaner for Android

- [Commons Lang](https://commons.apache.org/proper/commons-lang/) **[Apache License]** A package of Java utility classes for the classes that are in java.lang's hierarchy

- [libxposed API](https://github.com/libxposed/api) **[Apache License]** The modern Xposed module API this library implements and ships as a runtime dependency.

- [LSPosed](https://github.com/LSPosed/LSPosed/) **[GPL-3.0 License]** Reference implementation whose legacy API shapes (`IXposedHookCmdInit`, `XC_InitPackageResources`, `XC_LayoutInflated`, `XResources`, `HiddenApiBridge`) are followed here for module compatibility.

- [PineTool](https://github.com/answer2/PineTool/) **[LGPL-3.0 License]** PineTool is a tool that makes using Pine easier.
