plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    // Whether to point at the local Firebase emulator suite.
    // Defaults to true for debug builds, false for release.
    // Override via: ./gradlew assembleDebug -PuseFirebaseEmulator=false
    val useEmulatorDebug = (project.findProperty("useFirebaseEmulator") as String?)
        ?.toBoolean()
        ?: false
    val useEmulatorRelease = (project.findProperty("useFirebaseEmulator") as String?)
        ?.toBoolean()
        ?: false
    val firebaseEmulatorHost = project.findProperty("firebaseEmulatorHost") as String?
        ?: "10.0.2.2"

    namespace = "com.example.androidapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.androidapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // MediaPipe Tasks Text does not ship x86_64 native libraries.
        // Excluding x86_64 forces 32-bit x86 compat mode on x86_64 emulators,
        // where the x86 variant of libmediapipe_tasks_text_jni.so IS available.
        // Physical ARM devices are unaffected (arm64-v8a is included).
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86")
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USE_FIREBASE_EMULATOR", useEmulatorDebug.toString())
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"$firebaseEmulatorHost\"")
        }
        release {
            buildConfigField("boolean", "USE_FIREBASE_EMULATOR", useEmulatorRelease.toString())
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"$firebaseEmulatorHost\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    androidResources {
        noCompress += "tflite"
    }

}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Fragment (for XML-based screens)
    implementation(libs.androidx.fragment.ktx)

    // Compose (using BoM)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Firebase (using BoM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)


    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.room.testing)

    // Navigation Compose
    implementation(libs.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.play.services)

    // Coil - Image loading from URL
    implementation(libs.coil.compose)

    // Google Fonts for Compose (Playfair Display + Inter)
    implementation(libs.compose.ui.text.google.fonts)

    // Gson (for Room type converters)
    implementation(libs.gson)
    // WorkManager
    implementation(libs.work.runtime.ktx)
    // DataStore Preferences
    implementation(libs.datastore.preferences)
    implementation(libs.zxing.core)

    // MediaPipe Tasks Text (on-device text embeddings)
    implementation(libs.mediapipe.tasks.text)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))

    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                org.jetbrains.dokka.DokkaConfiguration.Visibility.PROTECTED
            )
        )

        sourceLink {
            localDirectory.set(file("src/main/java"))
            remoteUrl.set(
                uri("https://github.com/nimonht/AndroidApp/tree/master/app/src/main/java").toURL()
            )
            remoteLineSuffix.set("#L")
        }
    }
}
