package com.romualdsigning.studentgradecalc.domain.model

enum class GradeStatus {
    GRADED,
    UNKNOWN,
}

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

