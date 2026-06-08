package com.knowme.app

import android.app.Application
import com.knowme.app.digest.DigestScheduler

class KnowmeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 每日基线 + 按用户选择的自动模式恢复后台任务
        DigestScheduler.schedule(this)
        container.applyDigestSchedule()
    }
}
