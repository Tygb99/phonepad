package com.tygb99.phonepad

/**
 * Only a short, still touch is a click. Holding longer than [TAP_MAX_MS] is a move even if the
 * finger never left the spot (owner 2026-10-30: long presses are movement, not clicks). A touch
 * that ends [SMALL_MOVE_NET_DP]+ away from its start after [SMALL_MOVE_MIN_MS]+ is a cursor nudge.
 */
object TapPolicy {
    const val TAP_SLOP_DP = 8f
    const val TAP_MAX_MS = 300L
    const val SMALL_MOVE_NET_DP = 4f
    const val SMALL_MOVE_MIN_MS = 150L

    fun isTap(pathDp: Float, netDp: Float, durationMs: Long): Boolean {
        if (pathDp >= TAP_SLOP_DP) return false
        if (durationMs > TAP_MAX_MS) return false
        val deliberateSmallMove = netDp >= SMALL_MOVE_NET_DP && durationMs >= SMALL_MOVE_MIN_MS
        return !deliberateSmallMove
    }
}
