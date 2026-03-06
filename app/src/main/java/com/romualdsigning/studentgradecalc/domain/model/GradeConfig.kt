package com.romualdsigning.studentgradecalc.domain.model

enum class LetterGrade(val label: String) {
    A("A"),
    B_PLUS("B+"),
    B("B"),
    C_PLUS("C+"),
    C("C"),
    D_PLUS("D+"),
    D("D"),
    F("F"),
    X("X")
}

data class GradeBand(
    val letter: LetterGrade,
    val minScore: Double,
)

data class GradingConfig(
    val caWeight: Double,
    val examWeight: Double,
    val passCutoff: Double,
    val caMaxRaw: Double = 30.0,
    val examMaxRaw: Double = 70.0,
    val maxPercent: Double = 100.0,
    val gradeBands: List<GradeBand>,
) {
    fun letterFor(score: Double?, unknown: Boolean): LetterGrade {
        if (unknown || score == null) {
            return LetterGrade.X
        }
        return gradeBands.firstOrNull { score >= it.minScore }?.letter ?: LetterGrade.F
    }

    companion object {
        val strictDefault = GradingConfig(
            caWeight = 0.30,
            examWeight = 0.70,
            passCutoff = 65.0,
            gradeBands = listOf(
                GradeBand(LetterGrade.A, 85.0),
                GradeBand(LetterGrade.B_PLUS, 80.0),
                GradeBand(LetterGrade.B, 75.0),
                GradeBand(LetterGrade.C_PLUS, 70.0),
                GradeBand(LetterGrade.C, 65.0),
                GradeBand(LetterGrade.D_PLUS, 60.0),
                GradeBand(LetterGrade.D, 55.0),
                GradeBand(LetterGrade.F, 0.0),
            ),
        )
    }
}

