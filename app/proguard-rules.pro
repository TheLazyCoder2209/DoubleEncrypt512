# 🔐 DoubleEncrypt512 - Final Hardened Rules

# 1. Fix for 'Missing class' errors (Tink's optional dependencies)
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.api.client.http.**
-dontwarn com.google.api.client.http.javanet.**
-dontwarn org.joda.time.Instant

# 2. Keep the Security-Crypto & Tink engine intact
-keep class androidx.security.crypto.** { *; }
-keep interface androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-keep interface com.google.crypto.tink.** { *; }

# 3. Keep DocumentFile for Scoped Storage access
-keep class androidx.documentfile.** { *; }

# 4. Protect your specific logic from being stripped or renamed
-keep class com.lazycoder.doubleencrypt512.CryptoEngine { *; }
-keep class com.lazycoder.doubleencrypt512.VaultStorage { *; }

# 5. General Optimization & Metadata
-dontwarn androidx.security.crypto.**
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod