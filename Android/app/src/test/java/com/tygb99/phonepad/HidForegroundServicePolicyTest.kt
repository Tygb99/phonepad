package com.tygb99.phonepad

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HidForegroundServicePolicyTest {
    @Test
    fun serviceRunsOnlyForHidSetupConnectingOrConnectedStates() {
        assertTrue(
            HidForegroundServicePolicy.shouldRun(
                appRegistered = false,
                connected = false,
                connectionPending = true,
                discoverablePending = false,
            ),
        )
        assertTrue(
            HidForegroundServicePolicy.shouldRun(
                appRegistered = true,
                connected = false,
                connectionPending = false,
                discoverablePending = true,
            ),
        )
        assertTrue(
            HidForegroundServicePolicy.shouldRun(
                appRegistered = true,
                connected = true,
                connectionPending = false,
                discoverablePending = false,
            ),
        )

        assertFalse(
            HidForegroundServicePolicy.shouldRun(
                appRegistered = true,
                connected = false,
                connectionPending = false,
                discoverablePending = false,
            ),
        )
    }
}
