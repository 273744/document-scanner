plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.myapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // API Keys - Store in local.properties for security
            // Add to local.properties: GOOGLE_CLIENT_ID=your_client_id_here
            buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${project.findProperty("GOOGLE_CLIENT_ID") ?: ""}\"")
            buildConfigField("String", "DROPBOX_APP_KEY", "\"${project.findProperty("DROPBOX_APP_KEY") ?: ""}\"")
            buildConfigField("String", "DROPBOX_APP_SECRET", "\"${project.findProperty("DROPBOX_APP_SECRET") ?: ""}\"")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // API Keys for release build
            buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${project.findProperty("GOOGLE_CLIENT_ID") ?: ""}\"")
            buildConfigField("String", "DROPBOX_APP_KEY", "\"${project.findProperty("DROPBOX_APP_KEY") ?: ""}\"")
            buildConfigField("String", "DROPBOX_APP_SECRET", "\"${project.findProperty("DROPBOX_APP_SECRET") ?: ""}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Traditional Android View dependencies
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity:1.9.0")

    // CameraX dependencies for document capture
    // CameraX core library - provides core camera functionality
    implementation("androidx.camera:camera-core:1.3.1")
    // CameraX Camera2 implementation - provides Camera2 API support
    implementation("androidx.camera:camera-camera2:1.3.1")
    // CameraX Lifecycle - binds camera to lifecycle-aware components
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    // CameraX View - provides PreviewView for camera preview
    implementation("androidx.camera:camera-view:1.3.1")
    // CameraX Extensions - optional extensions like HDR, Night mode, etc.
    implementation("androidx.camera:camera-extensions:1.3.1")

    // ARCore - Augmented Reality for document scanning and 3D placement
    // ARCore provides environmental understanding and motion tracking
    implementation("com.google.ar:core:1.42.0")
    // ARCore Extensions for SceneView (optional, for easier AR implementation)
    implementation("io.github.sceneview:arsceneview:2.0.4")

    // OpenCV for Android - Advanced image processing and computer vision
    // OpenCV provides edge detection, perspective correction, and document analysis
    // Using Maven Central version for easier setup (no manual SDK needed)
    implementation("org.opencv:opencv:4.9.0")

    // Alternative OpenCV (if above doesn't work, uncomment below):
    // implementation("com.quickbirdstudios:opencv:4.5.3.0")

    // Manual OpenCV SDK setup (if you prefer native performance):
    // 1. Download OpenCV Android SDK from https://opencv.org/releases/
    // 2. Import as module: File → New → Import Module → select OpenCV-android-sdk/sdk
    // 3. Add dependency: implementation project(':opencv')

    // ================================
    // ML Kit - Text Recognition & OCR
    // ================================

    // ML Kit Text Recognition V2 - Latin script (English)
    // Provides on-device text recognition for Latin-based languages
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    // ML Kit Text Recognition - Devanagari script (Hindi, Marathi, Nepali)
    // Supports Hindi and other Devanagari-based languages
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.0")

    // ML Kit Text Recognition - Chinese (Simplified and Traditional)
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")

    // ML Kit Text Recognition - Japanese
    implementation("com.google.mlkit:text-recognition-japanese:16.0.0")

    // ML Kit Text Recognition - Korean
    implementation("com.google.mlkit:text-recognition-korean:16.0.0")

    // ML Kit Language Identification
    // Automatically detect language of recognized text
    implementation("com.google.mlkit:language-id:17.0.5")

    // ML Kit Translation (optional, for translating recognized text)
    implementation("com.google.mlkit:translate:17.0.2")

    // ML Kit Document Scanner - Pre-built document scanning solution
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    // ML Kit Barcode Scanning (optional, for QR codes on documents)
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // ML Kit Image Labeling (optional, for document classification)
    implementation("com.google.mlkit:image-labeling:17.0.8")

    // ML Kit Common - Required for ML Kit components
    implementation("com.google.mlkit:common:18.10.0")

    // CameraX ML Kit Vision integration
    // Provides seamless integration between CameraX and ML Kit
    implementation("androidx.camera:camera-mlkit-vision:1.3.0-alpha03")

    // OpenGL ES - For 3D rendering and AR visualization
    // OpenGL ES is already included in the Android SDK
    // No additional dependency needed - use android.opengl package directly

    // TensorFlow Lite - For custom ML models (optional, advanced use)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Google Play Services - Required for ARCore and ML Kit
    implementation("com.google.android.gms:play-services-vision:20.1.3")
    implementation("com.google.android.gms:play-services-base:18.3.0")

    // iText PDF Library - PDF generation and manipulation
    // iText Core library for creating and editing PDFs
    implementation("com.itextpdf:itext7-core:7.2.5")
    // iText Layout module for advanced layout features
    implementation("com.itextpdf:layout:7.2.5")
    // iText Kernel module for low-level PDF operations
    implementation("com.itextpdf:kernel:7.2.5")
    // iText IO module for font and image handling
    implementation("com.itextpdf:io:7.2.5")

    // Room Database - Local database persistence
    // Room runtime library
    implementation("androidx.room:room-runtime:2.6.1")
    // Room annotation processor (kapt for Kotlin)
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    // Room Kotlin extensions (optional but recommended)
    implementation("androidx.room:room-ktx:2.6.1")

    // ================================
    // Cloud Storage - Google Drive & Dropbox
    // ================================

    // Google Drive API - For Google Drive integration
    // Google Drive REST API v3 (stable version)
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    // Google API Client for Android
    implementation("com.google.api-client:google-api-client-android:2.0.0")
    // Google HTTP Client for Android
    implementation("com.google.http-client:google-http-client-android:1.42.3")
    // Google OAuth Client
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    // Google API Client Gson
    implementation("com.google.api-client:google-api-client-gson:2.0.0")

    // Google Sign-In - Authentication for Google Drive
    // Google Sign-In SDK
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    // Google Identity
    implementation("com.google.android.gms:play-services-identity:18.0.1")

    // Dropbox API SDK - For Dropbox integration
    // Dropbox Core SDK
    implementation("com.dropbox.core:dropbox-core-sdk:6.1.0")
    // Dropbox Android SDK (includes UI components)
    implementation("com.dropbox.core:dropbox-android-sdk:6.1.0")

    // WorkManager - Background sync for cloud storage
    // WorkManager runtime
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // WorkManager for Java
    implementation("androidx.work:work-runtime:2.9.0")
    // WorkManager RxJava3 support (optional)
    implementation("androidx.work:work-rxjava3:2.9.0")
    // WorkManager testing
    androidTestImplementation("androidx.work:work-testing:2.9.0")

    // Retrofit - REST API client for cloud services
    // Retrofit core
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Retrofit Gson converter
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Retrofit RxJava adapter (optional)
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")
    // Retrofit scalars converter (for plain text responses)
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    // OkHttp - HTTP client (used by Retrofit)
    // OkHttp core
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // OkHttp logging interceptor (for debugging)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson - JSON serialization/deserialization
    // Gson core library
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines - For asynchronous operations
    // Coroutines core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // Coroutines Android
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Coroutines Play Services (for Google APIs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Compose dependencies (keeping for compatibility)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    // Accompanist permissions for Compose
    implementation("com.google.accompanist:accompanist-permissions:0.37.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}