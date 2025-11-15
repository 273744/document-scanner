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
