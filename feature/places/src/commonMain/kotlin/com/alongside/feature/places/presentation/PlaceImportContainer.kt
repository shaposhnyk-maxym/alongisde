package com.alongside.feature.places.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.importing.PlaceImportPipeline
import com.alongside.core.domain.place.importing.PlaceImportResult
import com.alongside.core.domain.place.importing.extractShareUrl
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container

/**
 * Drives the share-link confirmation card: runs [PlaceImportPipeline.import] once on creation
 * (not a user [PlaceImportIntent] - the same "work happens in onCreate" shape as
 * [com.alongside.feature.pairing.presentation.PairingContainer]'s active-trip observation), then
 * lets the user [PlaceImportIntent.Accept] (persist) or [PlaceImportIntent.Discard] it.
 */
public class PlaceImportContainer(
    private val shareText: String,
    private val pipeline: PlaceImportPipeline,
    private val placeCandidateRepository: PlaceCandidateRepository,
    private val authSessionCache: AuthSessionCache,
    private val pairingRepository: PairingRepository,
) : ViewModel(),
    ContainerHost<PlaceImportState, PlaceImportSideEffect> {
    override val container: Container<PlaceImportState, PlaceImportSideEffect> =
        container(PlaceImportState()) { runImport() }

    public fun onIntent(intent: PlaceImportIntent) {
        when (intent) {
            PlaceImportIntent.Accept -> accept()
            PlaceImportIntent.Discard -> discard()
        }
    }

    private suspend fun Syntax<PlaceImportState, PlaceImportSideEffect>.runImport() {
        val url = extractShareUrl(shareText)
        if (url == null) {
            reduce { errorState(state, "No Google Maps link found in the shared text") }
            return
        }
        val uid = authSessionCache.get()?.user?.uid
        val trip = uid?.let { pairingRepository.observeActiveTrip(it).first() }
        if (uid == null || trip == null) {
            reduce { errorState(state, "No active trip to import this place into") }
            return
        }
        when (val result = pipeline.import(shareUrl = url, tripId = trip.id, addedByUserId = uid)) {
            is PlaceImportResult.Imported ->
                reduce { state.copy(status = PlaceImportStatus.FOUND, place = result.place) }
            PlaceImportResult.NotFound ->
                reduce { state.copy(status = PlaceImportStatus.NOT_FOUND) }
            is PlaceImportResult.Failure -> {
                val message = result.cause.message ?: "Something went wrong importing this place"
                reduce { errorState(state, message) }
            }
        }
    }

    private fun errorState(
        current: PlaceImportState,
        message: String,
    ) = current.copy(status = PlaceImportStatus.ERROR, errorMessage = message)

    private fun accept() =
        intent {
            val place = state.place ?: return@intent
            placeCandidateRepository.upsert(place)
            postSideEffect(PlaceImportSideEffect.Imported)
        }

    private fun discard() =
        intent {
            postSideEffect(PlaceImportSideEffect.Discarded)
        }
}
