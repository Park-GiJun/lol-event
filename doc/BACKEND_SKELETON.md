# Backend Architecture - Microservices

> ATPOS 3.0 백엔드 마이크로서비스 아키텍처 설명서.
> Spring Cloud Gateway + Eureka 기반, 서비스별 헥사고날 아키텍처 적용.
> main-service: Kotlin/Spring Boot, telex-service: TypeScript/NestJS (Fastify).

---

## 1. Tech Stack Summary

### Shared Infrastructure

| Layer | Technology | Version |
|-------|-----------|---------|
| **Gateway** | Spring Cloud Gateway (WebFlux) | (Cloud BOM) |
| **Discovery** | Netflix Eureka (Server + Client) | (Cloud BOM) |
| **Database** | PostgreSQL 16 (Primary + Replica) | 스키마 분리 |
| **Cache** | Redis 7 + Redisson 3.52.0 | |
| **Monitoring** | Prometheus + Grafana | |
| **CI/CD** | Jenkins | Pipeline |

### main-service (Kotlin/Spring Boot)

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Kotlin | 2.1.20 |
| **JVM** | Java (Toolchain) | 25 |
| **Framework** | Spring Boot | 4.0.1 |
| **Cloud** | Spring Cloud (Oakwood) | 2025.1.1 |
| **Build** | Gradle Kotlin DSL (Multi-module) | 9.3.1 |
| **IPC** | OpenFeign | (Cloud BOM) |
| **ORM/Query** | Spring Data JPA + Kotlin JDSL | 3.8.0 |
| **DB Migration** | Flyway | (Boot BOM) |

### telex-service (TypeScript/NestJS)

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | TypeScript | ^5.7.0 |
| **Runtime** | Node.js | 22+ |
| **Framework** | NestJS + Fastify | ^11.0.0 |
| **Discovery** | eureka-js-client | ^4.5.0 |
| **Build** | npm | |

---

## 2. 서비스 구성

```
backend/ (Root multi-module)
├── common/          → 공유 라이브러리 (enums, exceptions, response)
├── eureka-server/   → 서비스 디스커버리 (port 8761)
├── api-gateway/     → Spring Cloud Gateway WebFlux (port 8080)
├── main-service/    → Backoffice 서비스 (port 8081)
└── telex-service/   → POS 마스터/전문 통신 서비스 (port 8082)
```

| 서비스 | 역할 | Port | DB 스키마 |
|--------|------|------|-----------|
| **eureka-server** | 서비스 디스커버리 (등록/탐색) | 8761 | - |
| **api-gateway** | API 라우팅, JWT 검증, CORS | 8080 | - |
| **main-service** | Backoffice — 사용자 관리, 매장 관리, 메뉴, 매출, 대시보드 등 관리 기능 | 8081 | `atpos_main` |
| **telex-service** | POS 마스터/전문 — POS 마스터 데이터 관리, VAN/PG 전문 통신, 결제 처리 (**NestJS/Fastify**) | 8082 | `atpos_telex` |

### 서비스 간 통신

- **프론트엔드 → api-gateway → 각 서비스**: HTTP (Gateway가 라우팅)
- **서비스 간**: OpenFeign (Kotlin) / HTTP client (Node.js) + Eureka 서비스 디스커버리 (`lb://service-name`)
- **라우팅 규칙**: `/api/telex/**` → telex-service, `/api/**` → main-service, `/graphql` → main-service

---

## 3. Project Structure

