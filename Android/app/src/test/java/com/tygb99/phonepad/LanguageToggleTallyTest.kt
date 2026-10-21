package com.tygb99.phonepad

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageToggleTallyTest {
    private val today = LocalDate.of(2026, 10, 21)

    @Test
    fun keySeparatesDateAndPreset() {
        assertEquals("toggle_2026-10-21_mac", LanguageToggleTally.key(today, HostOsPreset.MAC))
        assertEquals(
            "toggle_2026-10-21_mac_right_option",
            LanguageToggleTally.key(today, HostOsPreset.MAC_RIGHT_OPTION),
        )
    }

    @Test
    fun recentLinesListPressedDaysNewestFirstAndSkipOldOnes() {
        val counts = mapOf(
            LanguageToggleTally.key(today, HostOsPreset.MAC) to 12,
            LanguageToggleTally.key(today, HostOsPreset.MAC_RIGHT_OPTION) to 3,
            LanguageToggleTally.key(today.minusDays(2), HostOsPreset.MAC) to 40,
            LanguageToggleTally.key(today.minusDays(7), HostOsPreset.MAC) to 99,
            "unrelated" to 5,
        )

        assertEquals(
            listOf("2026-10-21 Mac 12 · Mac Opt 3", "2026-10-19 Mac 40"),
            LanguageToggleTally.recentLines(counts, today),
        )
    }

    @Test
    fun recentLinesEmptyWhenNothingPressed() {
        assertEquals(emptyList(), LanguageToggleTally.recentLines(emptyMap(), today))
    }
}
