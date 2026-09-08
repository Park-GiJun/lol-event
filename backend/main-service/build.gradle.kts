plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

extra["springCloudVersion"] = "2025.1.1"

dependencies {
    implementation(project(":common"))
    implementation(kotlin("reflect"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Cloud
    // Eureka 클라이언트와 Config 클라이언트는 걷어냈다.
    // 등록할 서비스가 자기 자신 하나뿐이라 디스커버리가 할 일이 없었고, Config Server 가
    // 내려주던 값은 application-prd.yml 과 환경변수로 옮겼다.
    // 두 스타터는 기동 시 레지스트리 폴링 스레드와 하트비트 스케줄러를 띄운다. 그만큼이 순수 낭비였다.

    // Ktor Client
    implementation("io.ktor:ktor-client-core:3.1.1")
    implementation("io.ktor:ktor-client-cio:3.1.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.1")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Kotlin JDSL
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.8.0")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.8.0")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.8.0")

    // Redis (Spring Boot 4.0 호환 - starter 아닌 core 사용)
    implementation("org.redisson:redisson:3.52.0")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Kotlin Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}
