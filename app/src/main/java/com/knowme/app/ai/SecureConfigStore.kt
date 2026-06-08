package com.knowme.app.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AI 配置的本地加密存储：保存多份档案 + 当前使用中的 id。
 * 全部经 Android Keystore 派生的主密钥加密，仅存于本机，绝不上传、绝不写日志。
 */
class SecureConfigStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

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

    fun loadProfiles(): List<AiProfile> {
        val raw = prefs.getString(KEY_PROFILES, null)
        if (raw != null) {
            return runCatching { json.decodeFromString<List<AiProfile>>(raw) }.getOrDefault(emptyList())
        }
        // 旧版本单配置 → 迁移成一份档案
        val legacyKey = prefs.getString(LEGACY_API_KEY, "").orEmpty()
        if (legacyKey.isNotBlank()) {
            val backend = runCatching {
                AiBackend.valueOf(prefs.getString(LEGACY_BACKEND, AiBackend.ANTHROPIC.name)!!)
            }.getOrDefault(AiBackend.ANTHROPIC)
            val migrated = AiProfile(
                id = "legacy",
                name = "我的 ${backend.label}",
                backend = backend,
                baseUrl = prefs.getString(LEGACY_BASE_URL, backend.defaultBaseUrl)!!,
                apiKey = legacyKey,
                model = prefs.getString(LEGACY_MODEL, backend.defaultModel)!!,
            )
            saveProfiles(listOf(migrated))
            setActiveId("legacy")
            return listOf(migrated)
        }
        return emptyList()
    }

    fun saveProfiles(profiles: List<AiProfile>) {
        prefs.edit().putString(KEY_PROFILES, json.encodeToString(profiles)).apply()
    }

    fun loadActiveId(): String? = prefs.getString(KEY_ACTIVE_ID, null)

    fun setActiveId(id: String?) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_PROFILES = "profiles"
        const val KEY_ACTIVE_ID = "active_id"
        // 旧版字段（仅用于迁移）
        const val LEGACY_BACKEND = "backend"
        const val LEGACY_BASE_URL = "base_url"
        const val LEGACY_API_KEY = "api_key"
        const val LEGACY_MODEL = "model"
    }
}
