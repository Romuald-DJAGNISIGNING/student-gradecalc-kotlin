package com.romualdsigning.studentgradecalc.domain.model

data class ChartPoint(
    val label: String,
    val count: Int,
)

data class ChartDataset(
    val points: List<ChartPoint>,
)

