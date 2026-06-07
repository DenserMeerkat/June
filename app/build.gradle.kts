import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutLibraries)
}

val appName = "June"
val appId = "com.denser.june"
val appNamespace = "com.denser.june"
val apkNamePrefix = "june"

val versionMajor = 0
val versionMinor = 9
val versionPatch = 0
val appVersionCode = 11
val appVersionName = "$versionMajor.$versionMinor.$versionPatch"

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val fossStoreFile = System.getenv("RELEASE_STORE_FILE") ?: keystoreProperties["storeFile"] as String?
val fossStorePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: keystoreProperties["storePassword"] as String?
val fossKeyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: keystoreProperties["keyAlias"] as String?
val fossKeyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: keystoreProperties["keyPassword"] as String?

val playStoreFile = System.getenv("PLAY_RELEASE_STORE_FILE") ?: keystoreProperties["playStoreFile"] as String?
val playStorePassword = System.getenv("PLAY_RELEASE_STORE_PASSWORD") ?: keystoreProperties["playStorePassword"] as String?
val playKeyAlias = System.getenv("PLAY_RELEASE_KEY_ALIAS") ?: keystoreProperties["playKeyAlias"] as String?
val playKeyPassword = System.getenv("PLAY_RELEASE_KEY_PASSWORD") ?: keystoreProperties["playKeyPassword"] as String?

android {
    namespace = appNamespace
    compileSdk = 36

    defaultConfig {
        applicationId = appId
        minSdk = 28
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = fossKeyAlias
            keyPassword = fossKeyPassword
            storePassword = fossStorePassword
            storeFile = fossStoreFile?.let { file(it) }
        }
        create("playRelease") {
            keyAlias = playKeyAlias
            keyPassword = playKeyPassword
            storePassword = playStorePassword
            storeFile = playStoreFile?.let { file(it) }
        }
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("foss") {
            dimension = "distribution"
            signingConfig = signingConfigs.getByName("release")
        }
        create("play") {
            dimension = "distribution"
            applicationIdSuffix = ".play"
            versionNameSuffix = "-play"
            signingConfig = signingConfigs.getByName("playRelease")
        }
    }

    val isBuildingBundle = project.gradle.startParameter.taskNames.any { it.contains("Bundle", ignoreCase = true) }

    if (!isBuildingBundle) {
        applicationVariants.all {
            val variant = this
            variant.outputs.configureEach {
                val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                val abi = output.getFilter(com.android.build.OutputFile.ABI)
                if (abi != null) {
                    output.outputFileName = "$apkNamePrefix-${variant.versionName}-${abi}.apk"
                } else {
                    output.outputFileName = "$apkNamePrefix-${variant.versionName}-universal.apk"
                }
            }
        }
    }

    splits {
        abi {
            isEnable = !isBuildingBundle
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    kotlin {
        compilerOptions {
            jvmToolchain(17)
        }
    }

    buildTypes {
        release {
            resValue("string", "app_name", appName)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "$appName (Debug)")
            versionNameSuffix = "-dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}



dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.adaptive.layout)
    implementation(libs.androidx.material3.adaptive.navigation)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)


    // Essential
    implementation(project(":core"))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)
    implementation(libs.materialKolor)           
    implementation(libs.colorpicker.compose)     
    implementation(libs.wavy.slider)             
    implementation(libs.composeIcons.fontAwesome)
    implementation(libs.androidx.core.splashscreen) 
    implementation(libs.aboutLibraries)          
    implementation(libs.aboutLibraries.compose.m3)


    // June
    implementation(libs.maplibre.compose)
    implementation(libs.osmdroid)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.emoji2.emojipicker)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.hyphen)

    // Play
    "playImplementation"(libs.gms.location)
    "playImplementation"(libs.kotlinx.coroutines.play.services)
    "playImplementation"(libs.play.app.update)
}
aboutLibraries {
    export.excludeFields.add("generated")
    library {
        duplicationMode = DuplicateMode.MERGE
        duplicationRule = DuplicateRule.SIMPLE
    }
}