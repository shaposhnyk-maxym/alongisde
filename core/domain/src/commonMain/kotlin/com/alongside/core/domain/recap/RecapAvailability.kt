package com.alongside.core.domain.recap

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

public fun recapAvailableAt(tripEndDate: LocalDate): LocalDate = tripEndDate.plus(1, DateTimeUnit.DAY)
