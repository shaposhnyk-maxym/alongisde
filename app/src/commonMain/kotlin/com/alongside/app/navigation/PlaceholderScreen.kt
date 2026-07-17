package com.alongside.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.alongside.core.ui.component.AlongsidePrimaryButton
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.OverlineLabelTone
import com.alongside.core.ui.theme.AlongsideSpacing

/**
 * Stand-in body for destinations whose feature module hasn't landed yet - keeps the
 * Navigation 3 backbone walkable end-to-end while features arrive milestone by milestone.
 */
@Composable
internal fun PlaceholderScreen(
    title: String,
    note: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = AlongsideSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg),
        ) {
            OverlineLabel(text = "Coming soon", tone = OverlineLabelTone.Accent)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                AlongsidePrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().padding(top = AlongsideSpacing.sm),
                )
            }
            extraContent()
        }
    }
}
