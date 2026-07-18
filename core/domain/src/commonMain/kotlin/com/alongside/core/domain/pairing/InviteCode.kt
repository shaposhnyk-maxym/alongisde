package com.alongside.core.domain.pairing

import kotlin.random.Random

public const val INVITE_CODE_LENGTH: Int = 6

/** A–Z + 2–9 minus the ambiguous 0/O/1/I — 32 symbols, easy to read aloud from a partner's screen. */
public const val INVITE_CODE_ALPHABET: String = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/** Upper bound of [InviteCodeGenerator.generateUnique] retries before giving up. */
public const val INVITE_CODE_MAX_GENERATION_ATTEMPTS: Int = 100

private val INVITE_CODE_REGEX = Regex("[$INVITE_CODE_ALPHABET]{$INVITE_CODE_LENGTH}")

public fun isValidInviteCodeFormat(code: String): Boolean = code.matches(INVITE_CODE_REGEX)

public class InviteCodeGenerator(
    private val random: Random = Random.Default,
) {
    public fun generate(): String =
        buildString(INVITE_CODE_LENGTH) {
            repeat(INVITE_CODE_LENGTH) {
                append(INVITE_CODE_ALPHABET[random.nextInt(INVITE_CODE_ALPHABET.length)])
            }
        }

    /**
     * Generates codes until [isTaken] reports a free one.
     *
     * @throws IllegalStateException after [INVITE_CODE_MAX_GENERATION_ATTEMPTS] taken codes in a row.
     */
    public suspend fun generateUnique(isTaken: suspend (String) -> Boolean): String {
        repeat(INVITE_CODE_MAX_GENERATION_ATTEMPTS) {
            val code = generate()
            if (!isTaken(code)) {
                return code
            }
        }
        error("No free invite code after $INVITE_CODE_MAX_GENERATION_ATTEMPTS attempts")
    }
}
