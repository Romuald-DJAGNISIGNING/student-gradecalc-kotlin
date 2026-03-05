package com.joelf.studentgradecalc.console.core

object ColumnMappingConfig {
    private val nameAliases = setOf(
        "name", "studentname", "student_name", "fullname", "full_name", "nom",
    )
    private val matriculeAliases = setOf(
        "matricule", "matriculation", "matriculeid", "registration", "studentid", "id",
    )
    private val caAliases = setOf(
        "ca", "continuousassessment", "coursework", "assignment", "test",
    )
    private val examAliases = setOf(
        "exam", "exammark", "examscore", "finalexam", "final_exam",
    )
    private val totalAliases = setOf(
        "total", "totalmark", "totalscore", "score", "mark", "gradepoint",
    )

    private fun normalize(header: String): String =
        header.lowercase().replace(Regex("[^a-z0-9]"), "")

    fun resolveCanonical(header: String): String? {
        return when (normalize(header)) {
            in nameAliases -> "name"
            in matriculeAliases -> "matricule"
            in caAliases -> "ca"
            in examAliases -> "exam"
            in totalAliases -> "total"
            else -> null
        }
    }
}
