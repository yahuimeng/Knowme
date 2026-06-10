package com.knowme.app.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 本地模型文件管理：列出、下载（带进度）、删除。模型存在 App 私有目录。 */
class LocalModelManager(context: Context) {

    private val dir: File = File(context.filesDir, "models").apply { mkdirs() }

    fun listModels(): List<String> =
        dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }?.map { it.name }?.sorted()
            ?: emptyList()

    fun pathFor(name: String): String = File(dir, name).absolutePath

    fun exists(name: String): Boolean = File(dir, name).exists()

    fun delete(name: String): Boolean = File(dir, name).delete()

    /**
     * 下载模型到 [name]。先写入 .part 临时文件，完成后改名，避免半截文件被当成可用模型。
     * onProgress 回传 0f..1f（无 Content-Length 时回传 -1f 表示未知）。
     */
    suspend fun download(url: String, name: String, onProgress: (Float) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            val target = File(dir, name)
            val part = File(dir, "$name.part")
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                }
                conn.connect()
                if (conn.responseCode !in 200..299) {
                    return@withContext Result.failure(
                        IllegalStateException("下载失败：HTTP ${conn.responseCode}（gated 模型需带 token 的直链）")
                    )
                }
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    part.outputStream().use { output ->
                        val buf = ByteArray(1 shl 16)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buf).also { read = it } >= 0) {
                            output.write(buf, 0, read)
                            downloaded += read
                            onProgress(if (total > 0) downloaded.toFloat() / total else -1f)
                        }
                    }
                }
                if (target.exists()) target.delete()
                if (!part.renameTo(target)) error("重命名失败")
                Result.success(Unit)
            } catch (e: Throwable) {
                part.delete()
                Result.failure(e)
            }
        }
}
