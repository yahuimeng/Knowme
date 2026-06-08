package com.knowme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knowme.app.ui.KnowmeRoot
import com.knowme.app.ui.MainViewModel
import com.knowme.app.ui.theme.KnowmeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as KnowmeApp).container
        setContent {
            KnowmeTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(container))
                KnowmeRoot(vm)
            }
        }
    }
}
