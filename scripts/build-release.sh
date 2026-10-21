#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to your Android SDK}"
"${GRADLE_BIN:-./gradlew}" :app:assembleRelease :app:lintRelease
mkdir -p .signing
chmod 700 .signing
if [ ! -f .signing/release.jks ]; then
  umask 077
  openssl rand -hex 32 > .signing/password
  keytool -genkeypair -keystore .signing/release.jks -storepass:file .signing/password -keypass:file .signing/password -alias qingping -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=QingPing Local' -storetype PKCS12
fi
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" sign --ks .signing/release.jks --ks-key-alias qingping --ks-pass file:.signing/password --out app/build/outputs/apk/release/qingping-0.1.0.apk app/build/outputs/apk/release/app-release-unsigned.apk
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify --verbose app/build/outputs/apk/release/qingping-0.1.0.apk
