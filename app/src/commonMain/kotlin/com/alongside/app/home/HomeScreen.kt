package com.alongside.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alongside.core.ui.component.CircleIconButton
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.component.ScreenHeader
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideTypography

private val SettingsButtonSize = 34.dp

@Composable
internal fun HomeScreen(
    state: HomeState,
    onOpenSettings: () -> Unit,
    onOpenRecap: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenMatches: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            val wordmarkStyle =
                MaterialTheme.alongsideTypography.displaySerifItalic.copy(fontSize = 22.sp, lineHeight = 26.sp)
            ScreenHeader(title = "Alongside", titleStyle = wordmarkStyle) {
                CircleIconButton(onClick = onOpenSettings, contentDescription = "Settings", size = SettingsButtonSize) {
                    Text("⚙")
                }
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AlongsideSpacing.xl)
                        .padding(bottom = AlongsideSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.xxl),
            ) {
                if (state.isRecapAvailable) {
                    HomeRecapEntryPoint(onClick = onOpenRecap)
                }
                when (val phase = state.phase) {
                    HomeTripPhase.NoActiveTrip -> HomeNoTripPlaceholder()
                    is HomeTripPhase.PreTrip -> HomePreTripCard(daysUntilReunion = phase.daysUntilReunion)
                    is HomeTripPhase.Active -> {
                        HomeTripDayCard(dayIndex = phase.dayIndex, totalDays = phase.totalDays, city = phase.today.city)
                        HomeTodayCard(summary = phase.today, dayIndex = phase.dayIndex, onClick = onOpenTimeline)
                        if (state.recentMatches.isNotEmpty()) {
                            HomeRecentMatchesRow(matches = state.recentMatches, onClick = onOpenMatches)
                        }
                    }
                    is HomeTripPhase.Completed ->
                        HomeCompletedTripCard(
                            daysTogether = phase.daysTogether,
                            isRecapAvailable = state.isRecapAvailable,
                        )
                }
            }
        }
    }
}
