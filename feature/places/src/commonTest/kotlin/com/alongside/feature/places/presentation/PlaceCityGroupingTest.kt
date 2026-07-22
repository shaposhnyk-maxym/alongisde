package com.alongside.feature.places.presentation

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private fun place(
    id: String,
    city: String?,
    countryCode: String? = null,
) = PlaceCandidate(
    id = id,
    tripId = "trip-1",
    name = "Place $id",
    latitude = 0.0,
    longitude = 0.0,
    note = null,
    addedByUserId = "owner-1",
    syncStatus = SyncStatus.PENDING,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0),
    city = city,
    countryCode = countryCode,
)

class PlaceCityGroupingTest {
    @Test
    fun `empty list groups to no groups at all`() {
        assertEquals(emptyList(), emptyList<PlaceCandidate>().groupedByCity())
    }

    @Test
    fun `a single city collects all its places into one group`() {
        val places = listOf(place("1", "Lviv"), place("2", "Lviv"))

        assertEquals(listOf(PlaceCityGroup("Lviv", places)), places.groupedByCity())
    }

    @Test
    fun `multiple cities are sorted alphabetically`() {
        val vinnytsiaPlace = place("1", "Vinnytsia")
        val lvivPlace = place("2", "Lviv")

        val groups = listOf(vinnytsiaPlace, lvivPlace).groupedByCity()

        assertEquals(
            listOf(
                PlaceCityGroup("Lviv", listOf(lvivPlace)),
                PlaceCityGroup("Vinnytsia", listOf(vinnytsiaPlace)),
            ),
            groups,
        )
    }

    @Test
    fun `a named group carries the countryCode of its places`() {
        val places = listOf(place("1", "Lviv", countryCode = "UA"), place("2", "Lviv", countryCode = "UA"))

        assertEquals(listOf(PlaceCityGroup("Lviv", places, countryCode = "UA")), places.groupedByCity())
    }

    @Test
    fun `places with no city are collected into one trailing group instead of dropped`() {
        val lvivPlace = place("1", "Lviv")
        val unknownPlace = place("2", null)

        val groups = listOf(lvivPlace, unknownPlace).groupedByCity()

        assertEquals(
            listOf(
                PlaceCityGroup("Lviv", listOf(lvivPlace)),
                PlaceCityGroup(null, listOf(unknownPlace)),
            ),
            groups,
        )
    }
}
