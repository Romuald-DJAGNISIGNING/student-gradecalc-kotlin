package com.joelf.studentgradecalc.data.io

object ColumnMappingConfig {
    private val nameAliases = setOf(
        "name", "studentname", "student_name", "fullname", "full_name", "nom"
    )
    private val matriculeAliases = setOf(
        "matricule", "matriculation", "matriculeid", "registration", "studentid", "id"
    )
    private val caAliases = setOf(
        "ca", "continuousassessment", "coursework", "assignment", "test"
    )
    private val examAliases = setOf(
        "exam", "exammark", "examscore", "finalexam", "final_exam"
    )
    private val totalAliases = setOf(
        "total", "totalmark", "totalscore", "score", "mark", "gradepoint"
    )

    fun normalize(header: String): String {
        return header.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    fun resolveCanonical(header: String): String? {
        val normalized = normalize(header)
        return when {
            normalized in nameAliases -> "name"
            normalized in matriculeAliases -> "matricule"
            normalized in caAliases -> "ca"
            normalized in examAliases -> "exam"
            normalized in totalAliases -> "total"
            else -> null
        }
    }
}
