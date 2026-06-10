package com.knowme.app.ai

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 端侧推理引擎：基于内嵌的 llama.cpp（llamalib / com.arm.aichat），支持 .gguf。
 *
 * 模型**常驻**：首次加载后一直留在内存，后续调用直接复用（不再反复加载）。
 * 每次生成只重设 system（底层会清空上下文 KV cache），保证"独立一问"互不污染。
 * 用户可在设置里「停止」以释放内存，或进程退出时随之释放。
 */
class LocalLlmEngine(context: Context) {

    private val engine = AiChat.getInferenceEngine(context.applicationContext)
    private val mutex = Mutex()
    private var loadedPath: String? = null

    suspend fun complete(modelPath: String, systemPrompt: String, userPrompt: String): AiOutcome =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    if (!File(modelPath).exists()) error("模型文件不存在：$modelPath")

                    // 等 native 异步初始化完成（解决首次调用竞态）
                    engine.state.first { it !is State.Uninitialized && it !is State.Initializing }
                    if (engine.state.value is State.Error) {
                        engine.cleanUp(); loadedPath = null
                    }

                    // 常驻：仅在未加载 / 换了模型时才加载
                    val needLoad = loadedPath != modelPath || engine.state.value !is State.ModelReady
                    if (needLoad) {
                        if (engine.state.value is State.ModelReady) engine.cleanUp()
                        engine.loadModel(modelPath)
                        loadedPath = modelPath
                    }

                    // 每次重设 system → 底层清空上下文，保证本次独立
                    if (systemPrompt.isNotBlank()) engine.setSystemPrompt(systemPrompt)

                    val text = engine.sendUserPrompt(userPrompt).toList().joinToString("").trim()
                    AiOutcome.Ok(text)
                } catch (e: Throwable) {
                    loadedPath = null
                    runCatching { engine.cleanUp() }
                    AiOutcome.Error("本地模型运行失败：${e.message ?: e.javaClass.simpleName}")
                }
            }
        }

    /** 手动停止：卸载模型、释放内存（设置里「停止」按钮调用）。 */
    fun stop() {
        loadedPath = null
        runCatching { engine.cleanUp() }
    }

    /** 当前是否有模型常驻在内存。 */
    fun isLoaded(): Boolean = loadedPath != null && engine.state.value is State.ModelReady
}
