package com.alongside.feature.places.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container

/**
 * Drives the Places tab: reactive list of the active trip's places (read-only - no user actions
 * yet, M16 adds add/edit/delete). The incomplete-photo retry loop that used to run here moved to
 * WorkManager (docs/roadmap.md M12.11, [PlaceRetryCoordinator]) - it survives process death,
 * which a loop tied to this Container's lifetime never could.
 */
public class PlacesListContainer(
    private val authSessionCache: AuthSessionCache,
    private val placesListDataSource: PlacesListDataSource,
) : ViewModel(),
    ContainerHost<PlacesListState, Nothing> {
    override val container: Container<PlacesListState, Nothing> =
        container(PlacesListState()) { observePlaces() }

    private suspend fun Syntax<PlacesListState, Nothing>.observePlaces() {
        val uid = authSessionCache.get()?.user?.uid ?: return
        placesListDataSource.observe(uid) { places -> reduce { state.copy(places = places, isLoading = false) } }
    }
}
