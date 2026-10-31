package com.futo.platformplayer.thor

/** Pure screen selection: display IDs are deliberately not assumed to be 0 and 1. */
object ThorScreenPolicy {
    data class Screen(val id: Int, val width: Int, val height: Int, val primary: Boolean)
    data class Layout(val controls: Int, val video: Int)

    fun select(screens: List<Screen>, swap: Boolean): Layout? {
        val available = screens.filter { it.width > 0 && it.height > 0 }.distinctBy { it.id }
        val main = available.firstOrNull { it.primary } ?: return null
        val lower = available.filter { it.id != main.id }
            .minWithOrNull(compareBy<Screen> { it.width.toLong() * it.height }.thenBy { it.id })
            ?: return null
        return if (swap) Layout(main.id, lower.id) else Layout(lower.id, main.id)
    }
}
