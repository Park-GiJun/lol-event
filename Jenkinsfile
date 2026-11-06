// 배포 파이프라인. master 에 올라가면 빌드 → 게이트 → 재배포까지 간다.
//
// Jenkins 는 컨테이너(ci-jenkins)로 돌고 docker.sock 으로 호스트의 도커를 부린다. 그래서 여기서
// `docker run -v <경로>` 의 <경로> 는 호스트 기준이다. 배포·비밀 디렉터리는 ~/jenkins/docker-compose.yml 에서
// 호스트와 같은 경로로 Jenkins 에 마운트해 두었으므로 같은 경로를 그대로 쓴다.
pipeline {
    agent any

    parameters {
        choice(name: 'SERVICE', choices: ['all', 'backend', 'frontend'], description: '배포할 서비스')
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    environment {
        HOST_ROOT = '/home/gijunpark/lol-event'
        DEPLOY_DIR = "${HOST_ROOT}/deploy"
        // DB 접속 정보 등 배포 환경 설정. main-service 가 /config 로 직접 읽는다.
        CONFIG_DIR = "${HOST_ROOT}/config"
        ENV_FILE = "${HOST_ROOT}/secrets/main-service.env"

        JAVA_IMAGE = 'eclipse-temurin:25-jre-alpine'
        NGINX_IMAGE = 'nginx:1.30.6-alpine'
        NODE_IMAGE = 'node:22-slim'

        MAIN_JVM = '-Xmx512m -XX:MaxMetaspaceSize=256m -Xss512k -XX:+ExitOnOutOfMemoryError'
        MAIN_MEM = '1280m'
    }

    stages {
        stage('Backend - Build') {
            when { expression { params.SERVICE != 'frontend' } }
            steps {
                dir('backend') {
                    // ktlint · 테스트 · checkErrorCodes 까지 돈다. Jenkins 가 빌드 끝에 프로세스를 정리하므로 데몬은 띄우지 않는다.
                    sh './gradlew build --no-daemon --parallel --build-cache'
                }
            }
        }

        stage('Frontend - Build') {
            when { expression { params.SERVICE != 'backend' } }
            steps {
                // Jenkins 이미지에 node 가 없어 컨테이너로 돌린다. 패키지 설치 스크립트가 Jenkins 비밀을
                // 읽지 못하도록 jenkins_home 전체가 아니라 이 잡의 워크스페이스만 넘긴다.
                sh '''
                    set -e
                    docker run --rm \
                        --user "$(id -u):$(id -g)" \
                        --mount "type=volume,src=jenkins_jenkins_home,dst=/work,volume-subpath=${WORKSPACE#/var/jenkins_home/}" \
                        -w /work/frontend \
                        -e HOME=/work/.jenkins-home \
                        -e CI=true \
                        "$NODE_IMAGE" sh -c '
                            set -e
                            mkdir -p "$HOME"
                            PNPM="npx --yes pnpm@11.9.0"
                            $PNPM install --frozen-lockfile
                            $PNPM lint
                            $PNPM test
                            $PNPM build
                        '
                '''
            }
        }

        stage('Backend - Deploy') {
            when { expression { params.SERVICE != 'frontend' } }
            steps {
                sh '''
                    set -e
                    cp backend/main-service/build/libs/*-SNAPSHOT.jar "$DEPLOY_DIR/main-service.jar.new"

                    docker rm -f lol-main-service 2>/dev/null || true
                    mv "$DEPLOY_DIR/main-service.jar.new" "$DEPLOY_DIR/main-service.jar"

                    docker run -d --name lol-main-service \
                        --network host \
                        --restart unless-stopped \
                        -m "$MAIN_MEM" \
                        -v "$DEPLOY_DIR/main-service.jar:/app.jar:ro" \
                        -v "$CONFIG_DIR:/config:ro" \
\
                        --env-file "$ENV_FILE" \
                        "$JAVA_IMAGE" java $MAIN_JVM -jar /app.jar \
                        --spring.profiles.active=prd \
                        --spring.config.additional-location=file:/config/

                    echo "main-service 기동 대기"
                    for i in $(seq 1 60); do
                        if docker exec lol-main-service wget -qO- http://127.0.0.1:8081/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
                            echo "main-service UP (${i}회째 확인)"
                            exit 0
                        fi
                        sleep 5
                    done
                    echo "main-service 가 5분 안에 UP 이 되지 않았다" >&2
                    docker logs --tail 80 lol-main-service >&2 || true
                    exit 1
                '''
            }
        }

        stage('Frontend - Deploy') {
            when { expression { params.SERVICE != 'backend' } }
            steps {
                sh '''
                    set -e
                    docker rm -f lol-frontend 2>/dev/null || true

                    rm -rf "$DEPLOY_DIR/frontend-dist"
                    cp -r frontend/dist "$DEPLOY_DIR/frontend-dist"
                    cp frontend/nginx.conf "$DEPLOY_DIR/frontend-nginx.conf"

                    docker run -d --name lol-frontend \
                        --network host \
                        --restart unless-stopped \
                        -v "$DEPLOY_DIR/frontend-dist:/app:ro" \
                        -v "$DEPLOY_DIR/frontend-nginx.conf:/etc/nginx/conf.d/default.conf:ro" \
                        "$NGINX_IMAGE"
                '''
            }
        }
    }
}
