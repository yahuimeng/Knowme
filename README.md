<div align="center">

# Knowme

**替你看通知，慢慢懂你**

纯本地 · 零服务器 · BYOK 的 Android 通知消化助手
接管手机全部通知 → 用**你自己的 AI** 消化成「今天哪些要紧 + 该做什么」

![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-29-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)

</div>

---

## 这是什么

每天几百条通知里，真正要紧的可能就三五条，其余是促销、群闲聊、系统打扰。**Knowme** 接管手机全部通知，交给你自己配置的 AI 消化成一份早报：

- 🔴 **要紧** —— 真人找你、需你亲自处理的
- 🟡 **留意** —— 知道一下就行（快递 / 账单 / 验证码 / 日程）
- ⚪️ **噪音** —— 自动折叠（促销 / 砍价 / 推送）

并从中抽出真正需要你动手的**待办**。所有数据只在你手机里，开发者全程不在数据链路上。

## ✨ 特性

- **🔒 纯本地，零服务器** —— 通知存本机 Room/SQLite，不上传任何数据，开发者看不到。
- **🔑 BYOK（自带 Key）** —— 不绑任何模型。填你自己的 Claude / OpenAI 兼容端点，key 经 Android Keystore 加密存本机。支持多套配置切换。
- **📴 可选端侧模型** —— 内嵌 llama.cpp，下载 GGUF 模型后**完全离线**消化，一个字节都不出设备。
- **🧠 越用越懂你** —— 从你的日常行为（点开 / 划走 / 展开）**被动学习**你在乎什么，三档越来越贴合本人。零配置、可在「我的」查看与重置。
- **📊 按生活来源排重要性** —— 真人通讯（微信/QQ/短信/钉钉/飞书…）优先，群里 @我 一律最高，营销游戏自动沉底。
- **💬 问问** —— 用你的 AI 追问本地通知库（「今天王总说了啥」），多轮对话、历史留存。
- **🛡 隐私可控** —— 可选监听哪些 App、数据保留期自动清理、一键清空全部。

## 🧠 它怎么工作

```
手机通知  ──▶  NotificationListenerService 抓取  ──▶  本地 Room 落库
                                                        │
                          本地规则预筛（明显噪音直接折叠，省 token）
                                                        │
                            你的 AI（云端或端侧）消化分级 + 抽待办
                                                        │
              确定性兜底（@我→要紧、真人通讯不当噪音、常关注来源保底）
                                                        │
                        今日早报：🔴要紧 / 🟡留意 / ⚪️噪音 + 待办
```

「越用越懂你」在背后按"来源"聚合你的互动信号（点开/展开=在乎、手动划走=不在乎，一键清空不计），形成本地偏好画像，再注入分类提示词 + 安全兜底——**只会让判断更准，永不自己把消息藏掉**。

## 🔒 隐私

- 通知原文、偏好画像、AI key **全部只存在本机**，App 不含任何上报/统计 SDK。
- 唯一的对外请求，是你的手机**直接**调用你自己填的 AI 端点（BYOK）；选端侧模型则完全离线。
- 数据可设保留期自动清理，或一键清空。

## 🚀 构建运行

**环境要求**

- Android Studio（最新版）+ **JDK 17**
- Android SDK 35
- 若要编译端侧模型模块（`:llamalib`）：**NDK `29.0.13113456`** + **CMake `3.31.6`**（在 SDK Manager 安装）

**步骤**

```bash
# 1. 带子模块克隆（端侧推理依赖 llama.cpp 子模块）
git clone --recursive https://github.com/yahuimeng/Knowme.git
# 已经克隆过则补拉子模块：
git submodule update --init --recursive
```

2. 用 **Android Studio 打开仓库根目录**（不是 `app/` 子目录）。
3. 等待自动 sync（Gradle 8.7 / AGP 8.6.1 / Kotlin 2.0.21）。`local.properties` 通常由 AS 自动生成，内容形如 `sdk.dir=/Users/<you>/Library/Android/sdk`。
4. 连真机，点 **Run**。
5. App 内：① 授予**通知使用权** ②「我的 → AI 服务」填你的 key（或下载端侧模型）→ 通知开始流入。

> 不需要端侧模型时，可在 `settings.gradle.kts` 去掉 `:llamalib` 模块，即可免装 NDK/CMake 直接构建。

## 🗂 工程结构

```
app/src/main/java/com/knowme/app/
├─ KnowmeApp.kt              Application，持有 AppContainer
├─ AppContainer.kt           极简服务定位器（DB / 加密配置 / AI 调用 / 偏好学习）
├─ notification/             NotificationListenerService 抓取与权限
├─ data/db/                  Room：实体 / DAO / 迁移
├─ digest/                   早报生成 + 本地规则分类
├─ learn/                    「越用越懂你」被动学习器
├─ ai/                       BYOK：AiProvider 接口 + Anthropic / OpenAI兼容 / 端侧引擎
└─ ui/                       Compose：主题 / 导航 / 5 个页面
llamalib/                    内嵌 llama.cpp 的端侧推理模块
third_party/llama.cpp        llama.cpp 子模块
```

## 🛠 技术栈

Kotlin · Jetpack Compose · Material 3 · Room · Coroutines/Flow · WorkManager · Ktor（AI HTTP）· llama.cpp（端侧）· Haze（磨砂玻璃）

## 🗺 路线图

- [x] 通知监听 + 本地存储
- [x] BYOK AI 层（云端 Provider + 加密 key + 端侧 llama.cpp）
- [x] 每日早报（三档分级 / 摘要 / 待办抽取 / 定时 + 手动）
- [x] 按生活来源排重要性 · 群聊 @我 优先
- [x] 越用越懂你 v1（被动学习 + 可查看可重置）
- [ ] 私聊 / 群聊精细区分
- [ ] 主动纠正入口（长按调档）· 偏好画像摘要
- [ ] 每周复盘

## 📄 许可证

[MIT](LICENSE) © 2026 yahuimeng

> 内嵌的 [llama.cpp](https://github.com/ggml-org/llama.cpp) 为其各自的 MIT 许可证。
