package com.tygb99.phonepad

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TapPolicyTest {
    @Test
    fun shortStillTouchesAreTaps() {
        assertTrue(TapPolicy.isTap(pathDp = 0f, netDp = 0f, durationMs = 90))
        assertTrue(TapPolicy.isTap(pathDp = 3.3f, netDp = 3.3f, durationMs = 116))
        assertTrue(TapPolicy.isTap(pathDp = 1f, netDp = 1f, durationMs = 239))
        assertTrue(TapPolicy.isTap(pathDp = 6f, netDp = 5f, durationMs = 100))
        assertTrue(TapPolicy.isTap(pathDp = 0f, netDp = 0f, durationMs = TapPolicy.TAP_MAX_MS))
    }

    @Test
    fun longPressIsMovementNotAClickEvenWhenStill() {
        assertFalse(TapPolicy.isTap(pathDp = 0f, netDp = 0f, durationMs = TapPolicy.TAP_MAX_MS + 1))
        assertFalse(TapPolicy.isTap(pathDp = 1.8f, netDp = 1.8f, durationMs = 1742))
        assertFalse(TapPolicy.isTap(pathDp = 3.6f, netDp = 2.8f, durationMs = 1066))
        assertFalse(TapPolicy.isTap(pathDp = 4f, netDp = 4f, durationMs = 441))
    }

    @Test
    fun shortDeliberateMoveThenLiftIsNotAClick() {
        assertFalse(TapPolicy.isTap(pathDp = 5f, netDp = 5f, durationMs = 300))
        assertFalse(TapPolicy.isTap(pathDp = 7f, netDp = 4f, durationMs = 150))
    }

    @Test
    fun travelBeyondSlopIsNeverATap() {
        assertFalse(TapPolicy.isTap(pathDp = 8f, netDp = 0f, durationMs = 80))
        assertFalse(TapPolicy.isTap(pathDp = 20f, netDp = 1f, durationMs = 80))
    }
}
