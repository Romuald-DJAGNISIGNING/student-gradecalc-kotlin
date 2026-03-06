package com.romualdsigning.studentgradecalc.domain.usecase

import com.romualdsigning.studentgradecalc.domain.model.ChartDataset
import com.romualdsigning.studentgradecalc.domain.model.ChartPoint
import com.romualdsigning.studentgradecalc.domain.model.ProcessingReport

class ChartDataBuilder {
    fun buildGradeDistribution(report: ProcessingReport): ChartDataset {
        val points = report.summary.gradeCounts
            .filterValues { it > 0 }
            .map { (grade, count) -> ChartPoint(label = grade.label, count = count) }

        return ChartDataset(points = points)
    }
}

