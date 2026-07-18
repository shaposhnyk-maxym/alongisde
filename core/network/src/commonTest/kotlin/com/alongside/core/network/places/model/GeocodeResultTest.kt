package com.alongside.core.network.places.model

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
