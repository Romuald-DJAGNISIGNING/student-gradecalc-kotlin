package com.romualdsigning.studentgradecalc.domain.usecase

import com.romualdsigning.studentgradecalc.data.io.NumericParser
import com.romualdsigning.studentgradecalc.domain.model.GradeResult
import com.romualdsigning.studentgradecalc.domain.model.GradeStatus
import com.romualdsigning.studentgradecalc.domain.model.GradingConfig
import com.romualdsigning.studentgradecalc.domain.model.IssueSeverity
import com.romualdsigning.studentgradecalc.domain.model.LetterGrade
import com.romualdsigning.studentgradecalc.domain.model.NormalizedStudent
import com.romualdsigning.studentgradecalc.domain.model.ProcessingReport
import com.romualdsigning.studentgradecalc.domain.model.ProcessingSummary
import com.romualdsigning.studentgradecalc.domain.model.StudentInputRow
import com.romualdsigning.studentgradecalc.domain.model.ValidationIssue
import kotlin.math.round

class GradingEngine(
    private val config: GradingConfig = GradingConfig.strictDefault,
) {
    fun batchGrade(rows: List<StudentInputRow>): ProcessingReport {
        val issues = mutableListOf<ValidationIssue>()
        val deduped = dedupeRows(rows, issues)

        val results = deduped
            .map { evaluate(it, issues) }
            .sortedBy { it.rowIndex }

        val summary = buildSummary(results)

        return ProcessingReport(
            results = results,
            issues = issues.sortedBy { it.rowIndex },
            summary = summary,
        )
    }

    private fun dedupeRows(
        rows: List<StudentInputRow>,
        issues: MutableList<ValidationIssue>,
    ): List<StudentInputRow> {
        val byKey = linkedMapOf<String, StudentInputRow>()

        // I keep the newest row because teachers often correct marks at the bottom.
        rows.forEach { row ->
            val key = dedupeKey(row)
            if (key == null) {
                byKey["row-${row.rowIndex}"] = row
                return@forEach
            }

            val existing = byKey[key]
            if (existing != null) {
                issues += ValidationIssue(
                    rowIndex = row.rowIndex,
                    severity = IssueSeverity.WARNING,
                    code = "DUPLICATE",
                    message = "Duplicate identifier found. Keeping row ${row.rowIndex}, replacing row ${existing.rowIndex}.",
                )
            }
            byKey[key] = row
        }

        return byKey.values.toList()
    }

    private fun dedupeKey(row: StudentInputRow): String? {
        val matricule = row.matricule?.trim().orEmpty()
        if (matricule.isNotEmpty()) {
            return "m:${matricule.lowercase()}"
        }

        val name = row.name?.trim().orEmpty()
        if (name.isNotEmpty()) {
            return "n:${name.lowercase()}"
        }

        return null
    }

    private fun evaluate(
        row: StudentInputRow,
        issues: MutableList<ValidationIssue>,
    ): GradeResult {
        val reasons = mutableListOf<String>()

        val name = row.name?.trim().takeUnless { it.isNullOrEmpty() }
        val matricule = row.matricule?.trim().takeUnless { it.isNullOrEmpty() }
        val hasIdentity = !name.isNullOrEmpty() || !matricule.isNullOrEmpty()

        val caPresent = !row.ca.isNullOrBlank()
        val examPresent = !row.exam.isNullOrBlank()
        val totalPresent = !row.total.isNullOrBlank()

        val ca = NumericParser.parseFlexible(row.ca)
        val exam = NumericParser.parseFlexible(row.exam)
        val total = NumericParser.parseFlexible(row.total)

        val normalized = NormalizedStudent(
            rowIndex = row.rowIndex,
            name = name,
            matricule = matricule,
            ca = ca,
            exam = exam,
            total = total,
            caPresent = caPresent,
            examPresent = examPresent,
            totalPresent = totalPresent,
        )

        if (!hasIdentity) {
            reasons += "Missing identifier"
            issues += ValidationIssue(
                rowIndex = row.rowIndex,
                severity = IssueSeverity.ERROR,
                code = "MISSING_ID",
                message = "Both name and matricule are missing.",
            )
        }

        if (caPresent && ca == null) reasons += "CA mark is not numeric"
        if (examPresent && exam == null) reasons += "Exam mark is not numeric"
        if (totalPresent && total == null) reasons += "Total mark is not numeric"

        val scoreDecision = resolveFinalScore(normalized, reasons)
        val rounded = scoreDecision.score?.round2()
        val unknown = !hasIdentity || rounded == null

        if (unknown) {
            issues += ValidationIssue(
                rowIndex = row.rowIndex,
                severity = IssueSeverity.ERROR,
                code = "UNKNOWN_GRADE",
                message = if (reasons.isEmpty()) {
                    "Unable to compute final score from provided marks."
                } else {
                    reasons.joinToString("; ")
                },
            )
        } else if (scoreDecision.usedFallback) {
            issues += ValidationIssue(
                rowIndex = row.rowIndex,
                severity = IssueSeverity.WARNING,
                code = "FALLBACK_TOTAL",
                message = "CA/Exam could not be used. Total fallback applied.",
            )
        }

        val letter = config.letterFor(rounded, unknown)
        val pass = rounded != null && rounded >= config.passCutoff

        return GradeResult(
            rowIndex = row.rowIndex,
            name = name,
            matricule = matricule,
            finalScore = rounded,
            letter = letter,
            pass = pass,
            status = if (unknown) GradeStatus.UNKNOWN else GradeStatus.GRADED,
            reasons = reasons,
            source = scoreDecision.source,
        )
    }

    private fun resolveFinalScore(
        row: NormalizedStudent,
        reasons: MutableList<String>,
    ): ScoreDecision {
        if (row.caPresent && row.examPresent && row.ca != null && row.exam != null) {
            val ca = row.ca
            val exam = row.exam

            if (NumericParser.inRange(ca, 0.0, config.caMaxRaw) &&
                NumericParser.inRange(exam, 0.0, config.examMaxRaw)
            ) {
                val rawScore = ca + exam
                if (isScoreValid(rawScore)) {
                    return ScoreDecision(score = rawScore, source = "CA+Exam (raw)", usedFallback = false)
                }
                reasons += "CA/Exam raw score is outside 0..100"
            }

            if (NumericParser.inRange(ca, 0.0, config.maxPercent) &&
                NumericParser.inRange(exam, 0.0, config.maxPercent)
            ) {
                val weighted = (ca * config.caWeight) + (exam * config.examWeight)
                if (isScoreValid(weighted)) {
                    return ScoreDecision(score = weighted, source = "CA+Exam (weighted)", usedFallback = false)
                }
                reasons += "Weighted CA/Exam score is outside 0..100"
            }

            reasons += "CA/Exam values are out of accepted ranges"
        } else if (row.caPresent || row.examPresent) {
            reasons += "CA/Exam pair is incomplete"
        }

        if (row.totalPresent && row.total != null) {
            val total = row.total
            if (isScoreValid(total)) {
                return ScoreDecision(score = total, source = "Total (fallback)", usedFallback = true)
            }
            reasons += "Total is out of 0..100 range"
        }

        if (row.totalPresent && row.total == null) {
            reasons += "Total is present but invalid"
        }

        return ScoreDecision(score = null, source = "Unavailable", usedFallback = false)
    }

    private fun buildSummary(results: List<GradeResult>): ProcessingSummary {
        val gradeCounts = LetterGrade.entries.associateWith { 0 }.toMutableMap()
        results.forEach { result ->
            gradeCounts[result.letter] = (gradeCounts[result.letter] ?: 0) + 1
        }

        val gradedScores = results
            .filter { it.status == GradeStatus.GRADED }
            .mapNotNull { it.finalScore }
            .sorted()

        val gradedRows = gradedScores.size
        val unknownRows = results.size - gradedRows
        val average = if (gradedRows == 0) 0.0 else gradedScores.average().round2()
        val median = median(gradedScores).round2()
        val passCount = results.count { it.status == GradeStatus.GRADED && it.pass }
        val passRate = if (gradedRows == 0) 0.0 else ((passCount.toDouble() / gradedRows) * 100).round2()

        return ProcessingSummary(
            totalRows = results.size,
            gradedRows = gradedRows,
            unknownRows = unknownRows,
            average = average,
            median = median,
            passRate = passRate,
            gradeCounts = gradeCounts,
        )
    }

    private fun median(sorted: List<Double>): Double {
        if (sorted.isEmpty()) return 0.0
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2
        }
    }

    private data class ScoreDecision(
        val score: Double?,
        val source: String,
        val usedFallback: Boolean,
    )

    private fun isScoreValid(value: Double): Boolean = NumericParser.inRange(value, 0.0, config.maxPercent)

    private fun Double.round2(): Double = round(this * 100) / 100
}

