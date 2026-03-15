#!/usr/bin/env bash
# 로컬 Windows에서 빌드한 installer를 서버에 업로드
# 사용법: bash scripts/upload-installer.sh
#
# 사전 조건:
#   1. npm run dist:win 으로 release/lol-collector-setup.exe 생성
#   2. SSH 키가 서버에 등록되어 있어야 함

set -e

SERVER="gijunpark@gijun.net"
REMOTE_PATH="/home/gijunpark/lol-event/deploy/frontend-dist/downloads/lol-collector-setup.exe"
LOCAL_FILE="release/lol-collector-setup.exe"

if [ ! -f "$LOCAL_FILE" ]; then
  echo "❌ $LOCAL_FILE 없음 — 먼저 'npm run dist:win' 실행"
  exit 1
fi

echo "📦 업로드 중: $LOCAL_FILE → $SERVER:$REMOTE_PATH"
ssh "$SERVER" "mkdir -p $(dirname $REMOTE_PATH)"
scp "$LOCAL_FILE" "$SERVER:$REMOTE_PATH"
echo "✅ 업로드 완료 — https://gijun.net/downloads/lol-collector-setup.exe"
