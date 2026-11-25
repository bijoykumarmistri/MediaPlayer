# Keep JNI bridge methods and observer callbacks for libmpv
-keep class dev.jdtech.mpv.MPVLib { *; }
-keep class dev.jdtech.mpv.MPVLib$* { *; }
-keep class is.xyz.mpv.MPVLib { *; }
-keep class is.xyz.mpv.MPVLib$* { *; }
-keepclassmembers class * implements dev.jdtech.mpv.MPVLib$EventObserver {
    <methods>;
}
-keepclassmembers class * implements dev.jdtech.mpv.MPVLib$LogObserver {
    <methods>;
}
