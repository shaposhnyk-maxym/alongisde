package com.alongside.data.sync

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

private interface FakeBinding {
    val name: String
}

private class FakeBindingA : FakeBinding {
    override val name = "A"
}

private class FakeBindingB : FakeBinding {
    override val name = "B"
}

/**
 * Regression guard for a real production bug found manually testing M12.6/M12.7: `DataModule.kt`
 * declared `single<SyncEntityBinding> { ... }` three times (Trip/DiaryEntry/Episode) with no
 * qualifier. Koin's registry keys a `single` by (type, qualifier) - three registrations of the
 * exact same type with no qualifier silently overwrite each other, so
 * `getAll<SyncEntityBinding>()` only ever returned the LAST-declared one. Only
 * `EpisodeSyncEntityBinding` ever actually ran; Trip's and DiaryEntry's local rows never got
 * marked SYNCED after a successful push, since `SyncCoordinator.bindingFor(...)` could never find
 * them - invisible in every unit test, since those always construct `SyncCoordinator` with an
 * explicit `bindings` list, bypassing Koin's `getAll` entirely. The fix: `single { Concrete() }
 * bind Interface::class` instead - this keeps each registration under its own concrete-type key
 * while still being discoverable via `getAll<Interface>()`.
 */
class KoinSameTypeMultiBindTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `bind keeps every same-interface single individually addressable for getAll`() {
        val app =
            startKoin {
                modules(
                    module {
                        single { FakeBindingA() } bind FakeBinding::class
                        single { FakeBindingB() } bind FakeBinding::class
                    },
                )
            }

        val all = app.koin.getAll<FakeBinding>()

        assertEquals(setOf("A", "B"), all.map { it.name }.toSet())
    }
}
