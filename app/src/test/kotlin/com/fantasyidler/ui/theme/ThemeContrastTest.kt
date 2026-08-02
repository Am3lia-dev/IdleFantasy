package com.fantasyidler.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ThemeContrastTest {

    @Test
    fun `dark background and surface support enhanced contrast text`() {
        assertContrastAtLeast(ParchmentText, DarkBackground, 7.0)
        assertContrastAtLeast(ParchmentText, DarkSurface, 7.0)
    }

    @Test
    fun `dark surface variant supports normal contrast muted text`() {
        assertContrastAtLeast(ParchmentTextMuted, DarkSurfaceVariant, 4.5)
    }

    @Test
    fun `dark containers remain visually distinct`() {
        assertEquals(3, setOf(DarkBackground, DarkSurface, DarkSurfaceVariant).size)
    }

    private fun assertContrastAtLeast(foreground: Color, background: Color, minimum: Double) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("Expected contrast of at least $minimum, but was $ratio", ratio >= minimum)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val lighter = max(relativeLuminance(first), relativeLuminance(second))
        val darker = min(relativeLuminance(first), relativeLuminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double =
        0.2126 * linearize(color.red.toDouble()) +
            0.7152 * linearize(color.green.toDouble()) +
            0.0722 * linearize(color.blue.toDouble())

    private fun linearize(component: Double): Double =
        if (component <= 0.04045) {
            component / 12.92
        } else {
            ((component + 0.055) / 1.055).pow(2.4)
        }
}
