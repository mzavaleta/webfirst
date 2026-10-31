plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing is supplied by the environment, never committed:
//   WEBFIRST_KEYSTORE           path to the .jks
//   WEBFIRST_KEYSTORE_PASSWORD  store password
//   WEBFIRST_KEY_ALIAS          key alias
//   WEBFIRST_KEY_PASSWORD       key password
// Without all four, release builds stay unsigned and debug builds are unaffected.
val releaseKeystorePath: String? = System.getenv("WEBFIRST_KEYSTORE")
val releaseKeystorePassword: String? = System.getenv("WEBFIRST_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("WEBFIRST_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("WEBFIRST_KEY_PASSWORD")
val hasReleaseSigning: Boolean = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() } && file(releaseKeystorePath!!).exists()

android {
    namespace = "io.github.mzavaleta.webfirst"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.mzavaleta.webfirst"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.all {
            it.useJUnit()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.browser:browser:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
