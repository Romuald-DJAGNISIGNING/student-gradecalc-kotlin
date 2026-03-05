package com.joelf.studentgradecalc.console.core

import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

class WorkbookExportService {
    data class ExportResult(
        val path: Path,
        val sizeBytes: Long,
    )

    fun export(report: ProcessingReport, destination: Path): ExportResult {
        destination.parent?.createDirectories()

        XSSFWorkbook().use { wb ->
            val grades = wb.createSheet("Grades")
            val summary = wb.createSheet("Summary")
            val issues = wb.createSheet("Issues")
            val chart = wb.createSheet("ChartData")

            fillGrades(wb, grades, report)
            fillSummary(wb, summary, report)
            fillIssues(wb, issues, report)
            fillChart(chart, report)

            Files.newOutputStream(destination).use(wb::write)
        }

        return ExportResult(destination, Files.size(destination))
    }

    private fun fillGrades(workbook: XSSFWorkbook, sheet: XSSFSheet, report: ProcessingReport) {
        val header = listOf("Row", "Name", "Matricule", "Final Score", "Letter", "Pass", "Status", "Source", "Reasons")
        appendRow(sheet, 0, header)
        applyHeaderStyle(workbook, sheet, 0, header.size)

        report.results.forEachIndexed { index, result ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(result.rowIndex.toDouble())
            row.createCell(1).setCellValue(result.name.orEmpty())
            row.createCell(2).setCellValue(result.matricule.orEmpty())
            row.createCell(3).setCellValue(result.finalScore ?: Double.NaN)
            row.createCell(4).setCellValue(result.letter.label)
            row.createCell(5).setCellValue(if (result.pass) "YES" else "NO")
            row.createCell(6).setCellValue(result.status.name)
            row.createCell(7).setCellValue(result.source)
            row.createCell(8).setCellValue(result.reasons.joinToString(" | "))

            if (result.finalScore == null) {
                row.getCell(3).setBlank()
            }

            val style = styleForGrade(workbook, result.letter.label)
            (0 until header.size).forEach { col -> row.getCell(col).cellStyle = style }
        }

        autoSize(sheet, header.size)
    }

    private fun fillSummary(workbook: XSSFWorkbook, sheet: XSSFSheet, report: ProcessingReport) {
        val header = listOf("Metric", "Value")
        appendRow(sheet, 0, header)
        applyHeaderStyle(workbook, sheet, 0, header.size)

        val s = report.summary
        val metrics = listOf(
            "Total Rows" to s.totalRows.toDouble(),
            "Graded Rows" to s.gradedRows.toDouble(),
            "Unknown Rows" to s.unknownRows.toDouble(),
            "Average Score" to s.average,
            "Median Score" to s.median,
            "Pass Rate %" to s.passRate,
        )

        metrics.forEachIndexed { index, (label, value) ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(label)
            row.createCell(1).setCellValue(value)
        }

        val start = metrics.size + 3
        appendRow(sheet, start - 1, listOf("Grade", "Count"))
        applyHeaderStyle(workbook, sheet, start - 1, 2)

        report.summary.gradeCounts.entries.forEachIndexed { index, (grade, count) ->
            val row = sheet.createRow(start + index)
            row.createCell(0).setCellValue(grade.label)
            row.createCell(1).setCellValue(count.toDouble())
        }

        autoSize(sheet, 2)
    }

    private fun fillIssues(workbook: XSSFWorkbook, sheet: XSSFSheet, report: ProcessingReport) {
        val header = listOf("Row", "Severity", "Code", "Message")
        appendRow(sheet, 0, header)
        applyHeaderStyle(workbook, sheet, 0, header.size)

        report.issues.forEachIndexed { index, issue ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(issue.rowIndex.toDouble())
            row.createCell(1).setCellValue(issue.severity.name)
            row.createCell(2).setCellValue(issue.code)
            row.createCell(3).setCellValue(issue.message)

            val style = styleForSeverity(workbook, issue.severity)
            (0 until header.size).forEach { col -> row.getCell(col).cellStyle = style }
        }

        autoSize(sheet, header.size)
    }

    private fun fillChart(sheet: XSSFSheet, report: ProcessingReport) {
        appendRow(sheet, 0, listOf("Grade", "Count"))
        report.summary.gradeCounts.entries.forEachIndexed { index, (grade, count) ->
            appendRow(sheet, index + 1, listOf(grade.label, count.toString()))
        }
        autoSize(sheet, 2)
    }

    private fun appendRow(sheet: XSSFSheet, rowIndex: Int, values: List<String>) {
        val row = sheet.createRow(rowIndex)
        values.forEachIndexed { index, value -> row.createCell(index).setCellValue(value) }
    }

    private fun applyHeaderStyle(workbook: XSSFWorkbook, sheet: XSSFSheet, rowIndex: Int, columns: Int) {
        val style = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            })
            alignment = HorizontalAlignment.CENTER
        }

        val row = sheet.getRow(rowIndex)
        (0 until columns).forEach { col -> row.getCell(col).cellStyle = style }
    }

    private fun styleForGrade(workbook: XSSFWorkbook, grade: String): XSSFCellStyle {
        val fill = when (grade) {
            "A", "B+", "B" -> IndexedColors.LIGHT_GREEN.index
            "C+", "C" -> IndexedColors.LIGHT_YELLOW.index
            "D+", "D" -> IndexedColors.LIGHT_ORANGE.index
            "F", "X" -> IndexedColors.ROSE.index
            else -> IndexedColors.WHITE.index
        }

        return workbook.createCellStyle().apply {
            fillForegroundColor = fill
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
    }

    private fun styleForSeverity(workbook: XSSFWorkbook, severity: IssueSeverity): XSSFCellStyle {
        val fill = when (severity) {
            IssueSeverity.ERROR -> IndexedColors.ROSE.index
            IssueSeverity.WARNING -> IndexedColors.LIGHT_YELLOW.index
            IssueSeverity.INFO -> IndexedColors.PALE_BLUE.index
        }

        return workbook.createCellStyle().apply {
            fillForegroundColor = fill
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
    }

    private fun autoSize(sheet: XSSFSheet, columns: Int) {
        (0 until columns).forEach(sheet::autoSizeColumn)
    }
}