```
{project-root}/
├── backend/
│   ├── build.gradle.kts          # Root 공유 설정 (plugins apply false)
│   ├── settings.gradle.kts       # 5개 모듈 선언
│   ├── .env.example              # 환경변수 템플릿
│   │
│   ├── common/                   # 공유 라이브러리 (순수 Kotlin, Spring 없음)
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/atpos/common/
│   │       ├── enums/            # Locale, MenuType, OperationStatus, TargetType, UserRole
│   │       ├── exception/        # 도메인 예외 7종
│   │       └── response/         # CommonApiResponse<T>
│   │
│   ├── eureka-server/            # 서비스 디스커버리
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── kotlin/.../eureka/EurekaServerApplication.kt
│   │       └── resources/application.yml
│   │
│   ├── api-gateway/              # API Gateway (WebFlux)
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── kotlin/.../gateway/
│   │       │   ├── ApiGatewayApplication.kt
│   │       │   ├── config/
│   │       │   │   ├── RouteConfig.kt       # 라우팅 정의 (RouteLocator)
│   │       │   │   └── CorsConfig.kt        # CORS 설정
│   │       │   └── filter/
│   │       │       └── JwtAuthenticationFilter.kt  # JWT 검증 (GlobalFilter)
│   │       └── resources/application.yml
│   │
│   ├── main-service/             # Backoffice 서비스
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── kotlin/.../main/  # 헥사고날 아키텍처 (§4 참조)
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── logback-spring.xml
│   │           └── db/migration/
│   │
│   └── telex-service/            # POS 마스터/전문 서비스 (NestJS)
│       ├── package.json
│       ├── tsconfig.json
│       ├── nest-cli.json
│       └── src/                  # 헥사고날 아키텍처 (§4.2 참조)
│           ├── main.ts
│           ├── app.module.ts
│           ├── common/
│           ├── config/
│           ├── health/
│           ├── domain/
│           ├── application/
│           └── infrastructure/
│
├── frontend/                     # SvelteKit 프론트엔드
├── doc/                          # 문서
├── history/                      # 변경 이력
│   ├── backend/
│   ├── frontend/
│   └── infra/
├── docker-atpos3_0/              # 로컬 인프라 Docker Compose
└── infra/
    ├── postgres/init-schemas.sql  # atpos_main, atpos_telex 스키마
    ├── prometheus/prometheus.yml   # 3개 서비스 scrape targets
    └── grafana/provisioning/
```

---

## 4. 헥사고날 아키텍처

두 비즈니스 서비스는 동일한 헥사고날 계층 구조를 따르되, 기술 스택이 다름.

**의존성 방향**: `domain` ← `application` ← `infrastructure`
- domain: 순수 비즈니스 로직 (외부 프레임워크 의존성 없음)
- application: domain만 참조
- infrastructure: 모든 계층 참조 + 외부 라이브러리

### 4.1 main-service (Kotlin/Spring Boot)

```
main-service/src/main/kotlin/com/atpos/main/
├── MainServiceApplication.kt      # @SpringBootApplication + @EnableDiscoveryClient + @EnableFeignClients
│
├── domain/
│   ├── model/                     # JPA Entity, Value Objects
│   └── service/                   # 도메인 서비스
│
├── application/
│   ├── port/
│   │   ├── in/                    # Inbound ports (Use case interfaces)
│   │   └── out/                   # Outbound ports (Repository interfaces)
│   ├── handler/                   # Use case 구현 (command/, query/)
│   └── dto/                       # Application DTOs (data class)
│
└── infrastructure/
    ├── config/                    # Security, Redis, Swagger 설정
    └── adapter/
        ├── in/web/                # @RestController + GlobalExceptionHandler
        └── out/
            ├── persistence/       # JPA Repository 구현
            └── client/            # OpenFeign 클라이언트
```

### 4.2 telex-service (TypeScript/NestJS)

