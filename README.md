# Knowme · 替你看通知，慢慢懂你

一个**纯本地、零服务器**的 Android App：接管手机全部通知 → 用**你自己的 AI（BYOK）**消化成「今天发生了什么 + 哪些事需要你做」。

## 核心原则（不可动摇）

- **本地优先**：通知存本机 Room/SQLite，开发者全程不在数据链路里。
- **BYOK**：不绑任何模型。用户自带 key，手机直接调用其选择的 AI 端点。key 经 Android Keystore 加密存本机。
- **Android-only**：iOS 纯软件拿不到全局通知（只能靠蓝牙硬件走 ANCS），故用原生 Kotlin + Compose。

## 怎么构建运行

> 本仓库是标准 Android Studio 工程，但需要 **JDK 17 + Android SDK**（命令行环境若只有 JDK 8 无法构建）。

1. 用 **Android Studio（最新版）** 打开本目录。
2. 让它自动 sync（会下载 Gradle 8.9、AGP 8.7、SDK 35）。
3. 新建 `local.properties` 写入你的 SDK 路径（AS 通常自动生成）：
   ```
   sdk.dir=/Users/<you>/Library/Android/sdk
   ```
4. 连真机（通知监听在模拟器上也可测），点 Run。
5. App 内：①「我的」授予**通知使用权** ②「我的 → AI 服务」填你的 key → 通知开始流入「时间线」。

> 注：`gradlew` wrapper 的 jar 未入库，Android Studio 首次 sync 会自动补全；或本机有 gradle 时运行 `gradle wrapper`。

## 工程结构

```
app/src/main/java/com/knowme/app/
├─ KnowmeApp.kt              Application，持有 AppContainer
├─ AppContainer.kt           极简服务定位器（DB / 加密配置 / AI 调用）
├─ MainActivity.kt           Compose 入口
├─ notification/
│  ├─ KnowmeNotificationListener.kt   命根子：NotificationListenerService 抓取通知
│  └─ NotificationAccess.kt           权限检查 / 跳转系统设置
├─ data/db/                  Room：实体 / DAO / AppDatabase
├─ ai/                       BYOK：AiProvider 接口 + Anthropic / OpenAI兼容 实现 + 加密存储
└─ ui/                       Compose：主题 / 导航 / 5 个页面(今日·时间线·待办·问问·我的)
```

## 路线图

- [x] 工程骨架 · Material3 主题 · 底部 5 Tab
- [x] 通知监听 + 本地存储（命根子打通）
- [x] BYOK AI 层（Provider 接口 + 加密 key + 测试连接 + 问问）
- [ ] 每日早报生成（WorkManager 定时 + AI 分级/摘要/待办抽取）
- [ ] 重要性反馈（越用越懂你的本地偏好画像）
- [ ] 首次引导 · 隐私数据页（保留期/导出/清空）
