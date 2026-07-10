# AlterCore

![](https://img.shields.io/badge/license-LGPL--3.0-orange.svg)
![](https://img.shields.io/badge/Android-8.0%20--%2017-blue.svg)
![](https://img.shields.io/badge/arch-armeabi--v7a%20%7C%20arm64--v8a%20%7C%20x86%20%7C%20x86--64%20%7C%20riscv64-brightgreen.svg)
![](https://img.shields.io/badge/Mode-Debug-red.svg)

AlterCore is a pure-Java Android ART hook library, requiring no native code.

Please note that this project is currently under active development – its API and behavior are subject to change. It is not yet recommended for production use.

## Features

+ Support Android 5.0 - 17 (API level 21 - 37)
+ Support armeabi-v7a, arm64-v8a, x86, x86-64, riscv64

## Quick Start


### Before and After Hook
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

### Replace Hook
```java
AlterCore.hook(Activity.class.getDeclaredMethod("onCreate", Bundle.class), new MethodReplacement() {
    @Override
    protected Object replace(HookParams params) throws Throwable {
        return null;
    }
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
