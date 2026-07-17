package com.alongside.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class AlongsideThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `display and body styles use bundled font families`() {
        var displayFamily: FontFamily? = null
        var bodyFamily: FontFamily? = null
        composeTestRule.setContent {
            AlongsideTheme {
                displayFamily = MaterialTheme.typography.displayLarge.fontFamily
                bodyFamily = MaterialTheme.typography.bodyLarge.fontFamily
            }
        }

        assertNotNull(displayFamily)
        assertNotEquals(FontFamily.Default, displayFamily)
        assertNotEquals(FontFamily.Serif, displayFamily)
        assertNotNull(bodyFamily)
        assertNotEquals(FontFamily.Default, bodyFamily)
    }

    @Test
    fun `extended colors expose sampled paper and toast tokens`() {
        var paper: Color? = null
        var toast: Color? = null
        composeTestRule.setContent {
            AlongsideTheme {
                paper = MaterialTheme.alongsideColors.paper
                toast = MaterialTheme.alongsideColors.toast
            }
        }

        assertEquals(Color(0xFFF9F5EC), paper)
        assertEquals(Color(0xFF22120A), toast)
    }

    @Test
    fun `extended typography provides bundled mono overline`() {
        var overlineFamily: FontFamily? = null
        composeTestRule.setContent {
            AlongsideTheme {
                overlineFamily = MaterialTheme.alongsideTypography.overline.fontFamily
            }
        }

        assertNotNull(overlineFamily)
        assertNotEquals(FontFamily.Monospace, overlineFamily)
    }

    @Test
    fun `small shape is a 10dp rounded rect, not a pill`() {
        var shape: Shape? = null
        var density: Density? = null
        composeTestRule.setContent {
            AlongsideTheme {
                shape = MaterialTheme.shapes.small
                density = LocalDensity.current
            }
        }

        val rounded = assertIs<androidx.compose.foundation.shape.RoundedCornerShape>(shape)
        val resolvedDensity = assertNotNull(density)
        val cornerPx = rounded.topStart.toPx(Size(100f, 100f), resolvedDensity)
        val expectedPx = with(resolvedDensity) { 10.dp.toPx() }
        assertEquals(expectedPx, cornerPx)
    }
}
