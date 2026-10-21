package com.tygb99.phonepad

import java.time.LocalDate

/**
 * 한영 버튼으로 보낸 횟수를 날짜·방식별로 센다(입력기 시험 전후 비교용).
 * 값은 [PREFS_NAME] SharedPreferences에 `toggle_<날짜>_<방식>` 키의 Int로 쌓인다.
 */
object LanguageToggleTally {
    const val PREFS_NAME = "language_toggle_counts"
    const val RECENT_DAYS = 7
    private const val KEY_PREFIX = "toggle_"

    fun key(date: LocalDate, preset: HostOsPreset): String = "$KEY_PREFIX${date}_${preset.storageValue}"

    fun recentLines(counts: Map<String, Int>, today: LocalDate, days: Int = RECENT_DAYS): List<String> {
        return (0 until days).mapNotNull { offset ->
            val date = today.minusDays(offset.toLong())
            val parts = HostOsPreset.entries.mapNotNull { preset ->
                counts[key(date, preset)]?.takeIf { it > 0 }?.let { "${preset.shortLabel} $it" }
            }
            if (parts.isEmpty()) null else "$date ${parts.joinToString(" · ")}"
        }
    }
}
