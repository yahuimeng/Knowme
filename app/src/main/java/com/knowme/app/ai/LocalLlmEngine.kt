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
 * 注意底层引擎是"多轮聊天"取向：setSystemPrompt 只能在刚加载完模型后调用一次。
 * 而 Knowme 每次都是独立一问（早报/问问），故每次生成都把上下文复位到干净状态再跑，
 * 避免上一次的内容污染这一次。模型用 mmap，复位/重载很快；真正耗时的是 CPU 推理本身。
 */
class LocalLlmEngine(context: Context) {

    private val engine = AiChat.getInferenceEngine(context.applicationContext)
    private val mutex = Mutex()

    suspend fun complete(modelPath: String, systemPrompt: String, userPrompt: String): AiOutcome =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    if (!File(modelPath).exists()) error("模型文件不存在：$modelPath")

                    // 1) 等 native 异步初始化完成（解决首次调用竞态）
                    engine.state.first { it !is State.Uninitialized && it !is State.Initializing }

                    // 2) 复位到干净状态：已加载或出错都先清理（cleanUp 会回到 Initialized）
                    when (engine.state.value) {
                        is State.ModelReady, is State.Error -> engine.cleanUp()
                        else -> {}
                    }

                    // 3) 加载模型 + 设置 system（必须紧跟在 loadModel 之后）
                    engine.loadModel(modelPath)
                    if (systemPrompt.isNotBlank()) engine.setSystemPrompt(systemPrompt)

                    // 4) 发用户提问，收集 token 流
                    val text = engine.sendUserPrompt(userPrompt).toList().joinToString("").trim()
                    AiOutcome.Ok(text)
                } catch (e: Throwable) {
                    runCatching { engine.cleanUp() }
                    AiOutcome.Error("本地模型运行失败：${e.message ?: e.javaClass.simpleName}")
                }
            }
        }
}
