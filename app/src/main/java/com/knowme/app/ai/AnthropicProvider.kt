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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Claude 原生 /v1/messages 接口。 */
object AnthropicProvider : AiProvider {

    @Serializable
    private data class Req(
        val model: String,
        val system: String,
        val messages: List<Msg>,
        @SerialName("max_tokens") val maxTokens: Int = 2048,
    )

    @Serializable
    private data class Msg(val role: String, val content: String)

    @Serializable
    private data class Resp(val content: List<Block> = emptyList(), val usage: Usage? = null)

    @Serializable
    private data class Block(val type: String = "text", val text: String = "")

    @Serializable
    private data class Usage(
        @SerialName("input_tokens") val inputTokens: Int = 0,
        @SerialName("output_tokens") val outputTokens: Int = 0,
    )

    override suspend fun complete(
        config: AiConfig,
        systemPrompt: String,
        userPrompt: String,
    ): AiOutcome = try {
        val url = config.baseUrl.trimEnd('/') + "/v1/messages"
        val response: HttpResponse = aiHttpClient.post(url) {
            headers {
                append("x-api-key", config.apiKey)
                append("anthropic-version", "2023-06-01")
            }
            contentType(ContentType.Application.Json)
            setBody(
                Req(
                    model = config.model,
                    system = systemPrompt,
                    messages = listOf(Msg("user", userPrompt)),
                )
            )
        }
        if (response.status == HttpStatusCode.OK) {
            val body = response.body<Resp>()
            val text = body.content.firstOrNull { it.type == "text" }?.text.orEmpty()
            AiOutcome.Ok(text.trim(), body.usage?.inputTokens ?: 0, body.usage?.outputTokens ?: 0)
        } else {
            AiOutcome.Error("Claude 返回 ${response.status.value}：${response.bodyAsText().take(300)}")
        }
    } catch (e: Exception) {
        AiOutcome.Error("请求失败：${e.message}")
    }
}
