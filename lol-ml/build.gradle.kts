plugins {
    kotlin("jvm") version "2.4.20"
    application
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "com.gijun"
version = "0.0.1-SNAPSHOT"

repositories { mavenCentral() }

kotlin {
    jvmToolchain(25)
}

dependencies {
    // 1·2단계는 ML 라이브러리를 쓰지 않는다. 여기 드라이버 말고 다른 것이 늘면 원칙을 다시 본다.
    runtimeOnly("org.postgresql:postgresql:42.7.13")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.gijun.lolml.MainKt")
}

tasks.withType<Test> { useJUnitPlatform() }

ktlint {
    version.set("1.8.0")
    android.set(false)
    ignoreFailures.set(false)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}
