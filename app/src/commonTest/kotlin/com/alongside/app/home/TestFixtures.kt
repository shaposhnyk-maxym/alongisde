package com.alongside.app.home

import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.domain.pairing.JoinTripResult
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.domain.recap.RecapRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.recap.Recap
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun testAuthSession(uid: String = "uid-1"): AuthSession =
    AuthSession(
        user = AuthUser(uid = uid, email = null, displayName = null, photoUrl = null),
        idToken = "id-token",
        refreshToken = null,
        expiresInSeconds = 3600L,
        issuedAt = Instant.fromEpochMilliseconds(0),
    )

internal class FakeAuthSessionCache(
    private var session: AuthSession? = testAuthSession(),
) : AuthSessionCache {
    override suspend fun get(): AuthSession? = session

    override suspend fun save(session: AuthSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}

internal fun fakeTrip(
    id: String = "trip-1",
    ownerId: String = "uid-1",
    memberId: String? = "partner-1",
    inviteCode: String = "ABCD23",
    startDate: LocalDate = LocalDate(2026, 7, 18),
    endDate: LocalDate = LocalDate(2026, 8, 1),
): Trip =
    Trip(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = inviteCode,
        startDate = startDate,
        endDate = endDate,
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** Only [observeActiveTrip] is exercised by Home - create/join belong to feature:pairing. */
internal class FakePairingRepository : PairingRepository {
    val activeTrip = MutableStateFlow<Trip?>(null)

    override suspend fun createTrip(
        ownerId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Trip = error("not used by Home")

    override suspend fun joinTrip(
        code: String,
        userId: String,
    ): JoinTripResult = error("not used by Home")

    override fun observeActiveTrip(userId: String): Flow<Trip?> = activeTrip

    override suspend fun getActiveTrip(userId: String): Trip? = activeTrip.value
}

internal class FakeRecapRepository : RecapRepository {
    private val recaps = MutableStateFlow<Map<String, Recap>>(emptyMap())

    override suspend fun ensureScheduled(
        tripId: String,
        availableAt: LocalDate,
    ) {
        recaps.value = recaps.value + (tripId to Recap(tripId = tripId, availableAt = availableAt))
    }

    override suspend fun getById(tripId: String): Recap? = recaps.value[tripId]

    override fun observeById(tripId: String): Flow<Recap?> = recaps.map { it[tripId] }
}

internal fun fakeDiaryEntry(
    id: String,
    userId: String,
    date: LocalDate,
    tripId: String = "trip-1",
): DiaryEntry =
    DiaryEntry(
        id = id,
        tripId = tripId,
        userId = userId,
        date = date,
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

internal fun fakeEpisode(
    id: String,
    diaryEntryId: String,
    placeName: String? = null,
    city: String? = null,
    photos: List<Photo> = emptyList(),
): Episode =
    Episode(
        id = id,
        diaryEntryId = diaryEntryId,
        startTime = Instant.fromEpochMilliseconds(0),
        endTime = Instant.fromEpochMilliseconds(0),
        latitude = 0.0,
        longitude = 0.0,
        placeName = placeName,
        description = null,
        descriptionAttempts = 0,
        photos = photos,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = Instant.fromEpochMilliseconds(0),
        city = city,
    )

internal fun fakePhoto(
    id: String = "photo-1",
    remoteUrl: String? = "https://example.com/photo.jpg",
): Photo =
    Photo(
        id = id,
        uri = "content://photo/$id",
        takenAt = Instant.fromEpochMilliseconds(0),
        latitude = 0.0,
        longitude = 0.0,
        remoteUrl = remoteUrl,
    )

internal fun fakePlaceCandidate(
    id: String,
    tripId: String = "trip-1",
    name: String = "Rynok Square",
    addedByUserId: String = "uid-1",
    updatedAt: Instant = Instant.fromEpochMilliseconds(0),
): PlaceCandidate =
    PlaceCandidate(
        id = id,
        tripId = tripId,
        name = name,
        latitude = 0.0,
        longitude = 0.0,
        note = null,
        addedByUserId = addedByUserId,
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = updatedAt,
    )

internal fun fakePlaceSwipe(
    candidateId: String,
    userId: String,
    direction: SwipeDirection,
    tripId: String = "trip-1",
): PlaceSwipe =
    PlaceSwipe(
        id = "$candidateId::$userId",
        tripId = tripId,
        candidateId = candidateId,
        userId = userId,
        direction = direction,
        swipedAt = Instant.fromEpochMilliseconds(0),
        syncStatus = SyncStatus.SYNCED,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** Only [observeByTrip] is exercised by Home. */
internal class FakeDiaryEntryRepository : DiaryEntryRepository {
    val entries = MutableStateFlow<List<DiaryEntry>>(emptyList())

    override suspend fun upsert(entry: DiaryEntry): Unit = error("not used by Home")

    override suspend fun getById(id: String): DiaryEntry? = error("not used by Home")

    override fun observeByTrip(tripId: String): Flow<List<DiaryEntry>> = entries.map { list -> list.filter { it.tripId == tripId } }

    override suspend fun delete(id: String): Unit = error("not used by Home")
}

/** Only [observeByDiaryEntry] is exercised by Home. */
internal class FakeEpisodeRepository : EpisodeRepository {
    val episodes = MutableStateFlow<List<Episode>>(emptyList())

    override suspend fun upsert(episode: Episode): Unit = error("not used by Home")

    override suspend fun getById(id: String): Episode? = error("not used by Home")

    override fun observeByDiaryEntry(diaryEntryId: String): Flow<List<Episode>> =
        episodes.map { list -> list.filter { it.diaryEntryId == diaryEntryId } }

    override suspend fun delete(id: String): Unit = error("not used by Home")
}

/** Only [observeByTrip] is exercised by Home. */
internal class FakePlaceCandidateRepository : PlaceCandidateRepository {
    val candidates = MutableStateFlow<List<PlaceCandidate>>(emptyList())

    override suspend fun upsert(place: PlaceCandidate): Unit = error("not used by Home")

    override suspend fun getById(id: String): PlaceCandidate? = error("not used by Home")

    override fun observeByTrip(tripId: String): Flow<List<PlaceCandidate>> = candidates.map { list -> list.filter { it.tripId == tripId } }

    override suspend fun delete(id: String): Unit = error("not used by Home")
}

/** Only [observeByTrip] is exercised by Home. */
internal class FakePlaceSwipeRepository : PlaceSwipeRepository {
    val swipes = MutableStateFlow<List<PlaceSwipe>>(emptyList())

    override suspend fun upsert(swipe: PlaceSwipe): Unit = error("not used by Home")

    override suspend fun getById(id: String): PlaceSwipe? = error("not used by Home")

    override fun observeByTrip(tripId: String): Flow<List<PlaceSwipe>> = swipes.map { list -> list.filter { it.tripId == tripId } }
}
