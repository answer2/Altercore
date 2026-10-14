import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "dev.answer.altercore.xposed"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    // Framework classes the module loader compiles against but the Android runtime provides.
    compileOnly(project(":stubs"))
    api(project(":altercore"))

    // AlterCore is the runtime provider of the modern API classes for the modules it loads, so
    // this is a real dependency rather than a compile-only one: modules declare it as
    // compileOnly themselves and resolve io.github.libxposed.* against this framework's loader.
    api("io.github.libxposed:api:102.0.0")
}


mavenPublishing {
    publishToMavenCentral(automaticRelease = false)   // false: release it by hand on the portal

    // sign everything
    signAllPublications()

    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            javadocJar = JavadocJar.Empty(),   // Central only needs the artifact to be there
            sourcesJar = SourcesJar.Sources()
        )
    )

    // coordinates
    coordinates(
        groupId = "io.github.answer2.altercore",
        artifactId = "xposed",
        version = "0.0.3"
    )

    // pom information Central checks
    pom {
        name.set("AlterCore")
        description.set("A pure-Java Android ART hook Xposed style library")
        inceptionYear.set("2026")
        url.set("https://github.com/answer2/Altercore")

        licenses {
            license {
                name.set("LGPL-3.0 License")
                url.set("https://opensource.org/license/lgpl-3-0")
            }
        }

        developers {
            developer {
                id.set("answer2")
                name.set("AnswerDev")
                email.set("nswera929@gmail.com")
                url.set("https://github.com/answer2")
            }
        }

        scm {
            url.set("https://github.com/answer2/Altercore")
            connection.set("scm:git:git://github.com/answer2/Altercore.git")
            developerConnection.set("scm:git:ssh://git@github.com/answer2/Altercore.git")
        }
    }
}
