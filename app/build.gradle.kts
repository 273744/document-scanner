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
            // TEMPORARY: Disabled minification to fix build
            // TODO: Re-enable after fixing ProGuard rules
            isMinifyEnabled = false
            isShrinkResources = false

            // Use aggressive ProGuard optimization
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
    
    // ================================
    // APK SPLITS - Reduce download size per device
    // NOTE: Commented out for now due to Kotlin DSL syntax issues
    // Uncomment and test separately after main build works
    // ================================
    /*
    splits {
        density {
            isEnable = true
            reset()
            include("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        }
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    */

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
    // ================================
    // CORE ANDROID (~5 MB)
    // ================================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity:1.9.0")

    // ================================
    // CAMERA (~2.5 MB) - REQUIRED
    // ================================
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    // REMOVED: camera-extensions (saves 2 MB)

    // ================================
    // AR FEATURES (~10 MB) - Optional
    // ================================
    implementation("com.google.ar:core:1.42.0")
    // REMOVED: arsceneview (saves 5 MB)

    // ================================
    // IMAGE PROCESSING
    // ================================
    // OpenCV - REQUIRED by AR classes
    implementation("org.opencv:opencv:4.9.0")

    // ML Kit Document Scanner
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation("com.google.mlkit:image-labeling:17.0.8")

    // ================================
    // OCR - All Languages
    // ================================
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    // Additional languages - REQUIRED by OCRTextRecognizer
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.0")
    implementation("com.google.mlkit:text-recognition-korean:16.0.0")

    // Language ID - REQUIRED by OCRTextRecognizer
    implementation("com.google.mlkit:language-id:17.0.5")

    implementation("com.google.mlkit:common:18.10.0")

    // ================================
    // PLAY SERVICES
    // ================================
    implementation("com.google.android.gms:play-services-base:18.3.0")

    // ================================
    // PDF - iText REQUIRED
    // ================================
    // iText - REQUIRED by PdfGenerator
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")
    implementation("com.itextpdf:kernel:7.2.5")
    implementation("com.itextpdf:io:7.2.5")

    // ================================
    // DATABASE (~2 MB)
    // ================================
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // ================================
    // CLOUD STORAGE
    // ================================
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Google Drive SDK - REQUIRED by CloudStorageManager
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    implementation("com.google.api-client:google-api-client-android:2.0.0")
    implementation("com.google.http-client:google-http-client-android:1.42.3")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.api-client:google-api-client-gson:2.0.0")

    implementation("com.dropbox.core:dropbox-core-sdk:6.1.0")

    // ================================
    // BACKGROUND TASKS (~2 MB)
    // ================================
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // REMOVED: work-runtime Java (redundant)
    // REMOVED: work-rxjava3 (saves 1 MB)
    androidTestImplementation("androidx.work:work-testing:2.9.0")

    // ================================
    // NETWORKING (~2 MB)
    // ================================
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // REMOVED: RxJava adapter (saves 1 MB)
    // REMOVED: Scalars converter (saves 100 KB)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ================================
    // JSON & COROUTINES (~3 MB)
    // ================================
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ================================
    // COMPOSE (~8 MB) - TEMPORARY
    // Re-enabled to fix build errors
    // TODO: Delete theme files and remove these
    // ================================
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // ================================
    // TESTING
    // ================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}