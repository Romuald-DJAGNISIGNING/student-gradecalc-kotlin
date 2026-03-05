package com.joelf.studentgradecalc.console.core

object NumericParser {
    private val missingTokens = setOf("na", "n/a", "none", "null", "absent", "missing", "-", "--", "x")

    fun parseFlexible(value: String?): Double? {
        val compact = value?.trim().orEmpty()
        if (compact.isEmpty()) return null
        if (compact.lowercase() in missingTokens) return null

        // Sheets often mix spaces, commas, symbols, and labels in score cells.
        var normalized = compact.replace(Regex("\\s+"), "")
        normalized = if (normalized.contains(',') && normalized.contains('.')) {
            normalized.replace(",", "")
        } else {
            normalized.replace(',', '.')
        }
        normalized = normalized.replace(Regex("[^0-9.\\-]"), "")

        if (normalized.isEmpty() || normalized == "-" || normalized == ".") return null
        if (normalized.count { it == '.' } > 1) return null

        return normalized.toDoubleOrNull()
    }

    fun inRange(value: Double, min: Double, max: Double): Boolean = value in min..max
}
