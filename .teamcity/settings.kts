import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.nodeJS
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
            gradleWrapperPath = "backend"
            jdkHome = "%env.JAVA_HOME%"
        }
        nodeJS {
            id = "frontend_install"
            name = "Frontend - Install Dependencies"
            workingDir = "frontend"
            shellScript = "npm ci"
        }
        nodeJS {
            id = "frontend_lint"
            name = "Frontend - Lint"
            workingDir = "frontend"
            shellScript = "npm run lint"
        }
        nodeJS {
            id = "frontend_build"
            name = "Frontend - Build"
            workingDir = "frontend"
            shellScript = "npm run build"
        }
        nodeJS {
            id = "lcu_install"
            name = "LCU Service - Install Dependencies"
            workingDir = "backend/lcu-service"
            shellScript = "npm ci"
        }
        nodeJS {
            id = "lcu_build"
            name = "LCU Service - Build"
            workingDir = "backend/lcu-service"
            shellScript = "npm run build"
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
