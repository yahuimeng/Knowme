package com.knowme.app

import android.app.Application
import com.knowme.app.digest.DigestScheduler

class KnowmeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 安排每日早报（每天约 8 点）
        DigestScheduler.schedule(this)
    }
}
