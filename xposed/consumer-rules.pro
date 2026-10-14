# Modules are APKs loaded at runtime, so their references to the framework API are by name and the
# host app's R8 cannot see them. Renaming or stripping any of these breaks every module at once.

-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }

# The replacement handle shipped under the upstream package name, and the libxposed API classes a
# module implements or resolves by name.
-keep class android.content.res.XResources { *; }
-keep class android.content.res.XResources$** { *; }
-keep class io.github.libxposed.api.** { *; }
-keep interface io.github.libxposed.api.** { *; }

# Entry point the host app calls to load modules.
-keep class dev.answer.altercore.xposed.AlterXposed { *; }

-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, Exceptions
