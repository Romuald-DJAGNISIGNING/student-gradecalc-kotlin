package com.romualdsigning.studentgradecalc.console

import com.romualdsigning.studentgradecalc.console.core.ChartDataBuilder
import com.romualdsigning.studentgradecalc.console.core.FileImportService
import com.romualdsigning.studentgradecalc.console.core.GradingEngine
import com.romualdsigning.studentgradecalc.console.core.WorkbookExportService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun main(args: Array<String>) {
    val options = CliOptions.parse(args.toList())
    if (options.showHelp || options.input == null || options.output == null) {
        printUsage()
        return
    }

    val importer = FileImportService()
    val engine = GradingEngine()
    val exporter = WorkbookExportService()
    val chartBuilder = ChartDataBuilder()

    runCatching {
        val inputPath = resolveInput(options.input)
        val rows = importer.parse(inputPath)
        val report = engine.batchGrade(rows)
        val exportResult = exporter.export(report, options.output)
        val chart = chartBuilder.buildGradeDistribution(report)

        println("Processed ${report.summary.totalRows} rows")
        println("Graded: ${report.summary.gradedRows} | Unknown: ${report.summary.unknownRows}")
        println("Average: ${report.summary.average} | Median: ${report.summary.median} | Pass rate: ${report.summary.passRate}%")
        println("Issues: ${report.issues.size}")
        println("Distribution: ${chart.points.joinToString { "${it.label}:${it.count}" }}")
        println("Exported workbook: ${exportResult.path.absolutePathString()} (${exportResult.sizeBytes} bytes)")
    }.onFailure { error ->
        System.err.println("CLI error: ${error.message}")
    }
}

private fun resolveInput(path: Path): Path {
    if (Files.exists(path)) return path
    val fromRoot = Path.of("..").resolve(path).normalize()
    return if (Files.exists(fromRoot)) fromRoot else path
}

private data class CliOptions(
    val input: Path?,
    val output: Path?,
    val showHelp: Boolean,
) {
    companion object {
        fun parse(args: List<String>): CliOptions {
            var input: Path? = null
            var output: Path? = null
            var help = false

            var index = 0
            while (index < args.size) {
                when (args[index]) {
                    "--help", "-h" -> help = true
                    "--input", "-i" -> if (index + 1 < args.size) input = Path.of(args[++index])
                    "--output", "-o" -> if (index + 1 < args.size) output = Path.of(args[++index])
                }
                index++
            }

            return CliOptions(input = input, output = output, showHelp = help)
        }
    }
}

private fun printUsage() {
    println("Student Grade Calculator - Kotlin Console")
    println("Usage:")
    println("  ./gradlew :console:run --args=\"--input samples/parity/input_students.csv --output build/exports/grades.xlsx\"")
    println("Options:")
    println("  --input, -i   Source .csv/.xlsx dataset")
    println("  --output, -o  Destination .xlsx workbook")
    println("  --help, -h    Show this help")
}

