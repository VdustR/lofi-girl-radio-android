# NewPipe Extractor uses Mozilla Rhino JS engine for YouTube player extraction
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**

# NewPipe Extractor
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Google Cast
-keep class com.google.android.gms.cast.** { *; }

# jsoup optional re2j dependency (used by NewPipe Extractor)
-dontwarn com.google.re2j.**
