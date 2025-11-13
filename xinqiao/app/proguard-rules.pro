-keep public class androidx.core.app.CoreComponentFactory { *; }
-keep public class * extends androidx.core.app.CoreComponentFactory { *; }
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Compose runtime & UI: prevent incorrect optimizations that may break lock verification ---
# Keep Compose runtime classes to avoid aggressive optimizations around synchronized blocks.
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# If you observe lock verification warnings in release builds with the optimized default file,
# you can additionally restrict optimizations that sometimes impact monitor enter/exit handling.
# Uncomment the next line to disable code-level optimizations globally (safer but larger apk):
# -dontoptimize
# Or selectively disable a few known risky optimization groups:
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!class/merging/*,!field/*
