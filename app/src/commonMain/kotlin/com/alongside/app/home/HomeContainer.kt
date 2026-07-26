package com.alongside.app.home

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.recap.RecapRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import kotlin.time.Clock

public data class HomeState(
    val isRecapAvailable: Boolean = false,
)

/**
 * Roadmap: "the rest of Home stays a placeholder, this is not a reason to rework the whole
 * screen" (docs/roadmap.md M20.1) - the smallest possible state holder gating the Recap entry
 * point on `today >= recap.availableAt`. Mirrors [com.alongside.feature.settings.presentation.SettingsContainer]'s
 * uid + active trip pattern.
 */
public class HomeContainer(
    private val authSessionCache: AuthSessionCache,
    private val pairingRepository: PairingRepository,
    private val recapRepository: RecapRepository,
    private val clock: Clock = Clock.System,
) : ViewModel(),
    ContainerHost<HomeState, Nothing> {
    override val container: Container<HomeState, Nothing> = container(HomeState()) { observeRecapAvailability() }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun Syntax<HomeState, Nothing>.observeRecapAvailability() {
        val uid = authSessionCache.get()?.user?.uid ?: return
        pairingRepository
            .observeActiveTrip(uid)
            .flatMapLatest { trip -> trip?.let { recapRepository.observeById(it.id) } ?: flowOf(null) }
            .collect { recap ->
                val today = clock.todayIn(TimeZone.currentSystemDefault())
                reduce { state.copy(isRecapAvailable = recap != null && today >= recap.availableAt) }
            }
    }
}
