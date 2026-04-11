import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

version = "2025.11"

project {
    buildType(Build)
}

object Build : BuildType({
    name = "Build"

    artifactRules = """
        backend/eureka-server/build/libs/*.jar => jars/
        backend/api-gateway/build/libs/*.jar => jars/
        backend/main-service/build/libs/*.jar => jars/
        frontend/dist/** => frontend-dist/
        backend/lcu-service/dist/** => lcu-service-dist/
        backend/lcu-service/package.json => lcu-service-dist/
        desktop-collector/build/compose/binaries/main/msi/*.msi => desktop-collector-dist/
    """.trimIndent()

    params {
        // 빌드 항목
        checkbox("build.backend", "true",
            label = "[빌드] Backend (Gradle)", description = "eureka-server, api-gateway, main-service JAR 빌드",
            checked = "true", unchecked = "false")
        checkbox("build.frontend", "true",
            label = "[빌드] Frontend", description = "React 앱 빌드",
            checked = "true", unchecked = "false")
        checkbox("build.lcu", "true",
            label = "[빌드] LCU Service", description = "Node.js LCU 서비스 빌드",
            checked = "true", unchecked = "false")
        checkbox("build.desktop", "false",
            label = "[빌드] Desktop Collector", description = "Compose Desktop 수집기 MSI 빌드",
            checked = "true", unchecked = "false")

        // 배포 항목
        checkbox("deploy.eureka", "true",
            label = "[배포] Eureka Server", description = "Eureka / Config Server 재배포",
            checked = "true", unchecked = "false")
        checkbox("deploy.main", "true",
            label = "[배포] Main Service", description = "main-service 재배포",
            checked = "true", unchecked = "false")
        checkbox("deploy.gateway", "true",
            label = "[배포] API Gateway", description = "api-gateway 재배포",
            checked = "true", unchecked = "false")
        checkbox("deploy.frontend", "true",
            label = "[배포] Frontend", description = "프론트엔드 정적 파일 재배포",
            checked = "true", unchecked = "false")
        checkbox("deploy.lcu", "true",
            label = "[배포] LCU Service", description = "lcu-service 재배포",
            checked = "true", unchecked = "false")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            id = "backend_build"
            name = "Backend - Gradle Build"
            tasks = "clean build -x test"
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
            scriptContent = """
                npm ci
                npm run lint
                npm run build
            """.trimIndent()
            conditions {
                equals("build.frontend", "true")
            }
        }
        script {
            id = "lcu_build"
            name = "LCU Service - Install & Build"
            workingDir = "backend/lcu-service"
            scriptContent = """
                npm ci
                npm run build
                npm prune --production
            """.trimIndent()
            conditions {
                equals("build.lcu", "true")
            }
        }
        gradle {
            id = "desktop_build"
            name = "Desktop Collector - Package MSI"
            tasks = "packageMsi"
            workingDir = "desktop-collector"
            gradleWrapperPath = ""
            jdkHome = "%env.JAVA_HOME%"
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
                DEPLOY_LCU="%build.lcu%"
                DEPLOY_DESKTOP="%build.desktop%"
                DO_EUREKA="%deploy.eureka%"
                DO_MAIN="%deploy.main%"
                DO_GATEWAY="%deploy.gateway%"
                DO_FRONTEND="%deploy.frontend%"
                DO_LCU="%deploy.lcu%"

                DEPLOY_DIR="/lol-event/deploy"
                HOST_DEPLOY="/home/gijunpark/lol-event/deploy"
                HOST_CONFIG="/home/gijunpark/lol-event/config"
                JAVA_IMAGE="eclipse-temurin:25-jdk-alpine"
                NODE_IMAGE="node:20-alpine"

                echo "=== Step 1: Copy build artifacts ==="
                mkdir -p ${'$'}DEPLOY_DIR

                if [ "${'$'}DEPLOY_BACKEND" = "true" ]; then
                    cp backend/eureka-server/build/libs/*-SNAPSHOT.jar ${'$'}DEPLOY_DIR/eureka-server.jar
                    cp backend/api-gateway/build/libs/*-SNAPSHOT.jar ${'$'}DEPLOY_DIR/api-gateway.jar
                    cp backend/main-service/build/libs/*-SNAPSHOT.jar ${'$'}DEPLOY_DIR/main-service.jar
                    echo "Backend JARs 복사 완료"
                fi

                if [ "${'$'}DEPLOY_FRONTEND" = "true" ]; then
                    rm -rf ${'$'}DEPLOY_DIR/frontend-dist
                    cp -r frontend/dist ${'$'}DEPLOY_DIR/frontend-dist
                    mkdir -p ${'$'}DEPLOY_DIR/frontend-dist/downloads

                    # Desktop Collector MSI → 프론트엔드 다운로드 경로
                    if ls desktop-collector/build/compose/binaries/main/msi/*.msi 1>/dev/null 2>&1; then
                        cp desktop-collector/build/compose/binaries/main/msi/*.msi ${'$'}DEPLOY_DIR/frontend-dist/downloads/lol-collector.msi
                        echo "Desktop Collector MSI 배포 완료"
                    elif [ "${'$'}DEPLOY_DESKTOP" = "true" ]; then
                        echo "WARNING: Desktop 빌드 활성화됐지만 MSI 파일 없음"
                    fi

                    echo "Frontend dist 복사 완료"
                fi

                if [ "${'$'}DEPLOY_LCU" = "true" ]; then
                    mkdir -p ${'$'}DEPLOY_DIR/lcu-service
                    cp -r backend/lcu-service/dist ${'$'}DEPLOY_DIR/lcu-service/dist
                    cp backend/lcu-service/package.json ${'$'}DEPLOY_DIR/lcu-service/
                    cp backend/lcu-service/package-lock.json ${'$'}DEPLOY_DIR/lcu-service/ 2>/dev/null || true
                    echo "LCU Service 복사 완료"
                fi

                echo "=== Step 2: Stop selected services ==="
                if [ "${'$'}DO_EUREKA"   = "true" ]; then docker stop lol-eureka        2>/dev/null || true; docker rm lol-eureka        2>/dev/null || true; fi
                if [ "${'$'}DO_MAIN"     = "true" ]; then docker stop lol-main-service  2>/dev/null || true; docker rm lol-main-service  2>/dev/null || true; fi
                if [ "${'$'}DO_GATEWAY"  = "true" ]; then docker stop lol-api-gateway   2>/dev/null || true; docker rm lol-api-gateway   2>/dev/null || true; fi
                if [ "${'$'}DO_FRONTEND" = "true" ]; then docker stop lol-frontend      2>/dev/null || true; docker rm lol-frontend      2>/dev/null || true; fi
                if [ "${'$'}DO_LCU"      = "true" ]; then docker stop lol-lcu-service   2>/dev/null || true; docker rm lol-lcu-service   2>/dev/null || true; fi

                echo "=== Step 3: Start Eureka Server (Config Server) ==="
                if [ "${'$'}DO_EUREKA" = "true" ]; then
                    docker run -d --name lol-eureka \
                        --network host \
                        --restart unless-stopped \
                        -v ${'$'}HOST_DEPLOY/eureka-server.jar:/app.jar:ro \
                        -v ${'$'}HOST_CONFIG:/config:ro \
                        --env-file /lol-event/secrets/shared.env \
                        ${'$'}JAVA_IMAGE java -jar /app.jar \
                        --spring.cloud.config.server.native.search-locations=classpath:/config,file:/config

                    echo "Waiting for Eureka to start..."
                    for i in ${'$'}(seq 1 30); do
                        if curl -sf http://localhost:8761/actuator/health > /dev/null 2>&1; then
                            echo "Eureka is UP!"
                            break
                        fi
                        echo "  waiting... (${'$'}i/30)"
                        sleep 3
                    done
                else
                    echo "Eureka 배포 스킵"
                fi

                echo "=== Step 4: Start Main Service ==="
                if [ "${'$'}DO_MAIN" = "true" ]; then
                    docker run -d --name lol-main-service \
                        --network host \
                        --restart unless-stopped \
                        -v ${'$'}HOST_DEPLOY/main-service.jar:/app.jar:ro \
                        --env-file /lol-event/secrets/main-service.env \
                        ${'$'}JAVA_IMAGE java -jar /app.jar \
                        --spring.profiles.active=prd
                else
                    echo "Main Service 배포 스킵"
                fi

                echo "=== Step 5: Start API Gateway ==="
                if [ "${'$'}DO_GATEWAY" = "true" ]; then
                    docker run -d --name lol-api-gateway \
                        --network host \
                        --restart unless-stopped \
                        -v ${'$'}HOST_DEPLOY/api-gateway.jar:/app.jar:ro \
                        --env-file /lol-event/secrets/shared.env \
                        --env LCU_SERVICE_URL=http://localhost:3002 \
                        ${'$'}JAVA_IMAGE java -jar /app.jar \
                        --spring.profiles.active=local
                else
                    echo "API Gateway 배포 스킵"
                fi

                echo "=== Step 6: Start Frontend ==="
                if [ "${'$'}DO_FRONTEND" = "true" ]; then
                    docker run -d --name lol-frontend \
                        --network host \
                        --restart unless-stopped \
                        -v ${'$'}HOST_DEPLOY/frontend-dist:/app:ro \
                        ${'$'}NODE_IMAGE sh -c "npm install -g serve && serve -s /app -l 8080"
                else
                    echo "Frontend 배포 스킵"
                fi

                echo "=== Step 7: Install lcu-service deps & Start ==="
                if [ "${'$'}DO_LCU" = "true" ]; then
                    docker run --rm \
                        -v ${'$'}HOST_DEPLOY/lcu-service:/app \
                        ${'$'}NODE_IMAGE sh -c "cd /app && npm ci --production"
                    docker run -d --name lol-lcu-service \
                        --network host \
                        --restart unless-stopped \
                        -v ${'$'}HOST_DEPLOY/lcu-service:/app:ro \
                        --env-file /lol-event/secrets/shared.env \
                        --env PORT=3002 \
                        --env KAFKA_BROKERS=localhost:9094 \
                        ${'$'}NODE_IMAGE sh -c "node /app/dist/main"
                else
                    echo "LCU Service 배포 스킵"
                fi

                echo "=== Deploy Complete ==="
                echo "Frontend:    http://localhost:8080"
                echo "API Gateway: http://localhost:9832"
                echo "Eureka:      http://localhost:8761"
            """.trimIndent()
        }
    }

    features {
        perfmon {
        }
    }
})
