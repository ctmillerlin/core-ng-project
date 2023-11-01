plugins {
    kotlin("jvm") version "1.9.20"
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:5.1.5")
    implementation("org.flywaydb:flyway-gradle-plugin:9.22.0")
    implementation("org.flywaydb:flyway-mysql:9.22.0")
}