package com.alongside.core.domain.recap

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration
import kotlin.time.Instant

/** How long until [availableAt]'s local midnight - clamped at zero, never negative, once it's passed. */
public fun durationUntilRecapNotification(
    availableAt: LocalDate,
    now: Instant,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Duration = (availableAt.atStartOfDayIn(zone) - now).coerceAtLeast(Duration.ZERO)
