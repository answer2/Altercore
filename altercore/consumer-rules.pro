# AlterCore is used by code that the app's R8 never sees: hooks installed from a module APK that is
# loaded at runtime, and generated trampolines that resolve classes by name. Keep the public surface
# under those names.

-keep class dev.answer.altercore.AlterCore { *; }
-keep class dev.answer.altercore.AlterHelper { *; }
-keep class dev.answer.altercore.AlterConfig { *; }
-keep class dev.answer.altercore.HookInfo { *; }
-keep class dev.answer.altercore.callback.** { *; }
-keep class dev.answer.altercore.core.HookParams { *; }
-keep class dev.answer.altercore.core.HookRecord { *; }
-keep class dev.answer.altercore.exception.** { *; }
-keep class dev.answer.altercore.monitoring.** { *; }

# Callbacks a host app writes are subclassed, so their overrides must survive.
-keepclassmembers class * extends dev.answer.altercore.callback.MethodHook {
    public void before(...);
    public void after(...);
}

-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, Exceptions
