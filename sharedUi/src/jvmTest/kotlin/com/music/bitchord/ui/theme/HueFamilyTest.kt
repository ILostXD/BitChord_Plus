package com.music.bitchord.ui.theme

import com.music.bitchord.ui.graphics.Swatch
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HueFamilyTest {
    @Test
    fun skyOverGoldAndSandStaysBlue() {
        // Astroworld-like: a big blue sky, a gold statue, grey sand.
        val family = hueFamily(
                listOf(
                    Swatch(0x6E9CD2, 450), // sky blue
                    Swatch(0x5A86C0, 200), // deeper blue
                    Swatch(0xC9A23A, 250), // gold
                    Swatch(0x9A9488, 300), // grey sand
                ),
            )
        assertNotNull(family)
        assertTrue("expected blue, got $family", family!!.blue > family.red && family.blue > family.green)
    }

    @Test
    fun mostlyBlackCoverHasNoFamily() {
        // UTOPIA-like: mostly black with some skin tone.
        assertNull(
            hueFamily(
                listOf(
                    Swatch(0x080808, 800),
                    Swatch(0x1A1A1A, 150),
                    Swatch(0x8A5A44, 60),
                ),
            ),
        )
    }
}
