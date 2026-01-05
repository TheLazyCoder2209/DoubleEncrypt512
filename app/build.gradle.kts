plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.lazycoder.doubleencrypt512"
    // Use a standard integer for compileSdk (34 or 35 are stable for 2026)
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lazycoder.doubleencrypt512"
        minSdk = 31    // Android 12
        targetSdk = 35 // Android 14/15
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Set this to true to protect your encryption logic from being easily read
            isMinifyEnabled = true
            isShrinkResources = true
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // Required for the DocumentFile and Scoped Storage logic we wrote
    implementation("androidx.documentfile:documentfile:1.0.1")

    // The security library for EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}