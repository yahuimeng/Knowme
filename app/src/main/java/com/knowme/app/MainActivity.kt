package com.knowme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knowme.app.notification.NotificationAccess
import com.knowme.app.ui.KnowmeRoot
import com.knowme.app.ui.MainViewModel
import com.knowme.app.ui.screens.OnboardingScreen
import com.knowme.app.ui.theme.KnowmeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as KnowmeApp).container
        // 覆盖安装/更新后系统不会自动 rebind 监听服务，冷启动时主动拉一次，恢复新通知录入
        NotificationAccess.requestRebind(this)
        setContent {
            KnowmeTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(container))
                var onboarded by remember { mutableStateOf(vm.onboarded) }
                if (!onboarded) {
                    OnboardingScreen(onDone = { vm.markOnboarded(); onboarded = true })
                } else {
                    KnowmeRoot(vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 回到前台再兜一次：用户刚授权、或服务运行中被系统回收时，都能把监听拉回来
        NotificationAccess.requestRebind(this)
    }
}
