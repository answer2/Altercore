@file:Suppress("UnstableApiUsage")
plugins {
    id("com.android.library")
}

android {
    namespace = "dev.tmpfs.libcoresyscall.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("proguard-rules.pro")
    }

    buildFeatures {
        viewBinding = false
        buildConfig = false
        resValues = false
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    buildToolsVersion = "36.0.0"

}
dependencies {
    compileOnly(project(":hiddenapi-stub"))
    compileOnly(libs.androidx.annotation)
}
