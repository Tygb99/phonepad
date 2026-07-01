package com.tygb99.phonepad

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HostInputPolicyTest {
    @Test
    fun macHostUsesControlSpace() {
        val preset = HostInputPolicy.resolvePreset(HostOsPreset.AUTO, "yong의 Mac mini")

        assertEquals(HostOsPreset.MAC, preset)
        assertEquals(
            KeyboardStroke(KeyboardReport.MOD_LEFT_CONTROL, KeyboardReport.KEY_SPACE),
            HostInputPolicy.languageToggleStroke(preset),
        )
    }

    @Test
    fun windowsHostUsesLang1ByDefault() {
        val preset = HostInputPolicy.resolvePreset(HostOsPreset.AUTO, "Windows 11 PC")

        assertEquals(HostOsPreset.WINDOWS_LANG1, preset)
        assertEquals(
            KeyboardStroke(0, KeyboardReport.KEY_LANG1),
            HostInputPolicy.languageToggleStroke(preset),
        )
    }

    @Test
    fun windowsRightAltFallbackUsesRightAltModifierOnly() {
        assertEquals(
            KeyboardStroke(KeyboardReport.MOD_RIGHT_ALT, 0),
            HostInputPolicy.languageToggleStroke(HostOsPreset.WINDOWS_RIGHT_ALT),
        )
    }

    @Test
    fun unknownAutoHostDoesNotSendAnyKey() {
        val preset = HostInputPolicy.resolvePreset(HostOsPreset.AUTO, "Living Room")

        assertEquals(HostOsPreset.AUTO, preset)
        assertNull(HostInputPolicy.languageToggleStroke(preset))
    }

    @Test
    fun unknownStorageValueFallsBackToAuto() {
        assertEquals(HostOsPreset.AUTO, HostOsPreset.fromStorageValue(null))
        assertEquals(HostOsPreset.AUTO, HostOsPreset.fromStorageValue("linux"))
    }

    @Test
    fun manualPresetCyclesThroughAvailableHostModes() {
        assertEquals(HostOsPreset.MAC, HostOsPreset.AUTO.nextManualPreset())
        assertEquals(HostOsPreset.WINDOWS_LANG1, HostOsPreset.MAC.nextManualPreset())
        assertEquals(HostOsPreset.WINDOWS_RIGHT_ALT, HostOsPreset.WINDOWS_LANG1.nextManualPreset())
        assertEquals(HostOsPreset.AUTO, HostOsPreset.WINDOWS_RIGHT_ALT.nextManualPreset())
    }

    @Test
    fun compactHostOsPressCyclesOnlyAtLongPressBoundary() {
        assertFalse(HostInputPolicy.shouldCycleCompactHostOsPress(0L, alreadyCycled = false))
        assertFalse(HostInputPolicy.shouldCycleCompactHostOsPress(999L, alreadyCycled = false))
        assertTrue(HostInputPolicy.shouldCycleCompactHostOsPress(1000L, alreadyCycled = false))
    }

    @Test
    fun compactHostOsPressCannotCycleTwiceForSamePress() {
        assertFalse(HostInputPolicy.shouldCycleCompactHostOsPress(1200L, alreadyCycled = true))
    }
}
