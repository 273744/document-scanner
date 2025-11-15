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
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    // OpenCV for Android - Image processing and computer vision
    // Note: OpenCV requires manual SDK setup. Uncomment after adding OpenCV SDK module:
    // 1. Download OpenCV Android SDK from https://opencv.org/releases/
    // 2. Import as module in Android Studio
    // 3. Add dependency: implementation project(':opencv')
    // implementation("com.github.iamareebjamal:opencv-android:4.10.0")

    // iText PDF Library - PDF generation and manipulation
    // iText Core library for creating and editing PDFs
    implementation("com.itextpdf:itext7-core:7.2.5")
    // iText Layout module for advanced layout features
    implementation("com.itextpdf:layout:7.2.5")
    // iText Kernel module for low-level PDF operations
    implementation("com.itextpdf:kernel:7.2.5")
    // iText IO module for font and image handling
    implementation("com.itextpdf:io:7.2.5")

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