```
telex-service/src/
├── main.ts                        # NestJS bootstrap + Eureka 등록
├── app.module.ts                  # Root NestJS module
│
├── common/                        # 공유 유틸 (= common 모듈 대응)
│   ├── response/                  # CommonApiResponse 호환 래퍼
│   ├── exception/                 # 도메인 예외 7종
│   └── filter/                    # GlobalExceptionFilter
│
├── config/                        # Eureka, 환경변수 설정
├── health/                        # /actuator/health 대응 헬스체크
│
├── domain/
│   ├── model/                     # Entity, Value Objects
│   └── service/                   # 도메인 서비스
│
├── application/
│   ├── port/
│   │   ├── in/                    # Inbound ports (Use case interfaces)
│   │   └── out/                   # Outbound ports (Repository interfaces)
│   ├── handler/                   # Use case 구현 (command/, query/)
│   └── dto/                       # Application DTOs
│
└── infrastructure/
    ├── config/                    # DB, 외부 연동 설정
    └── adapter/
        ├── in/web/                # NestJS @Controller
        └── out/
            ├── persistence/       # TypeORM/Prisma Repository 구현
            └── client/            # HTTP 클라이언트 (서비스 간 호출)
```

---

## 5. Gradle 멀티모듈 설정

### 5.1 `settings.gradle.kts`

```kotlin
rootProject.name = "atpos3"
include("common", "eureka-server", "api-gateway", "main-service")
// telex-service: Node.js (NestJS) — backend/telex-service/에서 npm으로 별도 관리
```

