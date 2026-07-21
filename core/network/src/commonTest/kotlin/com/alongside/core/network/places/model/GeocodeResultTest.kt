package com.alongside.core.network.places.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GeocodeResultTest {
    private fun component(
        longName: String,
        vararg types: String,
    ) = AddressComponent(longName = longName, shortName = longName, types = types.toList())

    @Test
    fun `prefers point_of_interest over everything else`() {
        val result =
            GeocodeResult(
                formattedAddress = "1 Fake St, City",
                addressComponents =
                    listOf(
                        component("City", "locality"),
                        component("Landmark", "point_of_interest"),
                        component("Downtown", "neighborhood"),
                    ),
            )

        assertEquals("Landmark", result.preferredPlaceName())
    }

    @Test
    fun `prefers neighborhood over route and locality`() {
        val result =
            GeocodeResult(
                formattedAddress = "1 Fake St, City",
                addressComponents =
                    listOf(
                        component("Fake St", "route"),
                        component("City", "locality"),
                        component("Downtown", "neighborhood"),
                    ),
            )

        assertEquals("Downtown", result.preferredPlaceName())
    }

    @Test
    fun `prefers route over locality when no point_of_interest or neighborhood is tagged`() {
        // Matches the real Google Geocoding response seen in M10's manual test: an address whose
        // components are only street_number/route/locality/administrative_area/... - no
        // point_of_interest or neighborhood component, even when the result as a whole is one.
        val result =
            GeocodeResult(
                formattedAddress = "1 Viale Bruno Buozzi, Arezzo",
                addressComponents =
                    listOf(
                        component("1", "street_number"),
                        component("Viale Bruno Buozzi", "route"),
                        component("Arezzo", "locality", "political"),
                        component("Arezzo", "administrative_area_level_3", "political"),
                    ),
            )

        assertEquals("Viale Bruno Buozzi", result.preferredPlaceName())
    }

    @Test
    fun `falls back to locality when nothing more specific is tagged`() {
        val result =
            GeocodeResult(
                formattedAddress = "City, Country",
                addressComponents = listOf(component("City", "locality", "political")),
            )

        assertEquals("City", result.preferredPlaceName())
    }

    @Test
    fun `falls back to the formatted address when no preferred component type is present`() {
        val result =
            GeocodeResult(
                formattedAddress = "Somewhere, Country",
                addressComponents = listOf(component("Country", "country", "political")),
            )

        assertEquals("Somewhere, Country", result.preferredPlaceName())
    }

    @Test
    fun `falls back to the formatted address when there are no address components at all`() {
        val result = GeocodeResult(formattedAddress = "Unnamed area", addressComponents = emptyList())

        assertEquals("Unnamed area", result.preferredPlaceName())
    }

    @Test
    fun `cityName prefers locality over route and point_of_interest`() {
        val result =
            GeocodeResult(
                formattedAddress = "1 Fake St, City",
                addressComponents =
                    listOf(
                        component("Landmark", "point_of_interest"),
                        component("Fake St", "route"),
                        component("Lviv", "locality", "political"),
                    ),
            )

        assertEquals("Lviv", result.cityName())
    }

    @Test
    fun `cityName falls back to administrative_area_level_2 when no locality is tagged`() {
        val result =
            GeocodeResult(
                formattedAddress = "1 Fake St, Region",
                addressComponents =
                    listOf(
                        component("Fake St", "route"),
                        component("Some Region", "administrative_area_level_2", "political"),
                    ),
            )

        assertEquals("Some Region", result.cityName())
    }

    @Test
    fun `cityName falls back to administrative_area_level_1 when neither locality nor level_2 is tagged`() {
        val result =
            GeocodeResult(
                formattedAddress = "Somewhere, State",
                addressComponents = listOf(component("Some State", "administrative_area_level_1", "political")),
            )

        assertEquals("Some State", result.cityName())
    }

    @Test
    fun `cityName is null when nothing in the cascade is tagged unlike preferredPlaceName's formatted-address fallback`() {
        val result =
            GeocodeResult(
                formattedAddress = "Somewhere, Country",
                addressComponents = listOf(component("Country", "country", "political")),
            )

        assertNull(result.cityName())
    }
}
