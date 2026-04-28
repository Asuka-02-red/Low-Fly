import java.util.Properties

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun propertyOrEnv(name: String): String =
    ((findProperty(name) as String?) ?: localProperties.getProperty(name) ?: System.getenv(name) ?: "")

fun escapeBuildString(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

val releaseStoreFile = propertyOrEnv("RELEASE_STORE_FILE")
val releaseStorePassword = propertyOrEnv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = propertyOrEnv("RELEASE_KEY_ALIAS")
val releaseKeyPassword = propertyOrEnv("RELEASE_KEY_PASSWORD")
val llmApiKey = escapeBuildString(propertyOrEnv("LLM_API_KEY"))
val llmApiSecret = escapeBuildString(propertyOrEnv("LLM_API_SECRET"))
val llmApiPassword = escapeBuildString(propertyOrEnv("LLM_API_PASSWORD"))
val llmAppId = escapeBuildString(propertyOrEnv("LLM_APP_ID"))
val llmProvider = escapeBuildString(propertyOrEnv("LLM_PROVIDER").ifBlank { "xfyun_spark" })
val llmEndpointUrl = escapeBuildString(propertyOrEnv("LLM_ENDPOINT_URL").ifBlank { "https://spark-api-open.xf-yun.com/x2/chat/completions" })
val llmInterfaceType = escapeBuildString(propertyOrEnv("LLM_INTERFACE_TYPE").ifBlank { "http_openapi" })
val llmModelName = escapeBuildString(propertyOrEnv("LLM_MODEL_NAME").ifBlank { "spark-x" })
val hasReleaseSigning = !releaseStoreFile.isNullOrBlank()
        && !releaseStorePassword.isNullOrBlank()
        && !releaseKeyAlias.isNullOrBlank()
        && !releaseKeyPassword.isNullOrBlank()

plugins {
    id("com.android.application")
    jacoco
}

android {
    namespace = "com.example.low_altitudereststop"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.low_altitudereststop"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "2.3.0"
        buildConfigField("boolean", "IS_DEMO_MODE", "false")
        buildConfigField("boolean", "ENABLE_AI_BALL", "true")
        buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
        buildConfigField("String", "LLM_PROVIDER", "\"$llmProvider\"")
        buildConfigField("String", "LLM_BASE_URL", "\"$llmEndpointUrl\"")
        buildConfigField("String", "LLM_MODEL_NAME", "\"$llmModelName\"")
        buildConfigField("String", "LLM_API_KEY", "\"$llmApiKey\"")
        buildConfigField("String", "LLM_API_SECRET", "\"$llmApiSecret\"")
        buildConfigField("String", "LLM_API_PASSWORD", "\"$llmApiPassword\"")
        buildConfigField("String", "LLM_APP_ID", "\"$llmAppId\"")
        buildConfigField("String", "LLM_INTERFACE_TYPE", "\"$llmInterfaceType\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                // Resolve keystore from the repository root so local.properties can use project-root-relative paths.
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    flavorDimensions += "channel"
    productFlavors {
        create("demo") {
            dimension = "channel"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            buildConfigField("boolean", "IS_DEMO_MODE", "true")
            buildConfigField("String", "CHANNEL_NAME", "\"demo\"")
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/api/\"")
            buildConfigField("String", "LLM_BASE_URL", "\"$llmEndpointUrl\"")
        }
        create("prod") {
            dimension = "channel"
            applicationIdSuffix = ".v2"
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("String", "CHANNEL_NAME", "\"prod\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.low-altitude.example.com/api/\"")
            buildConfigField("String", "LLM_BASE_URL", "\"$llmEndpointUrl\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        aidl = true
        buildConfig = true
        viewBinding = true
    }
    lint {
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
        baseline = file("lint-baseline.xml")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:session"))
    implementation(project(":core:storage"))
    implementation(project(":core:trace"))

    implementation(project(":feature:risk"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("androidx.navigation:navigation-fragment:2.8.9")
    implementation("androidx.navigation:navigation-ui:2.8.9")
    implementation("com.amap.api:3dmap:7.9.0.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.airbnb.android:lottie:6.6.7")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
}

jacoco {
    toolVersion = "0.8.12"
}
