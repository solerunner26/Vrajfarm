# Vraj Kheti Hisab release rules.
# Room, WorkManager, Glance and Compose ship their own consumer rules.

# Keep Room entities' field names stable for the JSON backup (doc 15).
-keep class com.vraj.khetihisab.data.entity.** { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Tink (security-crypto)
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
