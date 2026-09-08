plugins {
    kotlin("jvm") version "2.2.0" apply false
    kotlin("plugin.spring") version "2.2.0" apply false
    kotlin("plugin.jpa") version "2.2.0" apply false
    id("org.springframework.boot") version "4.0.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.gijun"
    version = "0.0.1-SNAPSHOT"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // 빌드는 JDK 25 로 돌리되 바이트코드 타깃은 24 로 맞춘다.
    // Kotlin 2.2 가 아직 JVM 타깃 25 를 못 내서, release 만 25 로 두면 Java 25 / Kotlin 24 로
    // 갈라진다. 그 불일치는 gradle.properties 의 validation.mode=warning 으로 덮여 있었다.
    // Kotlin 이 25 를 지원하면 아래 두 상수와 툴체인을 같이 올리면 된다.
    val bytecodeTarget = 24

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(25)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(bytecodeTarget.toString()))
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    tasks.withType<JavaCompile> {
        options.release.set(bytecodeTarget)
    }

    tasks.withType<Test> { useJUnitPlatform() }
}
