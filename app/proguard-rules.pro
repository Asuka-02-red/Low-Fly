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

# Retrofit / Gson
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.google.gson.** { *; }
-keep class com.example.low_altitudereststop.core.model.** { *; }
-keep class com.example.low_altitudereststop.feature.ai.model.** { *; }

# AIDL / Binder / Accessibility
-keep class com.example.low_altitudereststop.ai.** { *; }
-keep class com.example.low_altitudereststop.feature.ai.service.** { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.fragment.app.Fragment { *; }
-keepnames class * extends android.app.Activity
-keepnames class * extends androidx.fragment.app.Fragment

# Speech / TTS
-keep class android.speech.** { *; }
-keep class android.speech.tts.** { *; }

# AMap 3D Map SDK
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-keep class com.amap.api.maps.** { *; }
-keep class com.amap.api.mapcore.** { *; }
-keep class com.amap.api.services.** { *; }
# AMap optional location classes referenced by 3D map internals
-dontwarn com.amap.api.location.**

# WorkManager / Room generated implementations
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class **_Impl { *; }
