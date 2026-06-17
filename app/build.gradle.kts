plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

// ------------------ Avoid conflicts of dependencies ------------------
configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}
// ---------------------------------------------------------------------

android {
    namespace = "com.example.esp32_mpu6050_mobile_data_collection"
    compileSdk = 36
    buildToolsVersion = "35.0.0"
    ndkVersion = "27.0.12077973"

    defaultConfig {

        // ------------------ FOR CPP ------------------
        externalNativeBuild.cmake {
            cppFlags += listOf("-std=c++17", "-frtti", "-fexceptions")
            val glVersion = project.findProperty("gl.version") ?: "ES20"
            arguments(
                "-DPLATFORM=Android",
                "-DBUILD_EXAMPLES=OFF",
                "-DAPP_LIB_NAME=raymob",
                "-DGL_VERSION=$glVersion",
            )
        }
        // ---------------------------------------------

        applicationId = "com.example.esp32_mpu6050_mobile_data_collection"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // ------------------ Build Time Constants ------------------
        // Generates a build configuration based on the requested features in gradle.properties
        buildConfigField("boolean", "FEATURE_DISPLAY_KEEP_ON",
            (project.properties["display.keep_on"] ?: "false") as String
        )
        buildConfigField("boolean", "FEATURE_DISPLAY_IMMERSIVE",
            (project.properties["display.immersive"] ?: "false") as String
        )
        buildConfigField("boolean", "FEATURE_DISPLAY_INTO_CUTOUT",
            (project.properties["display.into_cutout"] ?: "false") as String
        )
        // ----------------------------------------------------------

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ------------------ FOR CPP ------------------
    externalNativeBuild.cmake.path = file("src/main/cpp/CMakeLists.txt")
    externalNativeBuild.cmake.version = "3.30.3"
    // ---------------------------------------------

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
        buildConfig = true // Enables Custom Build used to pass in build time constants
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // ------------------ My Dependencies ------------------
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation(libs.androidx.navigation.compose.android)
    //Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // -----------------------------------------------------

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
