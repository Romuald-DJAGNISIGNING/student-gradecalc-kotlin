package com.romualdsigning.studentgradecalc.domain.model

data class StudentInputRow(
    val rowIndex: Int,
    val name: String?,
    val matricule: String?,
    val ca: String?,
    val exam: String?,
    val total: String?,
    val rawValues: Map<String, String?>,
)

