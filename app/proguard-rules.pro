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

# iText PDF Library ProGuard Rules
# Keep all iText classes to prevent PDF generation errors
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Keep iText annotations
-keepattributes *Annotation*

# Keep iText bouncy castle classes
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep SLF4J classes (used by iText)
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# Prevent obfuscation of classes used by iText via reflection
-keepclassmembers class * {
    @com.itextpdf.kernel.pdf.* <methods>;
}

# Keep native methods for iText
-keepclasseswithmembernames class * {
    native <methods>;
}

# ====================================
# ARCore ProGuard Rules
# ====================================
# Keep ARCore classes
-keep class com.google.ar.** { *; }
-dontwarn com.google.ar.**

# Keep ARCore native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep ARCore Sceneform/SceneView classes
-keep class io.github.sceneview.** { *; }
-dontwarn io.github.sceneview.**

# ====================================
# OpenCV ProGuard Rules
# ====================================
# Keep all OpenCV classes
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Keep OpenCV native methods
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep OpenCV JNI classes
-keep class org.opencv.core.Mat { *; }
-keep class org.opencv.imgproc.Imgproc { *; }
-keep class org.opencv.imgcodecs.Imgcodecs { *; }

# ====================================
# ML Kit Text Recognition ProGuard Rules
# ====================================

# Keep all ML Kit classes
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep ML Kit Vision classes
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.android.gms.vision.**

# Keep ML Kit Document Scanner
-keep class com.google.android.gms.mlkit.** { *; }
-dontwarn com.google.android.gms.mlkit.**

# Keep ML Kit Text Recognition classes
-keep class com.google.mlkit.vision.text.** { *; }
-keep interface com.google.mlkit.vision.text.** { *; }

# Keep ML Kit Text Recognition Latin
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_text.**

# Keep ML Kit Text Recognition Devanagari (Hindi)
-keep class com.google.android.gms.internal.mlkit_vision_text_devanagari.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_text_devanagari.**

# Keep ML Kit Text Recognition Chinese
-keep class com.google.android.gms.internal.mlkit_vision_text_chinese.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_text_chinese.**

# Keep ML Kit Text Recognition Japanese
-keep class com.google.android.gms.internal.mlkit_vision_text_japanese.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_text_japanese.**

# Keep ML Kit Text Recognition Korean
-keep class com.google.android.gms.internal.mlkit_vision_text_korean.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_text_korean.**

# Keep ML Kit Language Identification
-keep class com.google.mlkit.nl.languageid.** { *; }
-keep class com.google.android.gms.internal.mlkit_language_id.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_language_id.**

# Keep ML Kit Translation
-keep class com.google.mlkit.nl.translate.** { *; }
-keep class com.google.android.gms.internal.mlkit_translate.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_translate.**

# Keep ML Kit Common
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_common.**

# Keep Google Play Services classes (required by ML Kit and ARCore)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep ML Kit native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep ML Kit model interfaces
-keep interface com.google.mlkit.vision.interfaces.** { *; }

# Keep ML Kit tasks and callbacks
-keep class com.google.android.gms.tasks.** { *; }
-keep interface com.google.android.gms.tasks.** { *; }

# Keep ML Kit result classes
-keepclassmembers class * {
    @com.google.mlkit.common.** *;
}

# Prevent obfuscation of ML Kit annotation processors
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep ML Kit TensorFlow Lite models
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep AutoValue classes used by ML Kit
-keep class com.google.auto.value.** { *; }
-dontwarn com.google.auto.value.**

# Keep ML Kit models and metadata

# ====================================
# Cloud Storage ProGuard Rules
# ====================================

# Google Drive API
-keep class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.services.drive.**

# Google API Client
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

# Google HTTP Client
-keep class com.google.http.client.** { *; }
-dontwarn com.google.http.client.**

# Google OAuth Client
-keep class com.google.oauth.client.** { *; }
-dontwarn com.google.oauth.client.**

# Google Sign-In
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**

# Dropbox SDK
-keep class com.dropbox.core.** { *; }
-dontwarn com.dropbox.core.**
-keep class com.dropbox.android.** { *; }
-dontwarn com.dropbox.android.**

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep interface okhttp3.** { *; }
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keepattributes Signature
-keepattributes *Annotation*

# Gson serialization/deserialization
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data classes for Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.mlkit.common.** { *; }

# ====================================
# TensorFlow Lite ProGuard Rules
# ====================================
# Keep TensorFlow Lite classes
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# Keep TensorFlow Lite native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep TensorFlow Lite GPU delegate
-keep class org.tensorflow.lite.gpu.** { *; }

# ====================================
# OpenGL ES ProGuard Rules
# ====================================
# Keep OpenGL classes
-keep class javax.microedition.khronos.** { *; }
-keep class android.opengl.** { *; }

# Keep OpenGL native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ====================================
# General Rules for AR and Computer Vision
# ====================================
# Keep attributes for AR and vision libraries
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep serialization classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Suppress warnings for missing classes in libraries
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

