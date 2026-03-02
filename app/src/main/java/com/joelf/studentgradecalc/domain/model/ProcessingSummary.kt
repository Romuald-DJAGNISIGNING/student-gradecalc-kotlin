package com.joelf.studentgradecalc.domain.model

data class ProcessingSummary(
    val totalRows: Int,
    val gradedRows: Int,
    val unknownRows: Int,
    val average: Double,
    val median: Double,
    val passRate: Double,
    val gradeCounts: Map<LetterGrade, Int>,
)
