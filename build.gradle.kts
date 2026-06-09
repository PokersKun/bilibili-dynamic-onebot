plugins {
    val kotlinVersion = "2.0.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

group = "top.colter"
version = "3.2.18"

application {
    mainClass.set("top.colter.mirai.plugin.bilibili.MainKt")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Ktor HTTP & WebSocket client
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("io.ktor:ktor-client-websockets:3.0.3")
    implementation("io.ktor:ktor-client-encoding:3.0.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")

    // YAML config
    implementation("com.charleskorn.kaml:kaml:0.57.0")

    // QR code
    implementation("com.google.zxing:javase:3.5.0")

    // Skia image rendering - all platforms
    implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.7.27")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.7.27")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-arm64:0.7.27")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.7.27")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.7.27")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    testImplementation(kotlin("test", "1.7.0"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}
