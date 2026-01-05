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

# 🔐 DoubleEncrypt512 Proguard Rules

# 1. Keep the Security-Crypto library intact
-keep class androidx.security.crypto.** { *; }
-keep interface androidx.security.crypto.** { *; }

# 2. Keep the DocumentFile library (used for Scoped Storage)
-keep class androidx.documentfile.** { *; }

# 3. Protect your own encryption logic from being over-optimized
-keep class com.lazycoder.doubleencrypt512.CryptoEngine { *; }
-keep class com.lazycoder.doubleencrypt512.VaultStorage { *; }

# 4. General Android optimization rules
-dontwarn androidx.security.crypto.**
-keepattributes *Annotation*, Signature, InnerClasses