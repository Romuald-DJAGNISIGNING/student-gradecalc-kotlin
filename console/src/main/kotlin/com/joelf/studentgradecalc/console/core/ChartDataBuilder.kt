package com.joelf.studentgradecalc.console.core

class ChartDataBuilder {
    fun buildGradeDistribution(report: ProcessingReport): ChartDataset {
        val points = report.summary.gradeCounts
            .filterValues { it > 0 }
            .map { (grade, count) -> ChartPoint(label = grade.label, count = count) }

        return ChartDataset(points = points)
    }
}
