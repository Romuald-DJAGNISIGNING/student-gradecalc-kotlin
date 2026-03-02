package com.joelf.studentgradecalc.data.io

object NumericParser {
    fun parseFlexible(value: String?): Double? {
        val compact = value?.trim().orEmpty()
        if (compact.isEmpty()) return null

        val normalized = compact
            .replace(Regex("\\s+"), "")
            .replace(',', '.')
            .replace(Regex("[^0-9.\\-]"), "")

        if (normalized.isEmpty() || normalized == "-" || normalized == ".") return null
        if (normalized.count { it == '.' } > 1) return null

        return normalized.toDoubleOrNull()
    }

    fun inRange(value: Double, min: Double, max: Double): Boolean {
        return value in min..max
    }
}
