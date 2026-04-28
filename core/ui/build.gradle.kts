plugins {
    id("com.android.library")
}

android {
    namespace = "com.example.low_altitudereststop.core.ui"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api("androidx.appcompat:appcompat:1.7.0")
    api("androidx.activity:activity:1.10.1")
    api("androidx.core:core-ktx:1.16.0")
    api("com.google.android.material:material:1.12.0")
    api("androidx.constraintlayout:constraintlayout:2.2.1")
    api("androidx.recyclerview:recyclerview:1.4.0")
    api("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}

