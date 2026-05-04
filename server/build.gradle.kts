plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
    jacoco
}

group = "com.lowaltitude.reststop"
version = "1.0.0"

val flywayVersion = "11.14.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.7")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-mysql:$flywayVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:all"))
    if (name == "compileJava") {
        options.compilerArgs.add("-Werror")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.12"
}

fun jacocoExcludes() = listOf(
    "**/DemoPlatformService.class",
    "**/DemoDataSeeder.class",
    "**/controller/**",
    "**/config/**",
    "**/security/**",
    "**/common/**",
    "**/mapper/**",
    "**/entity/**",
    "**/api/**",
    "**/AmapWeatherService*.class",
    "**/AlertService.class",
    "**/AuditLogService.class",
    "**/RefreshTokenStore.class",
    "**/ServerApplication.class"
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude(jacocoExcludes())
        }
    }))
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                minimum = providers.gradleProperty("serverCoverageMinimum")
                        .orElse("1.00")
                        .map { it.toBigDecimal() }
                        .get()
            }
        }
    }
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude(jacocoExcludes())
        }
    }))
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
