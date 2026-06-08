package com.knowme.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.knowme.app.ai.AiOutcome
import com.knowme.app.ui.MainViewModel

@Composable
fun AskScreen(vm: MainViewModel) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("问问", style = MaterialTheme.typography.headlineSmall)
        Text(
            "用你自己的 AI，追问这些天的通知。例如：今天有什么没回的？这周谁找我最多？",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("输入问题…") },
            minLines = 2,
        )

        Button(
            onClick = {
                loading = true
                answer = null
                vm.ask(question) { outcome ->
                    loading = false
                    answer = when (outcome) {
                        is AiOutcome.Ok -> outcome.text
                        is AiOutcome.Error -> outcome.message
                    }
                }
            },
            enabled = question.isNotBlank() && !loading,
        ) { Text("发送") }

        if (loading) {
            CircularProgressIndicator()
        }
        answer?.let {
            Card(Modifier.fillMaxWidth()) {
                Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
