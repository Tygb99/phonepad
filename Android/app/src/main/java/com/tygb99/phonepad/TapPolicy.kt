package com.tygb99.phonepad

/**
 * A touch that ends [SMALL_MOVE_NET_DP]+ away from its start after [SMALL_MOVE_MIN_MS]+ was a cursor
 * nudge, not a tap, even inside the slop; a slow press wobbling around one spot still clicks.
 */
object TapPolicy {
    const val TAP_SLOP_DP = 8f
    const val SMALL_MOVE_NET_DP = 4f
    const val SMALL_MOVE_MIN_MS = 150L

    fun isTap(pathDp: Float, netDp: Float, durationMs: Long): Boolean {
        if (pathDp >= TAP_SLOP_DP) return false
        val deliberateSmallMove = netDp >= SMALL_MOVE_NET_DP && durationMs >= SMALL_MOVE_MIN_MS
        return !deliberateSmallMove
    }
}
