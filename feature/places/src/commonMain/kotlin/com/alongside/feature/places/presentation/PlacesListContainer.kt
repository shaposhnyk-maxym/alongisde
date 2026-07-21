package com.alongside.feature.places.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container

/**
 * Drives the Places tab: reactive list of the active trip's places (read-only - no user actions
 * yet, M16 adds add/edit/delete) plus the incomplete-photo retry loop, which moves here from its
 * previous ad-hoc home directly on the tab's `LaunchedEffect` in `AlongsideApp.kt`.
 */
public class PlacesListContainer(
    private val authSessionCache: AuthSessionCache,
    private val placesListDataSource: PlacesListDataSource,
    private val placeRetryDataSource: PlaceRetryDataSource,
) : ViewModel(),
    ContainerHost<PlacesListState, Nothing> {
    override val container: Container<PlacesListState, Nothing> =
        container(PlacesListState()) { observePlaces() }

    private suspend fun Syntax<PlacesListState, Nothing>.observePlaces() {
        val uid = authSessionCache.get()?.user?.uid ?: return
        coroutineScope {
            launch { placeRetryDataSource.observeAndRetry(uid) }
            placesListDataSource.observe(uid) { places -> reduce { state.copy(places = places, isLoading = false) } }
        }
    }
}
