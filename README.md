# 清屏

本地运行的 Android 开屏跳过工具，Java + 原生 Android UI，无联网权限、无第三方 SDK。最低 Android 11，优先测试 Android 16；目标真机为小米 15，HyperOS 3.0.305.0.WOCCNXM.C11。

## 功能
- 用户手动启用无障碍服务和应用开关。
- 前台包切换后 9 秒窗口内，在右上区域匹配明确跳过文本；只点击可点击的小控件，每次进入应用最多一次。
- 记录点击接受和随后控件消失，二者均不冒充确定成功拦截。
- 优先列表：百度地图、百度贴吧、淘宝、京东、美团、懂球帝。通用规则，不代表这些应用实测通过。
- 本机演示页、小米权限指引、统计清除。

## 构建
安装 JDK 17+、Android SDK Platform 35 和 Build Tools 35.0.0，在 local.properties 配置 sdk.dir，然后运行 `./gradlew assembleDebug lintDebug`。

## 规则测试
`mkdir -p test-classes && javac -d test-classes app/src/main/java/cn/qingping/app/Rules.java tests/RulesTest.java && java -cp test-classes RulesTest`

## 边界
不提供跨应用跳转拦截、摇一摇拦截或永久保活；无障碍需要用户授权，强行停止后需要手动恢复。无广告页面样本时宁可漏点，不点击含有登录、授权、安装等扩展文本的控件。初版仅支持右上角的可访问跳过按钮。系统包和敏感应用不应开启。

## 本地签名发布
设置 `ANDROID_SDK_ROOT` 后运行 `scripts/build-release.sh`。首次会在 `.signing/` 生成仅本机保存的签名密钥和随机密码，该目录不进入 Git。请自行妥善备份；丢失后无法以原签名覆盖升级。输出为 `app/build/outputs/apk/release/qingping-0.1.0.apk`。

## Android 运行测试
运行 `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :fixture:assembleDebug`，在专用 Android 16 模拟器安装三个 APK 后执行：
`adb shell am instrument -w cn.qingping.app.test/cn.qingping.app.SmokeInstrumentation`
测试会重置模拟器的无障碍服务选择，并清空测试安装的本地偏好和统计。仅在专用模拟器运行，不要在日常手机运行。fixture 是独立广告模拟应用，不包含在交付 APK 中。
