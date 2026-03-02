package com.joelf.studentgradecalc.domain.model

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
