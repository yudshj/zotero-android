
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.github.triplet.play") version "3.7.0"
    id("dagger.hilt.android.plugin")
    id("realm-android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("compose")
    id("accompanist")
    id("daggerAndHilt")
    id("tests")
    id("com.google.dagger.hilt.android")
}

android {
    compileSdk = BuildConfig.compileSdkVersion
    namespace = "org.zotero.android"

    defaultConfig {
        applicationId = BuildConfig.appId
        minSdk = BuildConfig.minSdkVersion
        targetSdk = BuildConfig.targetSdk
        versionCode = BuildConfig.versionCode
        versionName = BuildConfig.version.name
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_API_URL", "\"https://api.zotero.org\"")
        buildConfigField("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "false")
        buildConfigField("String", "PSPDFKIT_KEY", "\"\"")
        manifestPlaceholders["enableCrashReporting"] = false

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("zotero.release.keystore")

            if (rootProject.file("keystore-secrets.txt").exists()) {
                val secrets: List<String> = rootProject
                    .file("keystore-secrets.txt")
                    .readLines()
                keyAlias = secrets[0]
                storePassword = secrets[1]
                keyPassword = secrets[2]
            }
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    androidResources {
        noCompress ("ttf", "mov", "avi", "json", "html", "csv", "obb")
    }
    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfigs
                .findByName("debug")
                ?.storeFile = rootProject.file("debug.keystore")

            buildConfigField("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "false")
            manifestPlaceholders["enableCrashReporting"] = false
            extra.set("enableCrashlytics", false)
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getAt("release")

            buildConfigField("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "true")
            manifestPlaceholders["enableCrashReporting"] = true
        }
    }
    setDefaultProductFlavors()
    productFlavors {
        dev {
            resValue("string", "app_name", """"Zotero Debug""")
            buildConfigField("String", "PSPDFKIT_KEY", readPspdfkitKey())
            applicationIdSuffix = ".debug"
        }
        internal {
            resValue("string", "app_name", """"Zotero Beta""")
            buildConfigField("String", "PSPDFKIT_KEY", readPspdfkitKey())
        }
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packagingOptions {
        resources.pickFirsts.add("META-INF/kotlinx-coroutines-core.kotlin_module")
    }

    androidComponents {
        beforeVariants { it.ignoreUnusedVariants() }
//        onVariants {
//            it.outputs.forEach { output ->
//                val newVersionName = "${BuildConfig.version.name}-${it.flavorName}.${gitLastCommitHash()}"
//                output.versionName.set(newVersionName)
//            }
//        }
    }
}

play {
    track.set("internal")
    defaultToAppBundles.set(true)
    resolutionStrategy.set(ResolutionStrategy.AUTO)
}

dependencies {

    //AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.6.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity-ktx:1.8.1")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.constraintlayout:constraintlayout-solver:2.0.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    //Material design
    implementation("com.google.android.material:material:1.10.0")

    //Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.9.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")

    //Crash
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.5.1")

    //PSPDFKIT
    implementation("com.pspdfkit:pspdfkit:8.10.0")

    //Retrofit 2
    implementation(Libs.Retrofit.kotlinSerialization)
    implementation(Libs.Retrofit.core)
    implementation(Libs.Retrofit.converterGson)
    implementation(Libs.Retrofit.converterScalars)

    //Ok HTTP
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    //GSON
    implementation("com.google.code.gson:gson:2.8.9")

    //ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")

    //Coil
    implementation(Libs.Coil.compose)

    //Other
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("joda-time:joda-time:2.10.9")
    implementation("org.greenrobot:eventbus:3.2.0")

    implementation("commons-io:commons-io:2.4")
    implementation("commons-codec:commons-codec:1.13")
    implementation("commons-validator:commons-validator:1.7")
    implementation("net.yslibrary.keyboardvisibilityevent:keyboardvisibilityevent:3.0.0-RC3")
}

kapt {
    correctErrorTypes = true
}

fun gitLastCommitHash(): String {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = "git rev-parse --verify --short HEAD".split(" ")
            standardOutput = byteOut
        }
        String(byteOut.toByteArray()).trim().also {
            if (it == "HEAD")
                logger.warn("Unable to determine current branch: Project is checked out with detached head!")
        }
    } catch (e: Exception) {
        logger.warn("Unable to determine current branch: ${e.message}")
        "Unknown Branch"
    }
}

fun readPspdfkitKey() : String {
    val file = rootProject
        .file("pspdfkit-key.txt")
    if (!file.exists()) {
        logger.warn("pspdfkit-key.txt file not found. Using PSPDFKit without a key")
        return "\"\""
    }
    val keys: List<String> = file
        .readLines()
    return "\"${keys[0]}\""
}