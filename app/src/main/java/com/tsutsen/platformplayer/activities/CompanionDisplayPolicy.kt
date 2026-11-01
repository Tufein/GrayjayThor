package com.tsutsen.platformplayer.activities

/** Deterministic display choice without Android window or lifecycle dependencies. */
internal object CompanionDisplayPolicy {
    data class Screen(
        val id: Int,
        val valid: Boolean = true,
        val awake: Boolean = true,
        val isPrivate: Boolean = false,
    )

    fun select(displays: List<Screen>, activityDisplayId: Int, currentDisplayId: Int? = null): Int? {
        val eligible = displays.filter {
            it.id != activityDisplayId && it.valid && it.awake && !it.isPrivate
        }
        // Keep the existing window if it is still usable; enumeration order can
        // change after another display is connected or its configuration changes.
        return eligible.firstOrNull { it.id == currentDisplayId }?.id
            ?: eligible.minByOrNull { it.id }?.id
    }
}
