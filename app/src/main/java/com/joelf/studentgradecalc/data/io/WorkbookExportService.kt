package com.joelf.studentgradecalc.data.io

import android.content.Context
import android.net.Uri
import com.joelf.studentgradecalc.domain.model.IssueSeverity
import com.joelf.studentgradecalc.domain.model.ProcessingReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class WorkbookExportService {
    suspend fun export(context: Context, report: ProcessingReport, destination: Uri): ExportResult =
        withContext(Dispatchers.IO) {
            val workbook = XSSFWorkbook()
            workbook.use { wb ->
                val grades = wb.createSheet("Grades")
                val summary = wb.createSheet("Summary")
                val issues = wb.createSheet("Issues")
                val chart = wb.createSheet("ChartData")

                fillGrades(wb, grades, report)
                fillSummary(wb, summary, report)
                fillIssues(wb, issues, report)
                fillChart(chart, report)

                context.contentResolver.openOutputStream(destination)?.use { output ->
                    wb.write(output)
                } ?: error("Could not open output stream.")
            }

            val size = context.contentResolver.openFileDescriptor(destination, "r")?.use { it.statSize }
            ExportResult(uri = destination, sizeBytes = size)
        }

    private fun fillGrades(workbook: XSSFWorkbook, sheet: XSSFSheet, report: ProcessingReport) {
        val header = listOf("Row", "Name", "Matricule", "Final Score", "Letter", "Pass", "Status", "Source", "Reasons")
        appendRow(sheet, 0, header)
        applyHeaderStyle(workbook, sheet, 0, header.size)

        report.results.forEachIndexed { index, result ->
            val rowIndex = index + 1
            val row = sheet.createRow(rowIndex)
            row.createCell(0).setCellValue(result.rowIndex.toDouble())
            row.createCell(1).setCellValue(result.name.orEmpty())
            row.createCell(2).setCellValue(result.matricule.orEmpty())
            if (result.finalScore != null) {
                row.createCell(3).setCellValue(result.finalScore)
            } else {
                row.createCell(3).setCellValue("")
            }
            row.createCell(4).setCellValue(result.letter.label)
            row.createCell(5).setCellValue(if (result.pass) "YES" else "NO")
            row.createCell(6).setCellValue(result.status.name)
            row.createCell(7).setCellValue(result.source)
            row.createCell(8).setCellValue(result.reasons.joinToString(" | "))

            val style = styleForGrade(workbook, result.letter.label)
            (0 until header.size).forEach { col ->
                row.getCell(col).cellStyle = style
            }
        }

        autoSize(sheet, header.size)
    }

    private fun fillSummary(workbook: XSSFWorkbook, sheet: XSSFSheet, report: ProcessingReport) {
        val header = listOf("Metric", "Value")
        appendRow(sheet, 0, header)
        applyHeaderStyle(workbook, sheet, 0, header.size)

        val summary = report.summary
        val metrics = listOf(
            "Total Rows" to summary.totalRows.toDouble(),
            "Graded Rows" to summary.gradedRows.toDouble(),
            "Unknown Rows" to summary.unknownRows.toDouble(),
            "Average Score" to summary.average,
            "Median Score" to summary.median,
            "Pass Rate %" to summary.passRate,
        )

        metrics.forEachIndexed { idx, (label, value) ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(label)
            row.createCell(1).setCellValue(value)
        }

        val start = metrics.size + 3
        appendRow(sheet, start - 1, listOf("Grade", "Count"))
        applyHeaderStyle(workbook, sheet, start - 1, 2)

        report.summary.gradeCounts.entries.forEachIndexed { idx, (grade, count) ->
            val row = sheet.createRow(start + idx)
            row.createCell(0).setCellValue(grade.label)
            row.createCell(1).setCellValue(count.toDouble())
        }

        autoSize(sheet, 2)
    }

    private fun fillIssues(workbook: XSSFWorkbook, sheet: XSSFSheet, report: ProcessingReport) {
        val header = listOf("Row", "Severity", "Code", "Message")
        appendRow(sheet, 0, header)
        applyHeaderStyle(workbook, sheet, 0, header.size)

        report.issues.forEachIndexed { idx, issue ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(issue.rowIndex.toDouble())
            row.createCell(1).setCellValue(issue.severity.name)
            row.createCell(2).setCellValue(issue.code)
            row.createCell(3).setCellValue(issue.message)

            val style = styleForSeverity(workbook, issue.severity)
            (0 until header.size).forEach { col ->
                row.getCell(col).cellStyle = style
            }
        }

        autoSize(sheet, header.size)
    }

    private fun fillChart(sheet: XSSFSheet, report: ProcessingReport) {
        appendRow(sheet, 0, listOf("Grade", "Count"))
        report.summary.gradeCounts.entries.forEachIndexed { idx, (grade, count) ->
            appendRow(sheet, idx + 1, listOf(grade.label, count.toString()))
        }
        autoSize(sheet, 2)
    }

    private fun appendRow(sheet: XSSFSheet, rowIndex: Int, values: List<String>) {
        val row = sheet.createRow(rowIndex)
        values.forEachIndexed { index, value -> row.createCell(index).setCellValue(value) }
    }

    private fun applyHeaderStyle(workbook: XSSFWorkbook, sheet: XSSFSheet, rowIndex: Int, columns: Int) {
        val row = sheet.getRow(rowIndex)
        val style = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            })
            alignment = HorizontalAlignment.CENTER
        }

        (0 until columns).forEach { col -> row.getCell(col).cellStyle = style }
    }

    private fun styleForGrade(workbook: XSSFWorkbook, grade: String): XSSFCellStyle {
        val color = when (grade) {
            "A", "B+", "B" -> IndexedColors.LIGHT_GREEN
            "C+", "C" -> IndexedColors.LEMON_CHIFFON
            "D+", "D" -> IndexedColors.LIGHT_YELLOW
            else -> IndexedColors.ROSE
        }

        return workbook.createCellStyle().apply {
            fillForegroundColor = color.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
    }

    private fun styleForSeverity(workbook: XSSFWorkbook, severity: IssueSeverity): XSSFCellStyle {
        val color = when (severity) {
            IssueSeverity.ERROR -> IndexedColors.ROSE
            IssueSeverity.WARNING -> IndexedColors.LIGHT_YELLOW
            IssueSeverity.INFO -> IndexedColors.PALE_BLUE
        }

        return workbook.createCellStyle().apply {
            fillForegroundColor = color.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
    }

    private fun autoSize(sheet: XSSFSheet, columns: Int) {
        (0 until columns).forEach(sheet::autoSizeColumn)
    }
}

data class ExportResult(
    val uri: Uri,
    val sizeBytes: Long?,
)