### 5.2 Root `build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm") version "2.1.20" apply false
    kotlin("plugin.spring") version "2.1.20" apply false
    kotlin("plugin.jpa") version "2.1.20" apply false
    kotlin("kapt") version "2.1.20" apply false
    id("org.springframework.boot") version "4.0.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.atpos"
    version = "0.0.1-SNAPSHOT"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
    tasks.withType<Test> { useJUnitPlatform() }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") }
    }
}
```

### 5.3 모듈별 `build.gradle.kts` 패턴

| 모듈 | 플러그인 | 핵심 의존성 |
|------|---------|------------|
| **common** | (kotlin.jvm만) | `kotlin-reflect` |
| **eureka-server** | kotlin.spring, spring-boot, dep-mgmt | `spring-cloud-starter-netflix-eureka-server` |
| **api-gateway** | kotlin.spring, spring-boot, dep-mgmt | `spring-cloud-starter-gateway-server-webflux`, eureka-client, actuator |
| **main-service** | kotlin.spring, kotlin.jpa, kapt, spring-boot, dep-mgmt | `project(":common")`, web, jpa, kotlin-jdsl, eureka-client, openfeign, redisson, swagger 등 |
| **telex-service** | — (npm 관리) | `@nestjs/core`, `@nestjs/platform-fastify`, `eureka-js-client` 등 |

> **주의**: api-gateway에는 `spring-boot-starter-web` 절대 포함 불가 (WebFlux 충돌). common 모듈도 Servlet 의존성 없어야 함.
> telex-service는 Gradle 빌드에 포함되지 않음. `cd backend/telex-service && npm run build`로 별도 빌드.

### 5.4 Kotlin Plugins 역할

| Plugin | 역할 |
|--------|------|
| `kotlin("plugin.spring")` | Spring 프록시 클래스에 `open` 자동 적용 |
| `kotlin("plugin.jpa")` | `@Entity` 등에 no-arg constructor 자동 생성 |
| `kotlin("kapt")` | QueryDSL Q-class 생성용 annotation processing |

---

## 6. common 모듈

순수 Kotlin 라이브러리. Spring Boot 의존성 없음.

### 6.1 공유 Enum (`com.atpos.common.enums`)

| Enum | 값 |
|------|----|
| `Locale` | KOREAN, ENGLISH |
| `MenuType` | FOLDER, PAGE |
| `OperationStatus` | OPEN, CLOSED |
| `TargetType` | DOMAIN, DESC, MENU, EXCEPTION |
| `UserRole` | ADMIN, HEADQUARTER, STORE_MANAGER, STORE_STAFF |

### 6.2 도메인 예외 (`com.atpos.common.exception`)

모든 예외는 `open class`로 선언 (서비스별 확장 가능):

| Exception | 기본 메시지 | HTTP Status |
|-----------|-----------|-------------|
| `DomainNotFoundException` | Resource not found | 404 |
| `DomainValidationException` | Validation failed | 400 |
| `DomainAlreadyExistsException` | Resource already exists | 409 |
| `DomainConflictException` | Resource conflict | 409 |
| `DomainForbiddenException` | Access forbidden | 403 |
| `DomainUnauthorizedException` | Unauthorized access | 401 |
| `DomainInvalidStateException` | Invalid state | 422 |

### 6.3 공통 응답 (`com.atpos.common.response`)

```kotlin
data class CommonApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val timestamp: String = Instant.now().toString()
) {
    companion object {
        fun <T> success(data: T): CommonApiResponse<T>
        fun <T> created(data: T): CommonApiResponse<T>
        fun <T> error(message: String, errorCode: String? = null): CommonApiResponse<T>
    }
}
```

---

## 7. API Gateway

Spring Cloud Gateway (WebFlux 기반). JWT 검증을 게이트웨이에서 중앙 처리.

### 7.1 라우팅 (`RouteConfig`)

```
/api/telex/** → lb://telex-service
/api/**       → lb://main-service
/graphql      → lb://main-service
```

`RouteLocatorBuilder`를 사용한 프로그래매틱 라우팅 (YAML property 호환성 이슈 방지).

### 7.2 JWT 인증 필터 (`JwtAuthenticationFilter`)

- `GlobalFilter` + `Ordered` 구현
- 제외 경로: `/api/auth/login`, `/api/auth/refresh`, `/actuator`
- `Authorization: Bearer {token}` 헤더 검증
- 인증 실패 시 401 응답

### 7.3 CORS (`CorsConfig`)

- 허용 Origin: `localhost:5173` (프론트엔드 dev), `localhost:3000`
- 허용 Method: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Credentials 허용, Max-Age 3600초

---

## 8. 서비스별 설정

### 8.1 `application.yml` 공통 패턴

```yaml
server:
  port: {port}

spring:
  application:
    name: {service-name}
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}?currentSchema={schema}
  flyway:
    schemas: [ {schema} ]
    default-schema: {schema}

eureka:
  client:
    service-url:
      defaultZone: http://${EUREKA_HOST:localhost}:${EUREKA_PORT:8761}/eureka/
```

### 8.2 Logback (`logback-spring.xml`)

프로필별 로깅:
- **default/dev/local**: 컬러 콘솔, DEBUG 레벨, Hibernate SQL 로깅
- **prd**: 파일 로깅 (rolling, 100MB/30일/3GB) + ERROR 분리 파일

---

## 9. 환경변수 (`.env.example`)

```env
POSTGRES_HOST=localhost
REDIS_HOST=localhost
EUREKA_HOST=localhost
EUREKA_PORT=8761
POSTGRES_USER=atpos
POSTGRES_PASSWORD=atpos
POSTGRES_DB=atpos
POSTGRES_PORT=5432
REDIS_PASSWORD=
REDIS_PORT=6379
SPRING_PROFILES_ACTIVE=local
```

---

## 10. PostgreSQL 스키마

서비스별 스키마 분리:

```sql
CREATE SCHEMA IF NOT EXISTS atpos_main;
GRANT ALL PRIVILEGES ON SCHEMA atpos_main TO atpos;

CREATE SCHEMA IF NOT EXISTS atpos_telex;
GRANT ALL PRIVILEGES ON SCHEMA atpos_telex TO atpos;
```

Flyway가 각 서비스 스키마의 마이그레이션을 독립 관리.

---

## 11. Monitoring

### Prometheus 설정

3개 서비스 scrape:

| Job | Target | Path |
|-----|--------|------|
| api-gateway | `host.docker.internal:8080` | `/actuator/prometheus` |
| main-service | `host.docker.internal:8081` | `/actuator/prometheus` |
| telex-service | `host.docker.internal:8082` | `/actuator/prometheus` |

+ 인프라: postgres-exporter, redis-exporter, node-exporter

---

## 12. 실행 순서

```bash
# 1. 인프라 (Docker)
cd docker-atpos3_0 && docker-compose up -d

# 2. Eureka Server (먼저 기동, 서비스 등록소)
./gradlew :eureka-server:bootRun

# 3. 비즈니스 서비스 (동시 기동 가능)
./gradlew :main-service:bootRun
cd backend/telex-service && npm run start:dev

# 4. API Gateway (서비스 등록 후)
./gradlew :api-gateway:bootRun
```

### 주요 빌드 명령

```bash
cd backend
./gradlew clean build -x test           # Kotlin 서비스 전체 빌드
./gradlew :main-service:test            # main-service 테스트
./gradlew :main-service:bootRun         # main-service 실행

# telex-service (Node.js)
cd backend/telex-service
npm install                              # 의존성 설치
npm run build                            # 프로덕션 빌드
npm run start:dev                        # 개발 모드 실행
npm test                                 # 테스트
```

---

## 13. Code Style

| 영역 | 규칙 |
|------|------|
### Kotlin (main-service)

| 영역 | 규칙 |
|------|------|
| **Style** | Kotlin coding conventions, `-Xjsr305=strict` |
| **DTO** | `data class` 사용 |
| **Entity** | `@Entity`, `plugin.jpa`가 no-arg constructor 자동 생성 |
| **Null Safety** | Kotlin null 타입 시스템 (`?`, `!!`, `?.`, `?:`) |
| **Logging** | `LoggerFactory.getLogger(javaClass)` |
| **Test** | JUnit 5 + Spring Boot Test |
| **패키지 `in`** | Kotlin 예약어 → 백틱 필수: `` `in` `` |

### TypeScript (telex-service)

| 영역 | 규칙 |
|------|------|
| **Style** | strict TypeScript, NestJS decorators |
| **DTO** | `class` + class-validator decorators |
| **Entity** | TypeORM Entity 또는 Prisma model |
| **Null Safety** | `strictNullChecks: true` |
| **Logging** | NestJS `Logger` (built-in) |
| **Test** | Jest + @nestjs/testing |

### 공통

| 영역 | 규칙 |
|------|------|
| **API 응답** | `CommonApiResponse<T>` — 동일 JSON 포맷 (`success`, `data`, `message`, `code`, `timestamp`) |
| **예외 처리** | 도메인 예외 7종 → 서비스별 GlobalExceptionHandler/Filter |
| **Naming** | camelCase (변수/메서드), PascalCase (클래스), UPPER_SNAKE (상수) |

---

## 14. CI/CD Pipeline (Jenkins)

```groovy
pipeline {
    agent any
    environment {
        JAVA_HOME = tool name: 'temurin-25', type: 'jdk'
    }
    stages {
        stage('Build') {
            steps { sh './gradlew clean build -x test' }
        }
        stage('Test') {
            steps { sh './gradlew test' }
            post { always { junit '**/build/test-results/test/*.xml' } }
        }
        stage('Deploy') {
            steps { sh './deploy.sh' }
        }
        stage('Health Check') {
            steps {
                sh 'curl -f http://localhost:8761/actuator/health'  // Eureka
                sh 'curl -f http://localhost:8080/actuator/health'  // Gateway
                sh 'curl -f http://localhost:8081/actuator/health'  // Main
                sh 'curl -f http://localhost:8082/actuator/health'  // Telex
            }
        }
    }
    triggers { pollSCM('H/5 * * * *') }
    options { timeout(time: 30, unit: 'MINUTES') }
}
```

---

## 15. 변경 이력 관리

```
history/
├── backend/    # 백엔드 변경 이력
├── frontend/   # 프론트엔드 변경 이력
└── infra/      # 인프라/CI/CD 변경 이력
```

- **파일명**: `YYYY-MM-DD_NN.md`
- 작업 완료 후 반드시 작성
- 변경의 "왜"와 "무엇" 기록
- 배포 시 추가 작업 포함
