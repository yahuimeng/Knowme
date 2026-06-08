package com.knowme.app.ai

import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容的 /v1/chat/completions 接口。
 * 豆包、DeepSeek、本地 Ollama 等都提供该兼容端点——填不同 baseUrl + key + model 即可。
 */
object OpenAiCompatProvider : AiProvider {

    @Serializable
    private data class Req(val model: String, val messages: List<Msg>)

    @Serializable
    private data class Msg(val role: String, val content: String)

    @Serializable
    private data class Resp(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: Msg? = null)

    override suspend fun complete(
        config: AiConfig,
        systemPrompt: String,
        userPrompt: String,
    ): AiOutcome = try {
        val url = config.baseUrl.trimEnd('/') + "/v1/chat/completions"
        val response: HttpResponse = aiHttpClient.post(url) {
            headers { append("Authorization", "Bearer ${config.apiKey}") }
            contentType(ContentType.Application.Json)
            setBody(
                Req(
                    model = config.model,
                    messages = listOf(
                        Msg("system", systemPrompt),
                        Msg("user", userPrompt),
                    ),
                )
            )
        }
        if (response.status == HttpStatusCode.OK) {
            val text = response.body<Resp>().choices.firstOrNull()?.message?.content.orEmpty()
            AiOutcome.Ok(text.trim())
        } else {
            AiOutcome.Error("模型返回 ${response.status.value}：${response.bodyAsText().take(300)}")
        }
    } catch (e: Exception) {
        AiOutcome.Error("请求失败：${e.message}")
    }
}
