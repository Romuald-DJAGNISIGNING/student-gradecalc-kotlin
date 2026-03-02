package com.joelf.studentgradecalc.ui.state

import com.joelf.studentgradecalc.domain.model.ChartDataset
import com.joelf.studentgradecalc.domain.model.ProcessingReport

data class GradeCalculatorUiState(
    val isLoading: Boolean = false,
    val sourceName: String = "No file imported",
    val report: ProcessingReport? = null,
    val chart: ChartDataset = ChartDataset(points = emptyList()),
    val error: String? = null,
    val exportMessage: String? = null,
)
