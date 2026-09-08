import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

version = "2025.11"

project {
    buildType(Build)
}

object Build : BuildType({
    name = "Build"

    // 컨테이너를 다섯에서 둘로 줄였다. 배경은 frontend/nginx.conf 주석 참고.
    //   lol-eureka / lol-api-gateway / lol-lcu-service 제거
    //   → nginx(정적 + API 프록시) + main-service 만 남는다.
    artifactRules = """
        backend/main-service/build/libs/*.jar => jars/
        frontend/dist/** => frontend-dist/
        desktop-collector/build/compose/binaries/main/msi/*.msi => desktop-collector-dist/
    """.trimIndent()

    params {
        // 빌드 항목
        checkbox("build.backend", "true",
            label = "[빌드] Backend (Gradle)", description = "main-service JAR 빌드",
            checked = "true", unchecked = "false")
        checkbox("build.frontend", "true",
            label = "[빌드] Frontend", description = "React 앱 빌드",
            checked = "true", unchecked = "false")
        checkbox("build.desktop", "false",
            label = "[빌드] Desktop Collector", description = "Compose Desktop 수집기 MSI 빌드",
            checked = "true", unchecked = "false")

        // 배포 항목
        checkbox("deploy.main", "true",
            label = "[배포] Main Service", description = "main-service 재배포",
            checked = "true", unchecked = "false")
        checkbox("deploy.frontend", "true",
            label = "[배포] Frontend (nginx)", description = "정적 파일 + API 프록시 재배포",
            checked = "true", unchecked = "false")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    triggers {
        vcs {
            branchFilter = "+:refs/heads/master"
        }
    }

    steps {
        gradle {
            id = "backend_build"
            name = "Backend - Gradle Build"
            // clean 을 빼면 체크아웃 디렉토리에 남은 증분 상태를 그대로 쓴다.
            // 붙여두면 매 빌드가 전체 재컴파일이라 1분 10초가 통째로 나갔다.
            //
            // 테스트는 켜 둔다. 예전에 -x test 로 꺼 놨는데, 그러면 도메인 규칙 테스트(Elo,
            // 포지션 판정, 라인 성과)가 CI 에서 한 번도 안 돌아 있으나 마나였다.
            // 전부 DB·네트워크를 안 타는 순수 테스트라 몇 초면 끝난다.
            tasks = "build"
            gradleParams = "--parallel --build-cache"
            workingDir = "backend"
            gradleWrapperPath = ""
            jdkHome = "%env.JAVA_HOME%"
            conditions {
                equals("build.backend", "true")
            }
        }
        script {
            id = "frontend_build"
            name = "Frontend - Install & Build"
            workingDir = "frontend"
            // --no-audit --no-fund: npm 은 설치할 때마다 취약점 조회와 후원 조회를 원격으로 돈다.
            // 이 빌드에서 그게 설치 시간의 대부분을 차지했다. 취약점 점검은 별도로 하면 된다.
            scriptContent = """
                npm ci --no-audit --no-fund
                npm run lint
                npm test
                npm run build
            """.trimIndent()
            conditions {
                equals("build.frontend", "true")
            }
        }
        script {
            id = "desktop_build"
            name = "Desktop Collector - Package MSI (Windows only)"
            scriptContent = """
                #!/bin/bash
                echo "=== Desktop Collector MSI 빌드 ==="
                # MSI 패키징은 Windows 전용 (jpackage + WiX 필요)
                # Linux CI에서는 스킵하고, 로컬 빌드된 MSI가 있으면 그대로 사용
                if ls desktop-collector/build/compose/binaries/main/msi/*.msi 1>/dev/null 2>&1; then
                    echo "기존 MSI 파일 발견 — 빌드 스킵"
                    ls -lh desktop-collector/build/compose/binaries/main/msi/*.msi
                else
                    echo "MSI 파일 없음 — Windows에서 로컬 빌드 후 커밋하세요:"
                    echo "  cd desktop-collector"
                    echo "  set JAVA_HOME=C:\\Users\\tpgj9\\.jdks\\ms-25.0.2"
                    echo "  gradlew.bat packageMsi"
                    echo "  git add build/compose/binaries/main/msi/"
                    echo ""
                    echo "또는 installer/ 디렉토리에 미리 빌드된 MSI를 배치하세요"
                fi
            """.trimIndent()
            conditions {
                equals("build.desktop", "true")
            }
        }
        script {
            id = "deploy"
            name = "Deploy - Services to Host"
            scriptContent = """
                #!/bin/bash
                set -e

                DEPLOY_BACKEND="%build.backend%"
                DEPLOY_FRONTEND="%build.frontend%"
                DEPLOY_DESKTOP="%build.desktop%"
                DO_MAIN="%deploy.main%"
                DO_FRONTEND="%deploy.frontend%"

                DEPLOY_DIR="/lol-event/deploy"
                HOST_DEPLOY="/home/gijunpark/lol-event/deploy"
                # DB 접속 정보 등 배포 환경 설정. 예전에는 Config Server 가 읽던 디렉터리다.
                HOST_CONFIG="/home/gijunpark/lol-event/config"
                # 실행에는 JDK 가 필요 없다. JRE 이미지가 컴파일러·도구를 안 들고 있어 더 가볍다.
                JAVA_IMAGE="eclipse-temurin:25-jre-alpine"
                # 정적 파일 서빙에 Node 는 과하다. serve 를 매 기동마다 npm 으로 깔고 있었다.
                # 이제 게이트웨이가 하던 API 프록시까지 이 nginx 가 맡는다.
                NGINX_IMAGE="nginx:1.27-alpine"

                # ── JVM 메모리 상한 ──────────────────────────────────────────────
                # 지금까지 힙 옵션도 컨테이너 메모리 제한도 없었다. 그러면 JVM 은 호스트
                # 메모리(32GB)의 4분의 1을 최대 힙으로 잡는다. 실제로 확인해 보니 세 JVM 모두
                # MaxHeapSize 가 7,960MB 였다. 셋이 동시에 크면 가용 메모리를 넘긴다.
                #
                # -Xss512k                    : 스레드 스택 기본 1MB → 절반.
                # -XX:+ExitOnOutOfMemoryError : 힙이 새면 죽고 재시작한다. 살아서 스래싱하는 것보다 낫다.
                # -XX:MaxMetaspaceSize        : 메타스페이스도 기본이 무제한이다.
                #
                # main-service 는 JPA + 배치 + 통계 집계를 전부 안고 있다. 통계가 매치를 통째로
                # 메모리에 올려 도는 구간이 있어 힙은 넉넉히 준다. GC 는 G1 그대로.
                MAIN_JVM="-Xmx512m -XX:MaxMetaspaceSize=256m -Xss512k -XX:+ExitOnOutOfMemoryError"
                MAIN_MEM="896m"

                echo "=== Step 1: Copy build artifacts ==="
                mkdir -p ${'$'}DEPLOY_DIR

                if [ "${'$'}DEPLOY_BACKEND" = "true" ]; then
                    cp backend/main-service/build/libs/*-SNAPSHOT.jar ${'$'}DEPLOY_DIR/main-service.jar
                    echo "Backend JARs 복사 완료"
                fi

                if [ "${'$'}DEPLOY_FRONTEND" = "true" ]; then
                    rm -rf ${'$'}DEPLOY_DIR/frontend-dist
                    cp -r frontend/dist ${'$'}DEPLOY_DIR/frontend-dist
                    mkdir -p ${'$'}DEPLOY_DIR/frontend-dist/downloads

                    # 사용자가 다운받는 진입점은 항상 런처(launcher-v*) MSI다.
                    # 본체(desktop-v*) MSI는 런처가 GitHub Releases에서 직접 받으므로 웹사이트에 둘 필요 없다.
                    # 파일명은 frontend(LcuPage.tsx)와의 호환을 위해 lol-collector.msi 그대로 유지한다.
                    MSI_DEPLOYED=false

                    echo "GitHub Releases에서 최신 Launcher MSI 확인 중 (launcher-v* 태그)..."
                    # GitHub의 download URL은 항상 releases/download/<tag>/<asset> 형식.
                    # URL에 /launcher-v 가 들어간 .msi만 필터링하면 본체(desktop-v*)와 안 섞인다.
                    # API는 published 내림차순이라 head -1이 가장 최신 launcher 릴리즈.
                    LAUNCHER_MSI_URL=${'$'}(curl -s https://api.github.com/repos/Park-GiJun/lol-event/releases \
                        | grep -o '"browser_download_url": *"[^"]*\/launcher-v[^"]*\.msi"' \
                        | head -1 | cut -d'"' -f4)

                    if [ -n "${'$'}LAUNCHER_MSI_URL" ]; then
                        echo "Launcher MSI 발견: ${'$'}LAUNCHER_MSI_URL"
                        curl -sL "${'$'}LAUNCHER_MSI_URL" -o ${'$'}DEPLOY_DIR/frontend-dist/downloads/lol-collector.msi
                        if [ ${'$'}? -eq 0 ] && [ -s "${'$'}DEPLOY_DIR/frontend-dist/downloads/lol-collector.msi" ]; then
                            MSI_DEPLOYED=true
                            MSI_VERSION=${'$'}(echo "${'$'}LAUNCHER_MSI_URL" | grep -oP '\d+\.\d+\.\d+' | head -1 || echo "unknown")
                            echo "Launcher MSI 다운로드 완료 (v${'$'}MSI_VERSION)"
                        fi
                    fi

                    # 폴백: 기존 배포된 파일 재사용
                    if [ "${'$'}MSI_DEPLOYED" = "false" ] && [ -f "${'$'}DEPLOY_DIR/frontend-dist/downloads/lol-collector.msi" ]; then
                        echo "기존 배포된 MSI 파일 재사용 (launcher-v* 릴리즈를 못 찾음)"
                        MSI_DEPLOYED=true
                    fi

                    if [ "${'$'}MSI_DEPLOYED" = "false" ]; then
                        echo "WARNING: Launcher MSI를 찾을 수 없음 — desktop-launcher/release.bat을 먼저 실행해 launcher-v* 릴리즈를 만드세요"
                    fi

                    echo "Frontend dist 복사 완료"
                fi

                echo "=== Step 2: Stop selected services ==="
                # 예전에 돌던 lol-eureka / lol-api-gateway / lol-lcu-service 는 이제 안 띄운다.
                # 남아 있으면 포트를 물고 있으니 무조건 내린다.
                docker stop lol-eureka lol-api-gateway lol-lcu-service 2>/dev/null || true
                docker rm   lol-eureka lol-api-gateway lol-lcu-service 2>/dev/null || true
                if [ "${'$'}DO_MAIN"     = "true" ]; then docker stop lol-main-service  2>/dev/null || true; docker rm lol-main-service  2>/dev/null || true; fi
                if [ "${'$'}DO_FRONTEND" = "true" ]; then docker stop lol-frontend      2>/dev/null || true; docker rm lol-frontend      2>/dev/null || true; fi

                echo "=== Step 3: Start Main Service ==="
                if [ "${'$'}DO_MAIN" = "true" ]; then
                    # /config 마운트가 반드시 있어야 한다.
                    # DB 접속 정보(spring.datasource)는 secrets/*.env 가 아니라
                    # HOST_CONFIG/application.yml 에 들어 있고, 예전에는 Config Server(lol-eureka)가
                    # 그걸 읽어 내려줬다. Config Server 를 걷어냈으므로 main-service 가 파일을
                    # 직접 읽어야 한다. 이 마운트를 빠뜨리면
                    #   "Failed to configure a DataSource: 'url' attribute is not specified"
                    # 로 기동이 막힌다.
                    docker run -d --name lol-main-service \
                        --network host \
                        --restart unless-stopped \
                        -m ${'$'}MAIN_MEM \
                        -v ${'$'}HOST_DEPLOY/main-service.jar:/app.jar:ro \
                        -v ${'$'}HOST_CONFIG:/config:ro \
                        --env-file /lol-event/secrets/main-service.env \
                        ${'$'}JAVA_IMAGE java ${'$'}MAIN_JVM -jar /app.jar \
                        --spring.profiles.active=prd \
                        --spring.config.additional-location=file:/config/
                else
                    echo "Main Service 배포 스킵"
                fi

                echo "=== Step 4: Start Frontend (nginx: 정적 + API 프록시) ==="
                if [ "${'$'}DO_FRONTEND" = "true" ]; then
                    # 정적 파일 서빙을 nginx 로 옮겼다. 예전에는 node:20-alpine 을 띄우고
                    # 기동할 때마다 `npm install -g serve` 를 돌렸다. 설정은 frontend/nginx.conf 에 있다.
                    cp frontend/nginx.conf ${'$'}DEPLOY_DIR/frontend-nginx.conf

                    docker run -d --name lol-frontend \
                        --network host \
                        --restart unless-stopped \
                        -m 32m \
                        -v ${'$'}HOST_DEPLOY/frontend-dist:/app:ro \
                        -v ${'$'}HOST_DEPLOY/frontend-nginx.conf:/etc/nginx/conf.d/default.conf:ro \
                        ${'$'}NGINX_IMAGE
                else
                    echo "Frontend 배포 스킵"
                fi

                echo "=== Deploy Complete ==="
                echo "Site: http://localhost:8080  (정적 + /api → main-service:8081)"
            """.trimIndent()
        }
    }

    features {
        perfmon {
        }
    }
})
