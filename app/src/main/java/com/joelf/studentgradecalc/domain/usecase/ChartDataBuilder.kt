package com.joelf.studentgradecalc.domain.usecase

import com.joelf.studentgradecalc.domain.model.ChartDataset
import com.joelf.studentgradecalc.domain.model.ChartPoint
import com.joelf.studentgradecalc.domain.model.ProcessingReport

class ChartDataBuilder {
    fun buildGradeDistribution(report: ProcessingReport): ChartDataset {
        val points = report.summary.gradeCounts
            .filterValues { it > 0 }
            .map { (grade, count) -> ChartPoint(label = grade.label, count = count) }

        return ChartDataset(points = points)
    }
}
