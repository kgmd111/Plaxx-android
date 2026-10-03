package com.plaxx.app.domain

enum class GroupType(val order: Int, val label: String) {
    W(1, "Wyklad"),
    C(2, "Cwiczenia"),
    L(3, "Laboratorium"),
    LK(4, "Lab. komputerowe"),
    P(5, "Projekt"),
    LEK(6, "Lektorat"),
    S(7, "Seminarium"),
    INNE(9, "Inne");

    companion object {
        /**
         * KOLEJNOSC SPRAWDZEN JEST KLUCZOWA:
         * LEK przed L, LK przed L - inaczej "LK1" wpadnie do "L".
         */
        fun from(group: String): GroupType {
            val s = group.uppercase().trim()
            return when {
                s.startsWith("LEK") -> LEK
                s.startsWith("LK") -> LK
                s.startsWith("L") -> L
                s.startsWith("P") -> P
                s.startsWith("\u0106") || s.startsWith("C") -> C
                s.startsWith("W") -> W
                s.startsWith("S") -> S
                else -> INNE
            }
        }
    }
}
