package com.romualdsigning.studentgradecalc.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.romualdsigning.studentgradecalc.domain.model.LetterGrade
import com.romualdsigning.studentgradecalc.domain.model.StudentInputRow
import org.junit.Test

class GradingEngineTest {
    private val engine = GradingEngine()

    @Test
    fun `uses weighted score for 0-100 CA and exam`() {
        val report = engine.batchGrade(
            listOf(
                StudentInputRow(
                    rowIndex = 2,
                    name = "Alice",
                    matricule = "MAT001",
                    ca = "80",
                    exam = "90",
                    total = null,
                    rawValues = emptyMap(),
                )
            )
        )

        val result = report.results.single()
        assertThat(result.finalScore).isEqualTo(87.0)
        assertThat(result.letter).isEqualTo(LetterGrade.A)
    }

    @Test
    fun `uses raw CA plus exam for 30-70 scale`() {
        val report = engine.batchGrade(
            listOf(
                StudentInputRow(
                    rowIndex = 3,
                    name = "Bob",
                    matricule = "MAT002",
                    ca = "28",
                    exam = "61",
                    total = null,
                    rawValues = emptyMap(),
                )
            )
        )

        val result = report.results.single()
        assertThat(result.finalScore).isEqualTo(89.0)
        assertThat(result.letter).isEqualTo(LetterGrade.A)
    }

    @Test
    fun `falls back to total for incomplete CA exam pair`() {
        val report = engine.batchGrade(
            listOf(
                StudentInputRow(
                    rowIndex = 4,
                    name = "Chris",
                    matricule = "MAT003",
                    ca = "20",
                    exam = null,
                    total = "66",
                    rawValues = emptyMap(),
                )
            )
        )

        val result = report.results.single()
        assertThat(result.finalScore).isEqualTo(66.0)
        assertThat(result.letter).isEqualTo(LetterGrade.C)
        assertThat(report.issues.any { it.code == "FALLBACK_TOTAL" }).isTrue()
    }

    @Test
    fun `returns X when mark set is incoherent`() {
        val report = engine.batchGrade(
            listOf(
                StudentInputRow(
                    rowIndex = 5,
                    name = "Dina",
                    matricule = "MAT004",
                    ca = "abc",
                    exam = "def",
                    total = null,
                    rawValues = emptyMap(),
                )
            )
        )

        val result = report.results.single()
        assertThat(result.letter).isEqualTo(LetterGrade.X)
        assertThat(result.finalScore).isNull()
    }

    @Test
    fun `keeps latest duplicate row`() {
        val report = engine.batchGrade(
            listOf(
                StudentInputRow(
                    rowIndex = 6,
                    name = "Eva",
                    matricule = "MAT005",
                    ca = "20",
                    exam = "30",
                    total = null,
                    rawValues = emptyMap(),
                ),
                StudentInputRow(
                    rowIndex = 7,
                    name = "Eva New",
                    matricule = "MAT005",
                    ca = "25",
                    exam = "35",
                    total = null,
                    rawValues = emptyMap(),
                ),
            )
        )

        assertThat(report.results).hasSize(1)
        assertThat(report.results.single().rowIndex).isEqualTo(7)
        assertThat(report.issues.any { it.code == "DUPLICATE" }).isTrue()
    }
}

