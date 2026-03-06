package com.romualdsigning.studentgradecalc.ui.state

import com.romualdsigning.studentgradecalc.domain.model.ChartDataset
import com.romualdsigning.studentgradecalc.domain.model.ProcessingReport

data class GradeCalculatorUiState(
    val isLoading: Boolean = false,
    val sourceName: String = "No file imported",
    val report: ProcessingReport? = null,
    val chart: ChartDataset = ChartDataset(points = emptyList()),
    val error: String? = null,
    val exportMessage: String? = null,
)

