package com.knowme.app.ai

/** 用户在设置页选择的 AI 后端类型。 */
enum class AiBackend(val label: String, val defaultBaseUrl: String, val defaultModel: String) {
    // Claude 原生 messages 接口
    ANTHROPIC("Claude", "https://api.anthropic.com", "claude-opus-4-8"),
    // OpenAI 兼容接口：豆包 / DeepSeek / 本地 Ollama 等都走这条
    OPENAI_COMPAT("OpenAI 兼容", "https://api.openai.com", "gpt-4o-mini"),
}

/**
 * 一份 BYOK 配置。全部存在本地（key 加密），绝不上传。
 */
data class AiConfig(
    val backend: AiBackend = AiBackend.ANTHROPIC,
    val baseUrl: String = AiBackend.ANTHROPIC.defaultBaseUrl,
    val apiKey: String = "",
    val model: String = AiBackend.ANTHROPIC.defaultModel,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}

/** 统一的对话结果（成功返回文本，失败带错误信息）。 */
sealed interface AiOutcome {
    data class Ok(val text: String) : AiOutcome
    data class Error(val message: String) : AiOutcome
}
