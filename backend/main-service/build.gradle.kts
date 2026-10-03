plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jlleitschuh.gradle.ktlint")
}

extra["springCloudVersion"] = "2025.1.1"

dependencies {
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
    // Spring MVC 는 Jackson 3 으로 직렬화한다. 이 모듈이 없으면 Kotlin 의 `is` 접두 프로퍼티가
    // `isKing` → `king` 으로 깎여 나가고, 기본값이 있는 생성자 파라미터가 본문에서 빠지면 400 이 된다.
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    // Kotlin not-null 파라미터에 Mockito 의 any() 를 쓰면 null 이 들어가 NPE 가 난다. 그걸 감싸 준다.
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

ktlint {
    version.set("1.5.0")
    android.set(false)
    ignoreFailures.set(false)
    coloredOutput.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    }
    filter {
        exclude { it.file.path.contains("/build/") }
        exclude { it.file.path.contains("/generated/") }
    }
}

// bootRun 도 lint 를 통과해야 기동되도록 — 로컬 실행 전에 형식/규칙 위반을 잡는다.
tasks.named("bootRun") {
    dependsOn("ktlintCheck")
}

// ── FE 에러 코드 생성 ────────────────────────────────────────────────────
// `ErrorCode.kt` 를 파싱해 프론트의 `errorCodes.generated.ts` 를 만든다. 컴파일 산출물이 아니라
// 소스를 읽는 이유는 이 task 가 **컴파일·클래스패스·Spring 컨텍스트에 기대지 않게** 하기 위함이다
// (생성기를 main 소스셋에 두면 프로덕션 jar 에 섞이고, 별도 소스셋은 이 한 줄짜리 일에 과하다).
//
// 대신 파싱이 성립하려면 enum 상수 형태가 `    NAME("설명"),` 여야 한다 — 형태가 깨지면 0 건을
// 읽고 task 가 실패한다(조용히 빈 파일을 쓰지 않는다).

val errorCodeSource = file("src/main/kotlin/com/gijun/main/shared/domain/exception/ErrorCode.kt")
val errorCodeTsFile = file("$rootDir/../frontend/src/lib/constants/errorCodes.generated.ts")

// companion object 안의 COMMON 목록은 들여쓰기가 깊어(16칸) 걸리지 않는다.
val errorCodeEntryPattern = Regex("""^ {4}([A-Z][A-Z0-9_]*)\("(.*)"\),$""")

fun readErrorCodes(): List<Pair<String, String>> {
    require(errorCodeSource.isFile) { "ErrorCode.kt 가 없다: $errorCodeSource" }
    val entries =
        errorCodeSource.readLines().mapNotNull { line ->
            errorCodeEntryPattern.find(line)?.let { it.groupValues[1] to it.groupValues[2] }
        }
    require(entries.isNotEmpty()) {
        "ErrorCode.kt 에서 코드를 하나도 읽지 못했다 — enum 상수가 `    NAME(\"설명\"),` 형태인지 확인한다."
    }
    val duplicated = entries.groupBy { it.first }.filterValues { it.size > 1 }.keys
    require(duplicated.isEmpty()) { "중복된 에러 코드: $duplicated" }
    return entries
}

// prettier 설정을 따른다 — 탭 들여쓰기 · 작은따옴표 · trailingComma none.
fun renderErrorCodeTs(): String =
    buildString {
        appendLine("/**")
        appendLine(" * 에러 코드 — **백엔드의 `ErrorCode` enum 에서 생성된다. 직접 고치지 않는다.**")
        appendLine(" *")
        appendLine(" * 정본: `backend/main-service/src/main/kotlin/com/gijun/main/shared/domain/exception/ErrorCode.kt`")
        appendLine(" * 재생성: backend 에서 `./gradlew generateErrorCodes`")
        appendLine(" *")
        appendLine(" * 화면에 띄울 문구는 여기 없다 — `errorMessages.ts` 가 소유한다.")
        appendLine(" * 아래 주석은 백엔드가 적어 둔 설명으로, 문구를 쓸 때 참고하는 값이다.")
        appendLine(" */")
        appendLine()
        appendLine("export const ERROR_CODE = {")
        readErrorCodes().forEach { (code, description) ->
            appendLine("\t/** $description */")
            appendLine("\t$code: '$code',")
        }
        appendLine("} as const;")
        appendLine()
        appendLine("/** 백엔드가 내려보낼 수 있는 에러 코드 전부. */")
        appendLine("export type ErrorCode = (typeof ERROR_CODE)[keyof typeof ERROR_CODE];")
    }.replace(",\n} as const;", "\n} as const;") // trailingComma: none

tasks.register("generateErrorCodes") {
    group = "codegen"
    description = "ErrorCode enum 에서 프론트의 errorCodes.generated.ts 를 만든다."
    inputs.file(errorCodeSource)
    outputs.file(errorCodeTsFile)
    doLast {
        errorCodeTsFile.parentFile.mkdirs()
        errorCodeTsFile.writeText(renderErrorCodeTs())
        logger.lifecycle("에러 코드 ${readErrorCodes().size} 종을 생성했다 — $errorCodeTsFile")
    }
}

tasks.register("checkErrorCodes") {
    group = "verification"
    description = "프론트 생성물이 ErrorCode enum 과 어긋나지 않는지 본다."
    inputs.file(errorCodeSource)
    doLast {
        // 개행은 정규화해서 비교한다 — git autocrlf 가 CRLF 로 바꿔 두면 내용이 같아도 어긋난다.
        val expected = renderErrorCodeTs().replace("\r\n", "\n")
        val actual = errorCodeTsFile.takeIf { it.isFile }?.readText()?.replace("\r\n", "\n")
        check(actual == expected) {
            "프론트 에러 코드 생성물이 ErrorCode enum 과 다르다 — `./gradlew generateErrorCodes` 를 돌리고 같이 커밋한다.\n" +
                "  대상: $errorCodeTsFile"
        }
    }
}

// 프론트 디렉터리가 있을 때만 검사에 건다 — 백엔드만 체크아웃한 곳에서 빌드가 깨지지 않게.
if (file("$rootDir/../frontend/src").isDirectory) {
    tasks.named("check") {
        dependsOn("checkErrorCodes")
    }
}
