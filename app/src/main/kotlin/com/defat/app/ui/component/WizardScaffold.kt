package com.defat.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared scaffold for every onboarding step: a title, scrollable content,
 * and a bottom bar with an optional Back button and a Next/Start button
 * (plan §8.1/§8.2).
 */
@Composable
fun WizardScaffold(
    title: String,
    nextLabel: String,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    modifier: Modifier = Modifier,
    backLabel: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable Column.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (onBack != null && backLabel != null) {
                    TextButton(onClick = onBack) { Text(backLabel) }
                } else {
                    Text("")
                }
                Button(onClick = onNext, enabled = nextEnabled) { Text(nextLabel) }
            }
        },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            content()
        }
    }
}
