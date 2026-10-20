plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.friday.assistant"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.friday.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
        ndk { abiFilters += "arm64-v8a" }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val releaseKey = System.getenv("FRIDAY_KEYSTORE_FILE")
    if (!releaseKey.isNullOrBlank()) {
        signingConfigs.create("privateRelease") {
            storeFile = file(releaseKey)
            storePassword = System.getenv("FRIDAY_STORE_PASSWORD")
            keyAlias = System.getenv("FRIDAY_KEY_ALIAS")
            keyPassword = System.getenv("FRIDAY_KEY_PASSWORD")
        }
        buildTypes.getByName("release") { signingConfig = signingConfigs.getByName("privateRelease") }
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    testImplementation("junit:junit:4.13.2")
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies { testImplementation("org.json:json:20240303") }

val verifyReleaseSigning by tasks.registering {
    doLast {
        check(listOf("FRIDAY_KEYSTORE_FILE", "FRIDAY_STORE_PASSWORD", "FRIDAY_KEY_ALIAS", "FRIDAY_KEY_PASSWORD").all { !System.getenv(it).isNullOrBlank() }) {
            "Configure private release-signing secrets first. Unsigned release distribution is disabled."
        }
    }
}
tasks.matching { it.name == "packageRelease" }.configureEach { dependsOn(verifyReleaseSigning) }

dependencies { testImplementation("org.robolectric:robolectric:4.14.1") }
