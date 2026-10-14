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
+ Hook methods and constructors with before, after and replace callbacks
+ Load third-party Xposed modules, both the legacy `de.robv.android.xposed` API and the libxposed API
+ Replace resources of hooked apps and get notified when their layouts are inflated

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

`AlterCore:xposed` lets your process host Xposed modules: it provides the module facing APIs and
dispatches module hooks onto the AlterCore engine. Modules compile against `io.github.libxposed:api`
as `compileOnly`, so this artifact carries it as a runtime dependency, because your app is the
framework that has to provide those classes.

Load a module APK and report every package that starts:

```java
AlterXposed.loadModule(new File("/data/app/~~xxxx/com.example.module-xxxx/base.apk"),
        librarySearchPath, false, moduleInfo);
AlterXposed.onPackageLoad(packageName, processName, appInfo, isFirstApplication, classLoader);
```

A loaded module can be hot reloaded, which needs API 102. Pass a class loader over the updated APK
to swap in new code, or null to rebuild the entries from the loader the module was read with:

```java
ModernModuleLoader.reload(modulePath, extras, replacementLoader);
```

Modules are found through `assets/xposed_init` (legacy) and through
`META-INF/xposed/java_init.list` with `module.prop` and `scope.list` (libxposed). A module that
ships both is started through both paths, like on other frameworks, and has to avoid hooking twice
by itself. `XposedBridge.getXposedVersion()` returns 93, `getApiVersion()` returns 102.

Resource hooking works by hooking the public `Resources` getters and matching each requested id by
its `package/type/entry` name, so no `Resources` instance of the host is replaced and no hidden
member is called. A package is offered to modules the first time one of its resources is read.

Works:

+ method and constructor hooking, `XposedHelpers`, `XC_MethodReplacement`, priorities, unhook
+ skipping the original constructor body, when the callback asks for it
+ `handleInitPackageResources`, `XResources.setReplacement` and `XC_LayoutInflated`
+ the libxposed chain API: `hook().intercept()`, `Chain`, exception modes, `detach()`, hook ids,
  `replaceHook()`, hot reload, `deoptimize()`, `getInvoker()`
+ `XSharedPreferences`, read only

Does not work:

+ zygote and system_server injection. `initZygote` runs when the module is loaded instead, and
  `onSystemServerStarting` is never called
+ theme attributes and `TypedArray` styling. `XResources` is also not a `Resources` subclass, since
  such a subclass cannot be compiled against `android.jar`
+ a replacement given as a resource id of another package. Register the object itself instead
+ `hookClassInitializer`, remote preferences and remote files (there is no manager process),
  `initCmdApp`, `XSharedPreferences.edit()`

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
