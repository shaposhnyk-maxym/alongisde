package com.alongside.core.network.places

import com.alongside.core.domain.place.importing.ShareLinkRedirectResult
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondRedirect
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// runBlocking, not runTest: see the M3 note in docs/roadmap.md.
class KtorShareLinkRedirectResolverTest {
    @Test
    fun `a 302 with a Location header resolves to the redirect target`() =
        runBlocking {
            val target = "https://www.google.com/maps/place/Rynok+Square/data=!4m2!3m1!1s0xabc:0xdef"
            val resolver = testKtorShareLinkRedirectResolver { respondRedirect(target) }

            val result = resolver.resolve("https://maps.app.goo.gl/abc")

            val resolved = assertIs<ShareLinkRedirectResult.Resolved>(result)
            assertEquals(target, resolved.url)
        }

    @Test
    fun `a 200 response with no redirect surfaces as Failure`() {
        runBlocking {
            val resolver = testKtorShareLinkRedirectResolver { respond("not a redirect") }

            val result = resolver.resolve("https://maps.app.goo.gl/abc")

            assertIs<ShareLinkRedirectResult.Failure>(result)
        }
    }

    @Test
    fun `a redirect status with no Location header surfaces as Failure`() {
        runBlocking {
            val resolver = testKtorShareLinkRedirectResolver { respond("", HttpStatusCode.Found) }

            val result = resolver.resolve("https://maps.app.goo.gl/abc")

            assertIs<ShareLinkRedirectResult.Failure>(result)
        }
    }
}
