package com.knowme.app.ai

import android.content.Context
import com.arm.aichat.AiChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 端侧推理引擎：基于内嵌的 llama.cpp（llamalib / com.arm.aichat），
 * 支持 .gguf 模型（Qwen 等中文模型），完全离线、不联网、零 token 费。
 * 模型加载较重，按路径缓存复用。
 */
class LocalLlmEngine(context: Context) {

    private val engine = AiChat.getInferenceEngine(context.applicationContext)
    private val mutex = Mutex()
    private var loadedPath: String? = null

    /** 单轮对话。system 与 user 分别下发，收集生成的 token 流为完整文本。 */
    suspend fun complete(modelPath: String, systemPrompt: String, userPrompt: String): AiOutcome =
        withContext(Dispatchers.Default) {
            try {
                if (!File(modelPath).exists()) error("模型文件不存在：$modelPath")
                mutex.withLock {
                    if (loadedPath != modelPath) {
                        engine.loadModel(modelPath)   // 架构不支持会抛 UnsupportedArchitectureException
                        loadedPath = modelPath
                    }
                    engine.setSystemPrompt(systemPrompt)
                }
                val text = engine.sendUserPrompt(userPrompt).toList().joinToString("").trim()
                AiOutcome.Ok(text)
            } catch (e: Throwable) {
                loadedPath = null
                runCatching { engine.cleanUp() }
                AiOutcome.Error("本地模型运行失败：${e.message ?: e.javaClass.simpleName}。可能机型不支持、内存不足或模型格式不符。")
            }
        }
}
