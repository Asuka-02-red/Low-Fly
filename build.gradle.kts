plugins {
    id("com.android.application") version "9.0.0" apply false
    id("com.android.library") version "9.0.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("org.springframework.boot") version "3.2.12" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.sonarqube") version "6.2.0.5505"
}

allprojects {
    group = "com.lowaltitude.reststop"
    version = "2.2.0"
}
