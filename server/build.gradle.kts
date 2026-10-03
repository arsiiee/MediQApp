plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

// The app's domain models are the shared contract. This module compiles that
// exact directory as plain JVM Kotlin — no Android, no Compose — so a change to
// a model breaks the server build if it needs Android APIs.
//
// Path is relative to `server/`, so `../../app` is the app module.
val sharedDomain = file("../app/src/main/java/com/example/mediq/domain")

kotlin {
    sourceSets["main"].kotlin.srcDir(sharedDomain)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.server.ktor.core)
    implementation(libs.server.ktor.netty)
    implementation(libs.server.ktor.auth)
    implementation(libs.server.ktor.auth.jwt)
    implementation(libs.server.ktor.content.negotiation)
    implementation(libs.server.ktor.serialization)
    implementation(libs.server.ktor.status.pages)
    implementation(libs.server.ktor.call.logging)
    implementation(libs.server.coroutines.core)
    implementation(libs.server.logback)

    implementation(libs.server.h2)
    implementation(libs.server.hikari)
    implementation(libs.server.java.jwt)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.mediq.server.MainKt")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}