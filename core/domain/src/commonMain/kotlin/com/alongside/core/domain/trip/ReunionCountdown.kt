package com.alongside.core.domain.trip

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Days remaining until [meetingDate] (Trip.startDate - the two sides are apart until the trip
 * begins, which is when they reunite). Clamped at zero rather than going negative once the
 * meeting date has passed - CLAUDE.md's countdown is a days-to-go display, not a signed offset.
 */
public fun daysUntilReunion(
    today: LocalDate,
    meetingDate: LocalDate,
): Int = maxOf(0, today.daysUntil(meetingDate))
