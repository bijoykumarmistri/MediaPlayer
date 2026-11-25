# Proguard rules for BKM Player

# libmpv / JNI bridges (video playback & zero-buffering timeline scrubbing)
-keep class dev.jdtech.mpv.MPVLib { *; }
-keep class dev.jdtech.mpv.MPVLib$* { *; }
-keep class is.xyz.mpv.MPVLib { *; }
-keep class is.xyz.mpv.MPVLib$* { *; }

# Media3 FFmpeg audio decoder extension (loaded via reflection by DefaultRenderersFactory)
-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keep class org.jellyfin.media3.ffmpeg.** { *; }
-dontwarn androidx.media3.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
