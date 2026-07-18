package com.alongside.core.domain.diary.processing

/** On exhaustion, regeneration is disabled but the last-generated description is kept. */
public const val MAX_DESCRIPTION_REGENERATION_ATTEMPTS: Int = 3

public fun canRegenerateDescription(attempts: Int): Boolean = attempts < MAX_DESCRIPTION_REGENERATION_ATTEMPTS
