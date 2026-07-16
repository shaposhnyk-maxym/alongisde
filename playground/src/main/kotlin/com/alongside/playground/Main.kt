package com.alongside.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alongside.core.ui.animation.CountUpText
import com.alongside.core.ui.animation.StaggerRevealColumn
import com.alongside.core.ui.animation.TypewriterText
import com.alongside.core.ui.component.AlongsideButton
import com.alongside.core.ui.component.AlongsideButtonVariant
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.theme.AlongsideTheme

@Composable
private fun PlaygroundContent() {
    AlongsideTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(text = "Alongside", style = MaterialTheme.typography.displayLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AlongsideButton(text = "Continue with Google", onClick = {})
                    AlongsideButton(text = "Join a Trip", onClick = {}, variant = AlongsideButtonVariant.Secondary)
                }

                PaperCard {
                    Text("Day 4 - Vinnytsia")
                }

                CountUpText(targetValue = 7, style = MaterialTheme.typography.displayMedium)

                TypewriterText(
                    text = "Your trip, together",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.size(320.dp, 48.dp),
                )

                StaggerRevealColumn(itemCount = 4, modifier = Modifier.size(320.dp, 260.dp)) { index ->
                    PaperCard { Text("Item $index") }
                }
            }
        }
    }
}

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Alongside Playground") {
            PlaygroundContent()
        }
    }
