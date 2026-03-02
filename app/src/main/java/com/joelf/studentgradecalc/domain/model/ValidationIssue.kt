package com.joelf.studentgradecalc.domain.model

enum class IssueSeverity {
    INFO,
    WARNING,
    ERROR,
}

data class ValidationIssue(
    val rowIndex: Int,
    val severity: IssueSeverity,
    val code: String,
    val message: String,
)
