package com.alongside.app.home

import com.alongside.core.domain.place.resolveMatchStatus
import com.alongside.core.domain.recap.daysTogether
import com.alongside.core.domain.trip.TripPhase
import com.alongside.core.domain.trip.daysUntilReunion
import com.alongside.core.domain.trip.tripPhase
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.core.model.place.MatchStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.trip.Trip
import kotlinx.datetime.LocalDate

private const val RECENT_MATCHES_LIMIT = 4

public data class HomeState(
    val today: LocalDate? = null,
    val trip: Trip? = null,
    val ownTodayEntry: DiaryEntry? = null,
    val partnerTodayEntry: DiaryEntry? = null,
    val ownTodayEpisodes: List<Episode> = emptyList(),
    val partnerTodayEpisodes: List<Episode> = emptyList(),
    val placeCandidates: List<PlaceCandidate> = emptyList(),
    val placeSwipes: List<PlaceSwipe> = emptyList(),
    val isRecapAvailable: Boolean = false,
) {
    public val phase: HomeTripPhase
        get() {
            val trip = trip ?: return HomeTripPhase.NoActiveTrip
            val today = today ?: return HomeTripPhase.NoActiveTrip
            return when (tripPhase(trip, today)) {
                TripPhase.PRE_TRIP -> HomeTripPhase.PreTrip(daysUntilReunion(today, trip.startDate))
                TripPhase.ACTIVE ->
                    HomeTripPhase.Active(
                        dayIndex = daysTogether(trip.startDate, today),
                        totalDays = daysTogether(trip.startDate, trip.endDate),
                        today = todaySummary(),
                    )
                TripPhase.POST_TRIP -> HomeTripPhase.Completed(daysTogether(trip.startDate, trip.endDate))
            }
        }

    /** Matches derived on the fly - never stored, same idiom as `MatcherState.matches`. */
    public val recentMatches: List<PlaceCandidate>
        get() {
            val trip = trip ?: return emptyList()
            val memberId = trip.memberId ?: return emptyList()
            return placeCandidates
                .filter { candidate ->
                    resolveMatchStatus(
                        ownerSwipe = swipeDirection(candidate.id, trip.ownerId),
                        memberSwipe = swipeDirection(candidate.id, memberId),
                    ) == MatchStatus.MATCHED
                }.sortedByDescending { it.updatedAt }
                .take(RECENT_MATCHES_LIMIT)
        }

    private fun swipeDirection(
        candidateId: String,
        userId: String,
    ): SwipeDirection? = placeSwipes.find { it.candidateId == candidateId && it.userId == userId }?.direction

    private fun todaySummary(): HomeTodaySummary {
        val ownEpisode = ownTodayEpisodes.firstOrNull()
        val partnerEpisode = partnerTodayEpisodes.firstOrNull()
        val leadEpisode = ownEpisode ?: partnerEpisode
        return HomeTodaySummary(
            placeName = leadEpisode?.placeName,
            city = leadEpisode?.city,
            photoModel = leadEpisode?.photos?.firstOrNull()?.loadableModel(),
            ownHasPhotos = ownTodayEpisodes.any { it.photos.isNotEmpty() },
            partnerHasPhotos = partnerTodayEpisodes.any { it.photos.isNotEmpty() },
        )
    }
}

public sealed interface HomeTripPhase {
    public data object NoActiveTrip : HomeTripPhase

    public data class PreTrip(
        val daysUntilReunion: Int,
    ) : HomeTripPhase

    public data class Active(
        val dayIndex: Int,
        val totalDays: Int,
        val today: HomeTodaySummary,
    ) : HomeTripPhase

    public data class Completed(
        val daysTogether: Int,
    ) : HomeTripPhase
}

public data class HomeTodaySummary(
    val placeName: String?,
    val city: String?,
    val photoModel: String?,
    val ownHasPhotos: Boolean,
    val partnerHasPhotos: Boolean,
)

private fun Photo.loadableModel(): String = remoteUrl ?: uri
