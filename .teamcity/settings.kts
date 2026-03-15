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

    // node_modules 제외 — artifact 1000개 제한 때문
    artifactRules = """
        backend/eureka-server/build/libs/*.jar => jars/
        backend/api-gateway/build/libs/*.jar => jars/
        backend/main-service/build/libs/*.jar => jars/
        frontend/dist/** => frontend-dist/
        backend/lcu-service/dist/** => lcu-service-dist/
        backend/lcu-service/package.json => lcu-service-dist/
        electron-collector/dist/** => electron-collector-dist/
    """.trimIndent()

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
        }
        script {
            id = "electron_build"
            name = "Electron Collector - TypeScript Check"
            workingDir = "electron-collector"
            scriptContent = """
                ELECTRON_SKIP_BINARY_DOWNLOAD=1 npm ci
                npm run build
            """.trimIndent()
        }
        script {
            id = "deploy"
            name = "Deploy - Services to Host"
            scriptContent = """
                #!/bin/bash
                set -e

                DEPLOY_DIR="/lol-event/deploy"
                HOST_DEPLOY="/home/gijunpark/lol-event/deploy"
                HOST_CONFIG="/home/gijunpark/lol-event/config"
                JAVA_IMAGE="eclipse-temurin:25-jdk-alpine"
                NODE_IMAGE="node:20-alpine"

                echo "=== Step 1: Copy build artifacts ==="
                mkdir -p ${'$'}DEPLOY_DIR
                cp backend/eureka-server/build/libs/*-SNAPSHOT.jar ${'$'}DEPLOY_DIR/eureka-server.jar
                cp backend/api-gateway/build/libs/*-SNAPSHOT.jar ${'$'}DEPLOY_DIR/api-gateway.jar
                cp backend/main-service/build/libs/*-SNAPSHOT.jar ${'$'}DEPLOY_DIR/main-service.jar
                rm -rf ${'$'}DEPLOY_DIR/frontend-dist
                cp -r frontend/dist ${'$'}DEPLOY_DIR/frontend-dist

                # installer/ 에 exe 있으면 downloads 에 배포
                mkdir -p ${'$'}DEPLOY_DIR/frontend-dist/downloads
                if ls installer/*.exe 1>/dev/null 2>&1; then
                    cp installer/*.exe ${'$'}DEPLOY_DIR/frontend-dist/downloads/
                    cp installer/latest.yml ${'$'}DEPLOY_DIR/frontend-dist/downloads/ 2>/dev/null || true
                    cp installer/*.blockmap ${'$'}DEPLOY_DIR/frontend-dist/downloads/ 2>/dev/null || true
                    echo "installer 배포 완료"
                fi

                echo "=== Step 1-b: Copy lcu-service artifacts ==="
                mkdir -p ${'$'}DEPLOY_DIR/lcu-service
                cp -r backend/lcu-service/dist ${'$'}DEPLOY_DIR/lcu-service/dist
                cp backend/lcu-service/package.json ${'$'}DEPLOY_DIR/lcu-service/
                cp backend/lcu-service/package-lock.json ${'$'}DEPLOY_DIR/lcu-service/ 2>/dev/null || true

                echo "=== Step 2: Stop existing services ==="
                docker stop lol-frontend lol-api-gateway lol-main-service lol-eureka lol-lcu-service 2>/dev/null || true
                docker rm lol-frontend lol-api-gateway lol-main-service lol-eureka lol-lcu-service 2>/dev/null || true

                echo "=== Step 3: Start Eureka Server (Config Server) ==="
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

                echo "=== Step 4: Start Main Service ==="
                docker run -d --name lol-main-service \
                    --network host \
                    --restart unless-stopped \
                    -v ${'$'}HOST_DEPLOY/main-service.jar:/app.jar:ro \
                    --env-file /lol-event/secrets/main-service.env \
                    ${'$'}JAVA_IMAGE java -jar /app.jar \
                    --spring.profiles.active=prd

                echo "=== Step 5: Start API Gateway ==="
                docker run -d --name lol-api-gateway \
                    --network host \
                    --restart unless-stopped \
                    -v ${'$'}HOST_DEPLOY/api-gateway.jar:/app.jar:ro \
                    --env-file /lol-event/secrets/shared.env \
                    --env LCU_SERVICE_URL=http://localhost:3002 \
                    ${'$'}JAVA_IMAGE java -jar /app.jar \
                    --spring.profiles.active=local

                echo "=== Step 6: Start Frontend ==="
                docker run -d --name lol-frontend \
                    --network host \
                    --restart unless-stopped \
                    -v ${'$'}HOST_DEPLOY/frontend-dist:/app:ro \
                    ${'$'}NODE_IMAGE sh -c "npm install -g serve && serve -s /app -l 8080"

                echo "=== Step 7: Install lcu-service deps & Start ==="
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

                echo "=== Deploy Complete ==="
                echo "Frontend:    http://localhost:8080"
                echo "API Gateway: http://localhost:9832"
                echo "Eureka:      http://localhost:8761"
            """.trimIndent()
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
    }
})
