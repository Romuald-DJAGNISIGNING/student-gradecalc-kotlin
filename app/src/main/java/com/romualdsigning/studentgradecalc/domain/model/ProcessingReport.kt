package com.romualdsigning.studentgradecalc.domain.model

data class ProcessingReport(
    val results: List<GradeResult>,
    val issues: List<ValidationIssue>,
    val summary: ProcessingSummary,
)

