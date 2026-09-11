#!/usr/bin/env bash
set -e

echo "🔨 [1/3] 빌드 검증: assembleDebug 실행 중..."
./gradlew assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK 생성 실패: $APK_PATH 파일이 없습니다."
    exit 1
fi
echo "✅ APK 빌드 성공: $APK_PATH"

echo "📱 [2/3] adb 연결 기기 확인 중..."
DEVICE_COUNT=$(adb devices | grep -w "device" | wc -l | tr -d ' ')

if [ "$DEVICE_COUNT" -eq "0" ]; then
    echo "⚠️ [주의] 현재 adb에 연결된 활성 안드로이드 기기가 없습니다."
    echo ""
    echo "💡 문제 해결 체크리스트:"
    echo "  1. 스마트폰의 화면을 켜고 잠금을 해제하세요."
    echo "  2. USB 케이블이 제대로 꽂혀 있는지 확인하세요 (필요 시 재연결)."
    echo "  3. 상단 알림창의 'USB 설정'이 '파일 전송'으로 되어 있는지 확인하세요."
    echo "  4. '개발자 옵션 > USB 디버깅'이 활성화되어 있는지 확인하세요."
    echo "  5. adb 데몬 재시작: 'adb kill-server && adb start-server'"
    echo ""
    echo "ℹ️ APK는 이미 성공적으로 빌드되어 있으므로, 기기 연결 후 아래 명령으로 즉시 설치할 수 있습니다:"
    echo "   adb install -r $APK_PATH"
    echo "   adb shell am start -n com.aistudio.strengthlog.vpmq/com.example.MainActivity"
    exit 0
fi

echo "🚀 [3/3] 기기 감지됨 ($DEVICE_COUNT 대). APK 설치 및 앱 실행 중..."
adb install -r "$APK_PATH"
adb shell am start -n com.aistudio.strengthlog.vpmq/com.example.MainActivity
echo "🎉 앱 실행 완료!"
