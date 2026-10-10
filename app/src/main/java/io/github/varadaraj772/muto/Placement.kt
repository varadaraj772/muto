package io.github.varadaraj772.muto

enum class Placement {
    TABLE,
    HAND,
    POCKET
}

object Scenario {
    const val PREFS = "muto_prefs"
    const val SILENT = "SILENT"
    const val VIBRATE = "VIBRATE"
    const val RING = "RING"

    fun key(p: Placement, screenOn: Boolean): String =
        "scen_${p.name.lowercase()}_${if (screenOn) "on" else "off"}"

    fun default(p: Placement, screenOn: Boolean): String = when (p to screenOn) {
        Placement.TABLE to false -> RING
        Placement.TABLE to true -> VIBRATE
        Placement.HAND to false -> VIBRATE
        Placement.HAND to true -> SILENT
        Placement.POCKET to false -> RING
        else -> SILENT
    }
}