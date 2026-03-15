import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.nodeJS
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

version = "2025.11"

project {
    description = "LoL Event - League of Legends Event Management Platform"

    vcsRoot(LolEventVcsRoot)

    buildType(BackendBuild)
    buildType(FrontendBuild)
    buildType(LcuServiceBuild)
    buildType(FullBuild)

    buildTypesOrder = arrayListOf(FullBuild, BackendBuild, FrontendBuild, LcuServiceBuild)

    params {
        param("env.JAVA_HOME", "/usr/lib/jvm/jdk-25")
    }
}

// =============================================================================
// VCS Root
// =============================================================================
object LolEventVcsRoot : GitVcsRoot({
    name = "lol-event"
    url = "file:///lol-event"
    branch = "refs/heads/master"
    branchSpec = "+:refs/heads/*"
    pollInterval = 30
})

// =============================================================================
// Backend Build (Gradle multi-module: common, eureka-server, api-gateway, main-service)
// =============================================================================
object BackendBuild : BuildType({
    name = "Backend Build"
    description = "Kotlin/Spring Boot backend - Gradle multi-module build"

    artifactRules = """
        backend/eureka-server/build/libs/*.jar => jars/
        backend/api-gateway/build/libs/*.jar => jars/
        backend/main-service/build/libs/*.jar => jars/
    """.trimIndent()

    vcs {
        root(LolEventVcsRoot)
    }

    steps {
        gradle {
            name = "Clean Build"
            tasks = "clean build -x test"
            workingDir = "backend"
            gradleWrapperPath = "backend"
            jdkHome = "%env.JAVA_HOME%"
        }
    }

    triggers {
        vcs {
            triggerRules = "+:backend/**"
            branchFilter = "+:*"
        }
    }

    features {
        perfmon {}
    }
})

// =============================================================================
// Frontend Build (React + TypeScript + Vite)
// =============================================================================
object FrontendBuild : BuildType({
    name = "Frontend Build"
    description = "React/TypeScript frontend - Vite build"

    artifactRules = "frontend/dist/** => frontend-dist/"

    vcs {
        root(LolEventVcsRoot)
    }

    steps {
        nodeJS {
            name = "Install Dependencies"
            shellScript = "npm ci"
            workingDir = "frontend"
        }
        nodeJS {
            name = "Lint"
            shellScript = "npm run lint"
            workingDir = "frontend"
        }
        nodeJS {
            name = "Build"
            shellScript = "npm run build"
            workingDir = "frontend"
        }
    }

    triggers {
        vcs {
            triggerRules = "+:frontend/**"
            branchFilter = "+:*"
        }
    }

    features {
        perfmon {}
    }
})

// =============================================================================
// LCU Service Build (NestJS + TypeScript)
// =============================================================================
object LcuServiceBuild : BuildType({
    name = "LCU Service Build"
    description = "NestJS LCU match collection service build"

    artifactRules = "backend/lcu-service/dist/** => lcu-service-dist/"

    vcs {
        root(LolEventVcsRoot)
    }

    steps {
        nodeJS {
            name = "Install Dependencies"
            shellScript = "npm ci"
            workingDir = "backend/lcu-service"
        }
        nodeJS {
            name = "Build"
            shellScript = "npm run build"
            workingDir = "backend/lcu-service"
        }
    }

    triggers {
        vcs {
            triggerRules = "+:backend/lcu-service/**"
            branchFilter = "+:*"
        }
    }

    features {
        perfmon {}
    }
})

// =============================================================================
// Full Build (all components)
// =============================================================================
object FullBuild : BuildType({
    name = "Full Build"
    description = "Build all components: Backend + Frontend + LCU Service"

    vcs {
        root(LolEventVcsRoot)
    }

    steps {
        gradle {
            name = "Backend - Clean Build"
            tasks = "clean build -x test"
            workingDir = "backend"
            gradleWrapperPath = "backend"
            jdkHome = "%env.JAVA_HOME%"
        }
        nodeJS {
            name = "Frontend - Install & Build"
            shellScript = """
                npm ci
                npm run build
            """.trimIndent()
            workingDir = "frontend"
        }
        nodeJS {
            name = "LCU Service - Install & Build"
            shellScript = """
                npm ci
                npm run build
            """.trimIndent()
            workingDir = "backend/lcu-service"
        }
    }

    triggers {
        vcs {
            branchFilter = "+:master"
        }
    }

    features {
        perfmon {}
    }
})
