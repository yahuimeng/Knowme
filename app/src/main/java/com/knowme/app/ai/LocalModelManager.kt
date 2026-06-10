package com.knowme.app.ai

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 本地模型文件管理：列出、从手机文件导入（带进度）、删除。模型存在 App 私有目录。 */
class LocalModelManager(context: Context) {

    private val dir: File = File(context.filesDir, "models").apply { mkdirs() }

    fun listModels(): List<String> =
        dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }?.map { it.name }?.sorted()
            ?: emptyList()

    fun pathFor(name: String): String = File(dir, name).absolutePath

    fun delete(name: String): Boolean = File(dir, name).delete()

    /**
     * 把用户在系统文件选择器里选中的 .gguf 拷进 App 私有目录（端侧引擎需要真实路径）。
     * 先写 .part 临时文件，完成后改名。onProgress 回传 0f..1f（无法获知大小时回 -1f）。
     * 返回保存后的文件名。
     */
    suspend fun importFrom(resolver: ContentResolver, uri: Uri, onProgress: (Float) -> Unit): Result<String> =
        withContext(Dispatchers.IO) {
            var name = "model.gguf"
            var total = -1L
            runCatching {
                resolver.query(uri, null, null, null, null)?.use { c ->
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (c.moveToFirst()) {
                        if (ni >= 0) c.getString(ni)?.let { name = it }
                        if (si >= 0 && !c.isNull(si)) total = c.getLong(si)
                    }
                }
            }
            val target = File(dir, name)
            val part = File(dir, "$name.part")
            try {
                resolver.openInputStream(uri)?.use { input ->
                    part.outputStream().use { output ->
                        val buf = ByteArray(1 shl 16)
                        var copied = 0L
                        var read: Int
                        while (input.read(buf).also { read = it } >= 0) {
                            output.write(buf, 0, read)
                            copied += read
                            onProgress(if (total > 0) copied.toFloat() / total else -1f)
                        }
                    }
                } ?: error("无法打开所选文件")
                if (target.exists()) target.delete()
                if (!part.renameTo(target)) error("保存失败")
                Result.success(name)
            } catch (e: Throwable) {
                part.delete()
                Result.failure(e)
            }
        }
}
