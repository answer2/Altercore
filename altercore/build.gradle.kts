plugins {
    id("com.android.library")
}

android {
    namespace = "dev.answer.altercore"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
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
}

dependencies {
    implementation(project(":core-syscall"))
    implementation ("androidx.annotation:annotation:1.0.2")
    compileOnly(project(":hiddenapi-stub"))
}