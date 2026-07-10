plugins {
    id("com.android.library")
}

android {
    namespace = "dev.answer.altercore"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }


    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {


    api(libs.panama.core)
    api(libs.panama.unsafe)
    api(libs.panama.llvm)

    implementation(libs.sun.cleaner)
    implementation(libs.r8.annotations)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}