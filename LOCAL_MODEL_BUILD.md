# 内嵌本地模型（llama.cpp）构建手册

本功能把 **llama.cpp** 编进 App（`:llamalib` 模块 + `third_party/llama.cpp` 子模块），
支持加载 **.gguf** 模型（Qwen 等中文模型），完全离线、零 token。

> ⚠️ 含 C++ native 代码，**必须在 Android Studio 里构建**（需 NDK/CMake）。
> 命令行/无 NDK 环境无法编译。

## 一次性准备

1. **拉取子模块源码**（项目根目录终端）：
   ```
   git submodule update --init --recursive
   ```
   （会下载 llama.cpp 源码到 `third_party/llama.cpp`）

2. **Android Studio → SDK Manager → SDK Tools**（勾选右下角 "Show Package Details"）安装：
   - **NDK (Side by side) `29.0.13113456`**
   - **CMake `3.31.6`**（若列表没有 3.31.6，装最新的 3.31.x，并把 `llamalib/build.gradle.kts` 里 `version = "3.31.6"` 改成你装的版本）

3. 若弹出 NDK license 提示，点接受。

## 构建运行

4. **Sync** Gradle → **Build**。
   - 第一次会编译整个 llama.cpp，**耗时几分钟**，属正常。
5. **连真机运行**（仅打包 arm64-v8a，模拟器跑不了）。

## 用起来

6. App 内：**我的 → AI 服务 → 添加新服务 → 选「本地模型」**
7. 下载一个 .gguf（**直链 URL**），例如 Qwen2.5 3B（非 gated，可直下）：
   - 文件名：`qwen2.5-3b-instruct-q4_k_m.gguf`
   - URL：`https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf`
8. 下完 → 在上方选中该模型 → 点「测试连接」→ ✅ → 设为使用中。
9. 之后早报/问问全离线走本地模型。

## 出问题怎么办

- **构建报错**：把 Build 面板第一条红错贴出来，多半是 NDK/CMake 版本或子模块没拉。
- **推理失败/崩溃**：多为机型内存不足（3B 需 ~3GB 空闲）或模型格式问题，换更小模型（1.5B）试。
- 想回云端：多档案随时切换，不影响。

## 结构

```
third_party/llama.cpp        llama.cpp 源码（git 子模块）
llamalib/                    JNI 库模块(com.arm.aichat)，CMake 编译 llama.cpp
  src/main/cpp/CMakeLists.txt  指向 ../../../../third_party/llama.cpp
  src/main/cpp/ai_chat.cpp     JNI 实现
app/.../ai/LocalLlmEngine.kt 调用 AiChat 接口，接进 AiProvider 抽象
```
