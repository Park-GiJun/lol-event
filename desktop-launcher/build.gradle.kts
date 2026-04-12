import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.compose") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.0"
}

group = "net.gijun"
version = "1.0.1"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    val ktorVersion = "3.0.3"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
}

kotlin {
    jvmToolchain(25)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

compose.desktop {
    application {
        mainClass = "net.gijun.launcher.LauncherMainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "LoL-Collector-Launcher"
            packageVersion = version.toString()
            vendor = "gijun.net"
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                dirChooser = true
                menuGroup = "LoL-Collector"
                shortcut = true
                // 본체 MSI(a1b2c3d4-...)와 반드시 다른 UUID
                upgradeUuid = "f7e8d9c0-1234-5678-9abc-def012345678"
            }
        }
    }
}
