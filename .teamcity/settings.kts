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

    artifactRules = """
        backend/eureka-server/build/libs/*.jar => jars/
        backend/api-gateway/build/libs/*.jar => jars/
        backend/main-service/build/libs/*.jar => jars/
        frontend/dist/** => frontend-dist/
        backend/lcu-service/dist/** => lcu-service-dist/
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

                echo "=== Step 2: Stop existing services ==="
                docker stop lol-frontend lol-api-gateway lol-main-service lol-eureka 2>/dev/null || true
                docker rm lol-frontend lol-api-gateway lol-main-service lol-eureka 2>/dev/null || true

                echo "=== Step 3: Start Eureka Server (Config Server) ==="
                docker run -d --name lol-eureka \
                    --network host \
                    --restart unless-stopped \
                    -v ${'$'}HOST_DEPLOY/eureka-server.jar:/app.jar:ro \
                    -v ${'$'}HOST_CONFIG:/config:ro \
                    --env-file /home/gijunpark/secrets/shared.env \
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
                    --env-file /home/gijunpark/secrets/main-service.env \
                    ${'$'}JAVA_IMAGE java -jar /app.jar \
                    --spring.profiles.active=prd

                echo "=== Step 5: Start API Gateway ==="
                docker run -d --name lol-api-gateway \
                    --network host \
                    --restart unless-stopped \
                    -v ${'$'}HOST_DEPLOY/api-gateway.jar:/app.jar:ro \
                    --env-file /home/gijunpark/secrets/shared.env \
                    ${'$'}JAVA_IMAGE java -jar /app.jar \
                    --spring.profiles.active=local

                echo "=== Step 6: Start Frontend ==="
                docker run -d --name lol-frontend \
                    --network host \
                    --restart unless-stopped \
                    -v ${'$'}HOST_DEPLOY/frontend-dist:/app:ro \
                    ${'$'}NODE_IMAGE sh -c "npm install -g serve && serve -s /app -l 8080"

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
