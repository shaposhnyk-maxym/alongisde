package com.alongside.core.domain.pairing

import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InviteCodeGeneratorTest {
    @Test
    fun `generated code has exactly six characters`() {
        val generator = InviteCodeGenerator(Random(seed = 42))
        repeat(100) {
            assertEquals(INVITE_CODE_LENGTH, generator.generate().length)
        }
    }

    @Test
    fun `generated code uses only the unambiguous alphabet`() {
        val generator = InviteCodeGenerator(Random(seed = 42))
        repeat(100) {
            val code = generator.generate()
            assertTrue(code.all { it in INVITE_CODE_ALPHABET }, "unexpected characters in $code")
            assertTrue(code.none { it in "01OI" }, "ambiguous characters in $code")
        }
    }

    @Test
    fun `a thousand generated codes are distinct`() {
        val generator = InviteCodeGenerator(Random(seed = 42))
        val codes = List(1_000) { generator.generate() }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `generateUnique retries past taken codes`() =
        runTest {
            val generator = InviteCodeGenerator(Random(seed = 42))
            val checked = mutableListOf<String>()
            val code =
                generator.generateUnique { candidate ->
                    checked += candidate
                    checked.size <= 3
                }
            assertEquals(4, checked.size)
            assertEquals(checked.last(), code)
        }

    @Test
    fun `generateUnique fails after the attempt cap when every code is taken`() =
        runTest {
            val generator = InviteCodeGenerator(Random(seed = 42))
            var attempts = 0
            assertFailsWith<IllegalStateException> {
                generator.generateUnique {
                    attempts += 1
                    true
                }
            }
            assertEquals(INVITE_CODE_MAX_GENERATION_ATTEMPTS, attempts)
        }

    @Test
    fun `format validation accepts a well-formed code`() {
        assertTrue(isValidInviteCodeFormat("ABCD23"))
    }

    @Test
    fun `format validation rejects malformed codes`() {
        assertFalse(isValidInviteCodeFormat(""), "empty")
        assertFalse(isValidInviteCodeFormat("ABC23"), "too short")
        assertFalse(isValidInviteCodeFormat("ABCD234"), "too long")
        assertFalse(isValidInviteCodeFormat("abcd23"), "lowercase")
        assertFalse(isValidInviteCodeFormat("ABCD10"), "ambiguous digits 1/0")
        assertFalse(isValidInviteCodeFormat("ABCDOI"), "ambiguous letters O/I")
        assertFalse(isValidInviteCodeFormat("AB CD2"), "whitespace")
    }
}
