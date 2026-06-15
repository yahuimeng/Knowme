package com.knowme.app.ai

import android.app.ActivityManager
import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 端侧推理引擎：基于内嵌的 llama.cpp（llamalib / com.arm.aichat），支持 .gguf。
 *
 * 模型**常驻**：加载后留在内存复用。可主动 [load] 启动、[stop] 停止（像播放/停止）。
 * 每次生成只重设 system（底层清空上下文），保证"独立一问"互不污染。
 */
class LocalLlmEngine(context: Context) {

    private val appContext = context.applicationContext
    private val engine = AiChat.getInferenceEngine(appContext)
    private val mutex = Mutex()
    private var loadedPath: String? = null

    /**
     * 加载前的内存预检。模型权重 + KV 缓存放不进可用内存时，native 层会被系统直接 OOM-kill
     * （Kotlin try/catch 拦不住，表现为闪退）。这里提前估算、放不下就友好拒绝，而不是把进程交出去送死。
     * 估算：权重(文件大小) × 1.3（含 KV/计算缓冲的粗略余量）。偏保守，宁可拒也不闪退。
     */
    private fun memoryGuardOrNull(modelPath: String): String? {
        val fileBytes = File(modelPath).length()
        if (fileBytes <= 0) return null
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val needBytes = (fileBytes * 1.3).toLong()
        if (needBytes <= mi.availMem) return null
        val gb = { b: Long -> "%.1f".format(b / 1_073_741_824.0) }
        return "这个模型约 ${gb(fileBytes)} GB，当前可用内存只有约 ${gb(mi.availMem)} GB，装不下会闪退。" +
            "请换更小的模型（建议 1–2B 的 Q4，如 Qwen2.5-1.5B-Instruct、Gemma-3-1B），或先关掉后台应用再试。"
    }

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** 调用方需持有 mutex。确保目标模型已加载到内存。 */
    private suspend fun ensureLoadedLocked(modelPath: String) {
        if (!File(modelPath).exists()) error("模型文件不存在：$modelPath")
        // 只在需要真正加载这个模型时预检（已加载好的复用，不必拦）
        if (loadedPath != modelPath) memoryGuardOrNull(modelPath)?.let { error(it) }
        engine.state.first { it !is State.Uninitialized && it !is State.Initializing }
        if (engine.state.value is State.Error) {
            engine.cleanUp(); loadedPath = null
        }
        val needLoad = loadedPath != modelPath || engine.state.value !is State.ModelReady
        if (needLoad) {
            if (engine.state.value is State.ModelReady) engine.cleanUp()
            engine.loadModel(modelPath)
            loadedPath = modelPath
        }
        _loaded.value = true
    }

    /** 主动启动（预加载）模型，不生成。供"播放"按钮调用。 */
    suspend fun load(modelPath: String): AiOutcome = withContext(Dispatchers.Default) {
        mutex.withLock {
            try {
                ensureLoadedLocked(modelPath)
                AiOutcome.Ok("已加载")
            } catch (e: Throwable) {
                loadedPath = null; _loaded.value = false
                runCatching { engine.cleanUp() }
                AiOutcome.Error("本地模型加载失败：${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    suspend fun complete(modelPath: String, systemPrompt: String, userPrompt: String): AiOutcome =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    ensureLoadedLocked(modelPath)
                    if (systemPrompt.isNotBlank()) engine.setSystemPrompt(systemPrompt)
                    val text = engine.sendUserPrompt(userPrompt).toList().joinToString("").trim()
                    AiOutcome.Ok(text)
                } catch (e: Throwable) {
                    loadedPath = null; _loaded.value = false
                    runCatching { engine.cleanUp() }
                    AiOutcome.Error("本地模型运行失败：${e.message ?: e.javaClass.simpleName}")
                }
            }
        }

    /** 停止：卸载模型、释放内存。 */
    fun stop() {
        loadedPath = null
        _loaded.value = false
        runCatching { engine.cleanUp() }
    }
}
