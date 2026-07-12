import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish") version "0.36.0"
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    compileOnly( project(":altercore"))
}


mavenPublishing {
    publishToMavenCentral(automaticRelease = false)   // false 表示手动确认发布

    // 签名所有产物（需要 GPG）
    signAllPublications()

    configure(
        AndroidMultiVariantLibrary(
            javadocJar = JavadocJar.Empty(),   // 不需要 Javadoc（或根据需要改为 Dokka）
            sourcesJar = SourcesJar.Sources()  // 打包源码
        )
    )

    // 坐标
    coordinates(
        groupId = "io.github.answer2.altercore",
        artifactId = "xposed",
        version = "0.0.2"
    )

    // POM 信息（必须填写完整）
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
