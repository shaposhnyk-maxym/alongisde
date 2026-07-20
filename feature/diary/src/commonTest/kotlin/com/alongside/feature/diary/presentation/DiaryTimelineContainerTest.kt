package com.alongside.feature.diary.presentation

import com.alongside.core.domain.diary.DayUnlockState
import com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline
import com.alongside.core.model.diary.Photo
import com.alongside.feature.diary.FakeAuthSessionCache
import com.alongside.feature.diary.FakeDiaryContentPuller
import com.alongside.feature.diary.FakeDiaryEntryRepository
import com.alongside.feature.diary.FakeEpisodeRepository
import com.alongside.feature.diary.FakeExifPhotoReader
import com.alongside.feature.diary.FakeGeocodingClient
import com.alongside.feature.diary.FakePairingRepository
import com.alongside.feature.diary.FakePhotoUploadClient
import com.alongside.feature.diary.FakeVisionClient
import com.alongside.feature.diary.capture.ExifPhotoReader
import com.alongside.feature.diary.fakeTrip
import com.alongside.feature.diary.testAuthSession
import com.alongside.feature.diary.testDiaryEntry
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_TODAY = LocalDate(2026, 7, 19)
private val FIXED_NOW = Instant.parse("2026-07-19T12:00:00Z")

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class DiaryTimelineContainerTest {
    private val pairingRepository = FakePairingRepository()
    private val diaryEntryRepository = FakeDiaryEntryRepository()
    private val episodeRepository = FakeEpisodeRepository()

    private fun containerUnderTest(
        uid: String = "owner-1",
        exifPhotoReader: ExifPhotoReader = FakeExifPhotoReader(emptyMap()),
    ): DiaryTimelineContainer {
        val captureCoordinator =
            DiaryCaptureCoordinator(
                diaryEntryRepository = diaryEntryRepository,
                episodeRepository = episodeRepository,
                processingPipeline =
                    EpisodeProcessingPipeline(
                        geocodingClient = FakeGeocodingClient(),
                        visionDescriptionClient = FakeVisionClient(),
                        imageBytesLoader = { byteArrayOf(1) },
                        photoUploadClient = FakePhotoUploadClient(),
                        clock = FixedClock,
                    ),
                exifPhotoReader = exifPhotoReader,
                clock = FixedClock,
            )
        return DiaryTimelineContainer(
            authSessionCache = FakeAuthSessionCache(testAuthSession(uid)),
            timelineDataSource =
                DiaryTimelineDataSource(
                    pairingRepository = pairingRepository,
                    diaryEntryRepository = diaryEntryRepository,
                    episodeRepository = episodeRepository,
                    diaryContentPuller = FakeDiaryContentPuller(),
                    captureCoordinator = captureCoordinator,
                ),
            captureCoordinator = captureCoordinator,
            clock = FixedClock,
        )
    }

    @Test
    fun `with no active trip the timeline stays empty`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                val state = awaitState()
                assertEquals(FIXED_TODAY, state.today)
                assertEquals("owner-1", state.ownUserId)
                assertEquals(emptyList(), state.items)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `an unlocked day carries both sides' episodes and a locked day carries its waiting reason`() =
        runTest {
            val trip =
                fakeTrip(
                    id = "trip-1",
                    ownerId = "owner-1",
                    memberId = "partner-1",
                    startDate = FIXED_TODAY,
                    endDate = FIXED_TODAY.plus(1, DateTimeUnit.DAY),
                )
            pairingRepository.activeTrip.value = trip
            val ownEntry =
                testDiaryEntry(
                    id = "own-1",
                    tripId = "trip-1",
                    userId = "owner-1",
                    date = FIXED_TODAY,
                    closedAt = FIXED_NOW,
                )
            val partnerEntry =
                testDiaryEntry(
                    id = "partner-1",
                    tripId = "trip-1",
                    userId = "partner-1",
                    date = FIXED_TODAY,
                    closedAt = FIXED_NOW,
                )
            diaryEntryRepository.upsert(ownEntry)
            diaryEntryRepository.upsert(partnerEntry)

            containerUnderTest().test(this) {
                runOnCreate()
                awaitState() // today/ownUserId bootstrap
                val loaded = awaitState()

                assertEquals(trip, loaded.trip)
                assertEquals("partner-1", loaded.partnerUserId)
                assertEquals(listOf(ownEntry), loaded.ownEntries)
                assertEquals(listOf(partnerEntry), loaded.partnerEntries)

                val items = loaded.items
                // Reunion day is trip.startDate, which is today in this fixture - the countdown
                // has already reached zero and the carousel opens straight on Day 1.
                assertEquals(2, items.size)

                val day1 = assertIs<DiaryTimelineItem.Day>(items[0]).card
                assertEquals(DayUnlockState.UNLOCKED, day1.unlockState)
                assertEquals(null, day1.waitingState)

                val day2 = assertIs<DiaryTimelineItem.Day>(items[1]).card
                assertEquals(DayUnlockState.LOCKED, day2.unlockState)
                assertEquals(DiaryDayWaitingState.PARTNER_CAPTURING, day2.waitingState)

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `the countdown shows while the reunion day is still ahead`() =
        runTest {
            val trip =
                fakeTrip(
                    id = "trip-1",
                    ownerId = "owner-1",
                    memberId = "partner-1",
                    startDate = FIXED_TODAY.plus(3, DateTimeUnit.DAY),
                    endDate = FIXED_TODAY.plus(4, DateTimeUnit.DAY),
                )
            pairingRepository.activeTrip.value = trip

            containerUnderTest().test(this) {
                runOnCreate()
                awaitState() // today/ownUserId bootstrap
                val loaded = awaitState()

                val countdown = assertIs<DiaryTimelineItem.Countdown>(loaded.items.first())
                assertEquals(3, countdown.daysUntilReunion)

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `processing captured photos marks the day as generating text then clears it once persisted`() =
        runTest {
            val trip =
                fakeTrip(id = "trip-1", ownerId = "owner-1", memberId = "partner-1", startDate = FIXED_TODAY, endDate = FIXED_TODAY)
            pairingRepository.activeTrip.value = trip
            val photo = Photo(id = "p1", uri = "content://p1", takenAt = FIXED_NOW, latitude = 1.0, longitude = 1.0)

            containerUnderTest(exifPhotoReader = FakeExifPhotoReader(mapOf("content://p1" to photo))).test(this) {
                runOnCreate()
                awaitState() // today/ownUserId bootstrap
                awaitState() // trip loaded, no entries yet

                containerHost.onIntent(DiaryTimelineIntent.ProcessCapturedPhotos(FIXED_TODAY, listOf("content://p1")))
                val generating = awaitState()
                assertEquals(FIXED_TODAY, generating.processingOwnDate)

                cancelAndIgnoreRemainingItems()
            }

            assertEquals(1, diaryEntryRepository.upserted.size)
            assertEquals(FIXED_TODAY, diaryEntryRepository.upserted.single().date)
            assertTrue(episodeRepository.upserted.isNotEmpty())
        }

    @Test
    fun `closing the day upserts today's own entry with a non-null closedAt`() =
        runTest {
            val trip =
                fakeTrip(id = "trip-1", ownerId = "owner-1", memberId = "partner-1", startDate = FIXED_TODAY, endDate = FIXED_TODAY)
            pairingRepository.activeTrip.value = trip
            val ownEntry = testDiaryEntry(id = "own-1", tripId = "trip-1", userId = "owner-1", date = FIXED_TODAY)
            diaryEntryRepository.upsert(ownEntry)

            containerUnderTest().test(this) {
                runOnCreate()
                awaitState() // today/ownUserId bootstrap
                awaitState() // trip + entry loaded

                containerHost.onIntent(DiaryTimelineIntent.CloseDay(FIXED_TODAY))
                awaitState() // reactive re-emit once closedAt lands on the entry

                cancelAndIgnoreRemainingItems()
            }

            val closed = diaryEntryRepository.upserted.last()
            assertEquals("own-1", closed.id)
            assertEquals(FIXED_NOW, closed.closedAt)
        }

    @Test
    fun `processing captured photos for a day other than today writes to that day's date`() =
        runTest {
            val otherDate = FIXED_TODAY.plus(1, DateTimeUnit.DAY)
            val trip =
                fakeTrip(id = "trip-1", ownerId = "owner-1", memberId = "partner-1", startDate = FIXED_TODAY, endDate = otherDate)
            pairingRepository.activeTrip.value = trip
            val photo = Photo(id = "p1", uri = "content://p1", takenAt = FIXED_NOW, latitude = 1.0, longitude = 1.0)

            containerUnderTest(exifPhotoReader = FakeExifPhotoReader(mapOf("content://p1" to photo))).test(this) {
                runOnCreate()
                awaitState() // today/ownUserId bootstrap
                awaitState() // trip loaded, no entries yet

                containerHost.onIntent(DiaryTimelineIntent.ProcessCapturedPhotos(otherDate, listOf("content://p1")))
                awaitState()

                cancelAndIgnoreRemainingItems()
            }

            assertEquals(otherDate, diaryEntryRepository.upserted.single().date)
        }

    @Test
    fun `closing a day other than today upserts that day's entry, not today's`() =
        runTest {
            val otherDate = FIXED_TODAY.plus(1, DateTimeUnit.DAY)
            val trip =
                fakeTrip(id = "trip-1", ownerId = "owner-1", memberId = "partner-1", startDate = FIXED_TODAY, endDate = otherDate)
            pairingRepository.activeTrip.value = trip
            val entry = testDiaryEntry(id = "other-1", tripId = "trip-1", userId = "owner-1", date = otherDate)
            diaryEntryRepository.upsert(entry)

            containerUnderTest().test(this) {
                runOnCreate()
                awaitState() // today/ownUserId bootstrap
                awaitState() // trip + entry loaded

                containerHost.onIntent(DiaryTimelineIntent.CloseDay(otherDate))
                awaitState() // reactive re-emit once closedAt lands on the entry

                cancelAndIgnoreRemainingItems()
            }

            val closed = diaryEntryRepository.upserted.last()
            assertEquals("other-1", closed.id)
            assertEquals(FIXED_NOW, closed.closedAt)
        }

    @Test
    fun `closing the day with no entry for today is a no-op`() =
        runTest {
            val trip =
                fakeTrip(id = "trip-1", ownerId = "owner-1", memberId = "partner-1", startDate = FIXED_TODAY, endDate = FIXED_TODAY)
            pairingRepository.activeTrip.value = trip

            containerUnderTest().test(this) {
                runOnCreate()
                awaitState() // today/ownUserId bootstrap
                awaitState() // trip loaded, no entries yet

                containerHost.onIntent(DiaryTimelineIntent.CloseDay(FIXED_TODAY))

                cancelAndIgnoreRemainingItems()
            }

            assertEquals(0, diaryEntryRepository.upserted.size)
        }
}
