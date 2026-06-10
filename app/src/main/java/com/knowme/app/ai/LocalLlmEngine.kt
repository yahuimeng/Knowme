package com.knowme.app.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 端侧推理引擎：基于 MediaPipe LLM Inference（LiteRT），完全离线、不联网、零 token 费。
 * 模型文件由用户在 App 内下载，按路径加载。加载较重，故按模型路径缓存复用。
 */
class LocalLlmEngine(private val context: Context) {

    private val mutex = Mutex()
    private var loadedPath: String? = null
    private var engine: LlmInference? = null

    private suspend fun ensureLoaded(modelPath: String): LlmInference = mutex.withLock {
        if (loadedPath == modelPath && engine != null) return@withLock engine!!
        // 切换模型：释放旧的
        engine?.close()
        engine = null
        loadedPath = null

        if (!File(modelPath).exists()) error("模型文件不存在：$modelPath")
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(1024)
            .build()
        val created = LlmInference.createFromOptions(context, options)
        engine = created
        loadedPath = modelPath
        created
    }

    /** 单轮对话。system 与 user 合并为一个 prompt（端侧小模型无独立 system 通道）。 */
    suspend fun complete(modelPath: String, systemPrompt: String, userPrompt: String): AiOutcome =
        withContext(Dispatchers.Default) {
            try {
                val llm = ensureLoaded(modelPath)
                val prompt = if (systemPrompt.isBlank()) userPrompt else "$systemPrompt\n\n$userPrompt"
                val text = llm.generateResponse(prompt).orEmpty().trim()
                AiOutcome.Ok(text)
            } catch (e: Throwable) {
                AiOutcome.Error("本地模型运行失败：${e.message ?: e.javaClass.simpleName}。可能是机型不支持或内存不足。")
            }
        }

    fun release() {
        engine?.close()
        engine = null
        loadedPath = null
    }
}
