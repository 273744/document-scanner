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
# ML Kit ProGuard Rules
# ====================================
# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep ML Kit Vision classes
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.android.gms.vision.**

# Keep ML Kit Document Scanner
-keep class com.google.android.gms.mlkit.** { *; }
-dontwarn com.google.android.gms.mlkit.**

# Keep Google Play Services classes (required by ML Kit and ARCore)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep ML Kit models
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

