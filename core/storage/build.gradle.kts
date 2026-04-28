plugins {
    id("com.android.library")
}

android {
    namespace = "com.example.low_altitudereststop.core.storage"
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
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    annotationProcessor("org.xerial:sqlite-jdbc:3.41.2.2")
    implementation("androidx.work:work-runtime:2.9.0")
    implementation(project(":core:network"))
    implementation(project(":core:session"))
    implementation(project(":core:trace"))
    implementation("com.google.code.gson:gson:2.11.0")
}

