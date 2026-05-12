import java.io.File
import java.util.Properties

fun decodeLocalProperty(value: String): String =
    value.replace("\\:", ":").replace("\\\\", "\\")

fun findSdkDir(rootDir: File): File? {
    val localProperties = File(rootDir, "local.properties")
    val sdkFromLocal = if (localProperties.exists()) {
        Properties().apply {
            localProperties.inputStream().use { load(it) }
        }.getProperty("sdk.dir")?.takeIf { it.isNotBlank() }?.let(::decodeLocalProperty)
    } else {
        null
    }

    val sdkPath = sdkFromLocal
        ?: System.getenv("ANDROID_SDK_ROOT")?.takeIf { it.isNotBlank() }
        ?: System.getenv("ANDROID_HOME")?.takeIf { it.isNotBlank() }

    return sdkPath?.let(::File)?.takeIf { it.isDirectory }
}

fun findAapt2Executable(sdkDir: File): File? {
    val executableName = if (System.getProperty("os.name").startsWith("Windows")) "aapt2.exe" else "aapt2"
    val buildToolsDir = File(sdkDir, "build-tools")
    if (!buildToolsDir.isDirectory) {
        return null
    }

    val preferredVersions = listOf("36.1.0", "36.0.0", "37.0.0")
    val preferredMatch = preferredVersions
        .asSequence()
        .map { File(buildToolsDir, "$it/$executableName") }
        .firstOrNull { it.isFile }
    if (preferredMatch != null) {
        return preferredMatch
    }

    return buildToolsDir.listFiles()
        ?.asSequence()
        ?.filter { it.isDirectory }
        ?.sortedByDescending { it.name }
        ?.map { File(it, executableName) }
        ?.firstOrNull { it.isFile }
}

if (System.getProperty("android.aapt2FromMavenOverride").isNullOrBlank()) {
    findSdkDir(rootDir)
        ?.let(::findAapt2Executable)
        ?.let { System.setProperty("android.aapt2FromMavenOverride", it.absolutePath) }
}

pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
    }
}

rootProject.name = "Low-altitude Rest Stop"
include(":app")
include(":server")

include(":core:ui")
include(":core:network")
include(":core:session")
include(":core:storage")
include(":core:trace")

include(":feature:risk")
