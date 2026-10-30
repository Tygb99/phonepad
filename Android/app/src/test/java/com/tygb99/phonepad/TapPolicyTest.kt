package com.tygb99.phonepad

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TapPolicyTest {
    @Test
    fun stillOrQuickTouchesStayTaps() {
        assertTrue(TapPolicy.isTap(pathDp = 0f, netDp = 0f, durationMs = 90))
        assertTrue(TapPolicy.isTap(pathDp = 0f, netDp = 0f, durationMs = 900))
        assertTrue(TapPolicy.isTap(pathDp = 6f, netDp = 5f, durationMs = 100))
    }

    @Test
    fun slowPressThatWobblesAroundOneSpotStillClicks() {
        assertTrue(TapPolicy.isTap(pathDp = 7.5f, netDp = 1f, durationMs = 600))
        assertTrue(TapPolicy.isTap(pathDp = 7.9f, netDp = 3.9f, durationMs = 400))
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
