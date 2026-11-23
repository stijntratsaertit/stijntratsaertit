plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Builds the manifest, tile banners and assets from config/scenes.json.
apply(from = "scenes.gradle.kts")

android {
    namespace = "app.fireplace"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.fireplace"
        minSdk = 26
        targetSdk = 35
        // GitHub Actions sets GITHUB_RUN_NUMBER, so every build installs over the previous one.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0.$versionCode"
    }

    signingConfigs {
        // A fixed key, committed on purpose, so each new APK can be installed over the old one.
        // It only signs this personal sideloaded app; it protects nothing else.
        create("sideload") {
            storeFile = rootProject.file("sideload.keystore")
            storePassword = "fireplace"
            keyAlias = "fireplace"
            keyPassword = "fireplace"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    sourceSets["main"].apply {
        manifest.srcFile(project.extra["scenesManifest"] as File)
        res.srcDir(project.extra["scenesRes"] as File)
        assets.srcDir(project.extra["scenesAssets"] as File)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
