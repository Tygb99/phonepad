package com.tygb99.phonepad

object HidForegroundServicePolicy {
    fun shouldRun(
        appRegistered: Boolean,
        connected: Boolean,
        connectionPending: Boolean,
        discoverablePending: Boolean,
    ): Boolean {
        return connected || connectionPending || discoverablePending
    }
}
