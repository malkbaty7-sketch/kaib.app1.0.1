package com.example

import androidx.compose.ui.text.font.FontWeight
import com.example.data.model.LineSpacingPreset
import com.example.data.model.ReaderFontWeightOption
import com.example.data.model.ReaderSettingsState
import com.example.data.model.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSettingsTest {

    @Test
    fun testDefaultReaderSettings() {
        val defaultState = ReaderSettingsState()
        assertEquals(ReaderTheme.LIGHT, defaultState.theme)
        assertEquals(16f, defaultState.fontSizeSp, 0.01f)
        assertEquals(FontWeight.Normal, defaultState.fontWeight)
        assertEquals(1.6f, defaultState.lineHeightMultiplier, 0.01f)
    }

    @Test
    fun testReaderThemesExistAndHaveDistinctTitles() {
        val themes = ReaderTheme.values()
        assertEquals(4, themes.size)
        assertTrue(themes.any { it == ReaderTheme.LIGHT })
        assertTrue(themes.any { it == ReaderTheme.DARK })
        assertTrue(themes.any { it == ReaderTheme.SEPIA })
        assertTrue(themes.any { it == ReaderTheme.CALM_GRAY })

        themes.forEach { theme ->
            assertNotNull(theme.title)
            assertNotNull(theme.backgroundColor)
            assertNotNull(theme.textColor)
        }
    }

    @Test
    fun testLineSpacingPresets() {
        val presets = LineSpacingPreset.values()
        assertTrue(presets.isNotEmpty())
        assertEquals(1.3f, LineSpacingPreset.COMPACT.multiplier, 0.01f)
        assertEquals(1.6f, LineSpacingPreset.BALANCED.multiplier, 0.01f)
        assertEquals(1.9f, LineSpacingPreset.WIDE.multiplier, 0.01f)
        assertEquals(2.2f, LineSpacingPreset.RELAXED.multiplier, 0.01f)
    }

    @Test
    fun testFontWeightOptions() {
        val options = ReaderFontWeightOption.values()
        assertEquals(3, options.size)
        assertEquals(FontWeight.Normal, ReaderFontWeightOption.NORMAL.weight)
        assertEquals(FontWeight.Medium, ReaderFontWeightOption.MEDIUM.weight)
        assertEquals(FontWeight.Bold, ReaderFontWeightOption.BOLD.weight)
    }
}
