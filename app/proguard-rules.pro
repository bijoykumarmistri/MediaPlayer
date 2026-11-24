# Proguard rules for BKM Player

# libmpv / JNI bridges
-keep class dev.jdtech.mpv.MPVLib { *; }
-keep class dev.jdtech.mpv.MPVLib$* { *; }
-keep class is.xyz.mpv.MPVLib { *; }
-keep class is.xyz.mpv.MPVLib$* { *; }

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin Coroutines & Reflection
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Data Models
-keep class com.mediaplayer.data.** { *; }
-keep class com.mediaplayer.vault.** { *; }
