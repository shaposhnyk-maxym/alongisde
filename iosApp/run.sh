#!/usr/bin/env bash
# Build + install + run Alongside on iOS Simulator, no Xcode GUI needed.
# Usage: ./run.sh ["iPhone 17"]
set -euo pipefail

DEVICE="${1:-iPhone 17}"
cd "$(dirname "$0")"

echo "==> xcodegen generate"
xcodegen generate

echo "==> xcodebuild (Debug, iOS Simulator)"
xcodebuild -project Alongside.xcodeproj -scheme Alongside \
    -destination "platform=iOS Simulator,name=$DEVICE" \
    -configuration Debug build CODE_SIGNING_ALLOWED=NO

if ! xcrun simctl list devices booted | grep -q "$DEVICE"; then
    echo "==> booting $DEVICE"
    xcrun simctl boot "$DEVICE"
fi

APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/Alongside-*/Build/Products/Debug-iphonesimulator \
    -maxdepth 1 -iname "Alongside.app" | head -1)

echo "==> install + launch"
xcrun simctl install booted "$APP_PATH"
xcrun simctl terminate booted com.alongside.app 2>/dev/null || true
xcrun simctl launch --console booted com.alongside.app
