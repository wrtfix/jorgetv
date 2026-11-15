plugins {
    id("com.android.application")
}

val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "cc.framextv.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "cc.framextv.tv"
        minSdk = 24
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.$buildNumber"
    }

    signingConfigs {
        create("release") {
            // Keystore fijo para que las actualizaciones se instalen encima sin desinstalar
            storeFile = file("release.keystore")
            storePassword = "framextv"
            keyAlias = "framextv"
            keyPassword = "framextv"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
