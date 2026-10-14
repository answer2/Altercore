import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
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
        consumerProguardFiles("consumer-rules.pro")
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
    implementation(libs.panama.unsafe)
    implementation(libs.panama.llvm)

    implementation(libs.sun.cleaner)
    implementation(libs.r8.annotations)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)   // false 表示手动确认发布

    // 签名所有产物（需要 GPG）
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
        artifactId = "core",
        version = "1.1.4"
    )

    // pom information Central checks
    pom {
        name.set("AlterCore")
        description.set("A pure-Java Android ART hook library")
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

