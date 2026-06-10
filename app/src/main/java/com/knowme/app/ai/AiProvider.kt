package com.knowme.app.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * AI 后端抽象：App 内部只依赖这个接口，"用谁分析"由用户配置决定。
 * Prompt 模板与 Provider 解耦——换模型不用改业务逻辑。
 */
interface AiProvider {
    /** 单轮对话：给定 system 与 user 文本，返回模型回复。 */
    suspend fun complete(config: AiConfig, systemPrompt: String, userPrompt: String): AiOutcome

    companion object {
        fun forBackend(backend: AiBackend): AiProvider = when (backend) {
            AiBackend.ANTHROPIC -> AnthropicProvider
            AiBackend.OPENAI_COMPAT -> OpenAiCompatProvider
            // 本地模型不走 HTTP Provider，由 AppContainer 直接路由到端侧引擎
            AiBackend.LOCAL -> error("LOCAL backend does not use an HTTP provider")
        }
    }
}

/** 所有 Provider 共用一个 Ktor client。 */
internal val aiHttpClient: HttpClient by lazy {
    HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }
}
