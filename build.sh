#!/bin/bash
set -e

cd "$(dirname "$0")"

./gradlew clean assembleRelease

echo ""
echo "빌드 완료. AAR 위치: build/outputs/aar/"
