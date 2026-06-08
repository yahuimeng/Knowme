package com.knowme.app.digest

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.knowme.app.KnowmeApp

/** 每天定时把当天通知消化成早报，并顺手做隐私保留期清理。 */
class DailyDigestWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as KnowmeApp).container
        // 先清理过期原文（隐私保留期）
        container.runRetentionCleanup()
        // 未配置 AI 时不报错，安静跳过——下次配置好自然会跑
        if (container.activeProfile()?.isConfigured != true) return Result.success()

        val generator = DigestGenerator(container.db, container::chat)
        return when (generator.generateForToday()) {
            is DigestResult.Ok -> Result.success()
            is DigestResult.Error -> Result.retry()
        }
    }
}
