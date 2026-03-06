package com.romualdsigning.studentgradecalc.console.core

enum class LetterGrade(val label: String) {
    A("A"),
    B_PLUS("B+"),
    B("B"),
    C_PLUS("C+"),
    C("C"),
    D_PLUS("D+"),
    D("D"),
    F("F"),
    X("X"),
}

enum class GradeStatus {
    GRADED,
    UNKNOWN,
}

enum class IssueSeverity {
    INFO,
    WARNING,
    ERROR,
}

data class GradeBand(
    val letter: LetterGrade,
    val minScore: Double,
)

data class GradingConfig(
    val caWeight: Double,
    val examWeight: Double,
    val passCutoff: Double,
    val caMaxRaw: Double,
    val examMaxRaw: Double,
    val maxPercent: Double,
    val gradeBands: List<GradeBand>,
) {
    fun letterFor(score: Double?, unknown: Boolean): LetterGrade {
        if (unknown || score == null) return LetterGrade.X
        return gradeBands.firstOrNull { score >= it.minScore }?.letter ?: LetterGrade.F
    }

    companion object {
        val strictDefault = GradingConfig(
            caWeight = 0.30,
            examWeight = 0.70,
            passCutoff = 65.0,
            caMaxRaw = 30.0,
            examMaxRaw = 70.0,
            maxPercent = 100.0,
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

data class StudentInputRow(
    val rowIndex: Int,
    val name: String?,
    val matricule: String?,
    val ca: String?,
    val exam: String?,
    val total: String?,
    val rawValues: Map<String, String?>,
)

data class NormalizedStudent(
    val rowIndex: Int,
    val name: String?,
    val matricule: String?,
    val ca: Double?,
    val exam: Double?,
    val total: Double?,
    val caPresent: Boolean,
    val examPresent: Boolean,
    val totalPresent: Boolean,
)

data class GradeResult(
    val rowIndex: Int,
    val name: String?,
    val matricule: String?,
    val finalScore: Double?,
    val letter: LetterGrade,
    val pass: Boolean,
    val status: GradeStatus,
    val reasons: List<String>,
    val source: String,
)

data class ValidationIssue(
    val rowIndex: Int,
    val severity: IssueSeverity,
    val code: String,
    val message: String,
)

data class ProcessingSummary(
    val totalRows: Int,
    val gradedRows: Int,
    val unknownRows: Int,
    val average: Double,
    val median: Double,
    val passRate: Double,
    val gradeCounts: Map<LetterGrade, Int>,
)

data class ProcessingReport(
    val results: List<GradeResult>,
    val issues: List<ValidationIssue>,
    val summary: ProcessingSummary,
)

data class ChartPoint(
    val label: String,
    val count: Int,
)

data class ChartDataset(
    val points: List<ChartPoint>,
)

