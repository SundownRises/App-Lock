# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep PIN repository classes for reflection
-keep class com.anika.applock.domain.** { *; }
-keep class com.anika.applock.platform.** { *; }

# Keep service classes
-keep class com.anika.applock.service.** { *; }
-keep class com.anika.applock.admin.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Compose
-keep class androidx.compose.** { *; }
