package com.knowme.app.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * AI 配置的本地加密存储。API key 经 Android Keystore 派生的主密钥加密，
 * 仅存于本机，绝不上传、绝不写日志。
 */
class SecureConfigStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "knowme_secure_ai",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun load(): AiConfig {
        val backend = runCatching {
            AiBackend.valueOf(prefs.getString(KEY_BACKEND, AiBackend.ANTHROPIC.name)!!)
        }.getOrDefault(AiBackend.ANTHROPIC)
        return AiConfig(
            backend = backend,
            baseUrl = prefs.getString(KEY_BASE_URL, backend.defaultBaseUrl)!!,
            apiKey = prefs.getString(KEY_API_KEY, "")!!,
            model = prefs.getString(KEY_MODEL, backend.defaultModel)!!,
        )
    }

    fun save(config: AiConfig) {
        prefs.edit()
            .putString(KEY_BACKEND, config.backend.name)
            .putString(KEY_BASE_URL, config.baseUrl)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_MODEL, config.model)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_BACKEND = "backend"
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
    }
}
