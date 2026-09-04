import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release signing credentials, kept out of Git (Publisher.md §2.5 / §4.1).
// keystore.properties lives at the repo root and is gitignored; the .jks it points at
// lives outside the repo entirely. When the file is absent — a fresh clone, CI, anyone
// but the release engineer — the release build stays unsigned rather than failing, so
// `assembleRelease` still works for size and R8 checks.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasSigningConfig = keystoreProps.isNotEmpty()

android {
    namespace = "com.dicereligion.edgecase"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.dicereligion.edgecase"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                // Play requires v2+; v1 is dead weight at minSdk 30.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        // Ad IDs are resolved per build type so a debug build can NEVER reach a live ad unit
        // (Docs/Ads.md §7.2). Self-clicks on a production unit are the top cause of account-level
        // enforcement, and enforcement lands on the publisher ID — which is shared with the
        // sibling apps. These IDs are not secrets: they ship inside every APK.
        debug {
            // Google's official test IDs. Not tied to any account.
            // …/9214589741 is the ANCHORED ADAPTIVE banner test unit — not the fixed-size one.
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")
            resValue("string", "admob_banner_unit", "ca-app-pub-3940256099942544/9214589741")
        }
        release {
            if (hasSigningConfig) signingConfig = signingConfigs.getByName("release")
            // R8 full mode: shrink, optimise, obfuscate, then drop unreferenced resources.
            // Both ad AARs ship their own consumer proguard.txt, so the SDKs need no rules here.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Dice Religion publisher pub-4587702028307036 — "EdgeCase — Plinth Banner".
            resValue("string", "admob_app_id", "ca-app-pub-4587702028307036~3708305513")
            resValue("string", "admob_banner_unit", "ca-app-pub-4587702028307036/8470994251")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        // AGP 9 disables resValue by default, like buildConfig. The per-build-type AdMob IDs
        // above are resValues, so this must stay on (Docs/Ads.md §7.2).
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.user.messaging.platform)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}