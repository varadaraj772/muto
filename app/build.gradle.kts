plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.varadaraj772.muto"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.varadaraj772.muto"
        minSdk = 26 // Bumped to 26 to avoid needing legacy backward-compatibility bloat
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Removed the test runner since we aren't writing UI tests
    }

    buildTypes {
        release {
            isMinifyEnabled = true     // CHANGED TO TRUE: This strips out unused code
            isShrinkResources = true   // ADDED: This deletes unused files/icons
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
}

dependencies {
    // This is the ONLY dependency you need to run background services.
    implementation(libs.androidx.core.ktx)

    // 🛑 ALL OF THESE HAVE BEEN REMOVED to save ~3 Megabytes:
    // implementation(libs.androidx.appcompat)
    // implementation(libs.material)
    // testImplementation(libs.junit)
    // androidTestImplementation(libs.androidx.espresso.core)
    // androidTestImplementation(libs.androidx.junit)
}