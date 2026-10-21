package com.tygb99.phonepad

enum class HostOsPreset(val storageValue: String, val label: String, val shortLabel: String) {
    AUTO("auto", "자동 추정", "자동"),
    MAC("mac", "Mac: Control + Space", "Mac"),
    MAC_RIGHT_OPTION("mac_right_option", "Mac: 오른쪽 Option 단독 (Araso)", "Mac Opt"),
    WINDOWS_LANG1("windows_lang1", "Windows: 한/영 키", "Win 한/영"),
    WINDOWS_RIGHT_ALT("windows_right_alt", "Windows: 오른쪽 Alt", "Win Alt");

    fun nextManualPreset(): HostOsPreset {
        return when (this) {
            AUTO -> MAC
            MAC -> MAC_RIGHT_OPTION
            MAC_RIGHT_OPTION -> WINDOWS_LANG1
            WINDOWS_LANG1 -> WINDOWS_RIGHT_ALT
            WINDOWS_RIGHT_ALT -> AUTO
        }
    }

    companion object {
        fun fromStorageValue(value: String?): HostOsPreset {
            return entries.firstOrNull { it.storageValue == value } ?: AUTO
        }
    }
}

data class KeyboardStroke(
    val modifier: Int,
    val keyUsage: Int,
)

object KeyboardReport {
    const val MOD_LEFT_CONTROL = 0x01
    const val MOD_RIGHT_ALT = 0x40
    const val KEY_SPACE = 0x2C
    const val KEY_LANG1 = 0x90
}

object HostInputPolicy {
    const val COMPACT_HOST_OS_LONG_PRESS_MS = 1000L

    fun resolvePreset(savedPreset: HostOsPreset, hostName: String?): HostOsPreset {
        if (savedPreset != HostOsPreset.AUTO) return savedPreset
        val normalized = hostName.orEmpty().lowercase()
        return when {
            normalized.contains("mac") ||
                normalized.contains("macbook") ||
                normalized.contains("imac") -> HostOsPreset.MAC
            normalized.contains("windows") ||
                normalized.contains("win11") ||
                normalized.contains("win10") ||
                normalized.contains(" pc") ||
                normalized.endsWith("pc") -> HostOsPreset.WINDOWS_LANG1
            else -> HostOsPreset.AUTO
        }
    }

    fun languageToggleStroke(preset: HostOsPreset): KeyboardStroke? {
        return when (preset) {
            HostOsPreset.AUTO -> null
            HostOsPreset.MAC -> KeyboardStroke(KeyboardReport.MOD_LEFT_CONTROL, KeyboardReport.KEY_SPACE)
            HostOsPreset.WINDOWS_LANG1 -> KeyboardStroke(0, KeyboardReport.KEY_LANG1)
            // Mac의 오른쪽 Option은 HID Right Alt와 같은 신호다. Araso 입력기의 기본 고치기 키.
            HostOsPreset.MAC_RIGHT_OPTION,
            HostOsPreset.WINDOWS_RIGHT_ALT -> KeyboardStroke(KeyboardReport.MOD_RIGHT_ALT, 0)
        }
    }

    fun shouldCycleCompactHostOsPress(pressDurationMs: Long, alreadyCycled: Boolean): Boolean {
        return !alreadyCycled && pressDurationMs >= COMPACT_HOST_OS_LONG_PRESS_MS
    }
}
