import java.io.File
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val signingPath = System.getenv("GHOST_WIFI_STORE_FILE")
val signingPass = System.getenv("GHOST_WIFI_STORE_PASSWORD")
val keyAliasEnv = System.getenv("GHOST_WIFI_KEY_ALIAS")
val keyPassEnv = System.getenv("GHOST_WIFI_KEY_PASSWORD")
val canSign = listOf(signingPath, signingPass, keyAliasEnv, keyPassEnv).all { !it.isNullOrBlank() } &&
    File(signingPath ?: "").isFile

android {
    namespace = "com.ghostdeveloper.ghostwifiauditor"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ghostdeveloper.ghostwifiauditor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        externalNativeBuild { cmake { cppFlags += "-std=c++17" } }
    }
    signingConfigs {
        create("persistent") {
            if (canSign) {
                storeFile = file(signingPath!!)
                storePassword = signingPass!!
                keyAlias = keyAliasEnv!!
                keyPassword = keyPassEnv!!
            }
        }
    }
    buildTypes {
        getByName("debug") { isMinifyEnabled = false }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            if (canSign) signingConfig = signingConfigs.getByName("persistent")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
