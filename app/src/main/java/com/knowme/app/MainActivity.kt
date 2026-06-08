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
import com.knowme.app.ui.KnowmeRoot
import com.knowme.app.ui.MainViewModel
import com.knowme.app.ui.screens.OnboardingScreen
import com.knowme.app.ui.theme.KnowmeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as KnowmeApp).container
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
}
