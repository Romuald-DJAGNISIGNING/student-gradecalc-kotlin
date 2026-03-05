package com.joelf.studentgradecalc.console.core

import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

class FileImportService {
    fun parse(path: Path): List<StudentInputRow> = when (path.extension.lowercase()) {
        "csv" -> parseCsv(path)
        "xlsx" -> parseXlsx(path)
        else -> error("Unsupported file '${path.name}'. Only CSV/XLSX are accepted.")
    }

    private fun parseCsv(path: Path): List<StudentInputRow> {
        val rows = parseCsvRows(Files.readString(path))
        if (rows.isEmpty()) return emptyList()

        val headers = rows.first()
        val indexes = canonicalIndices(headers)

        return rows.drop(1).mapIndexed { index, values ->
            toInputRow(rowIndex = index + 2, headers = headers, values = values, canonicalIndices = indexes)
        }
    }

    private fun parseXlsx(path: Path): List<StudentInputRow> {
        val formatter = DataFormatter()
        Files.newInputStream(path).use { input ->
            XSSFWorkbook(input).use { workbook ->
                val sheet = workbook.getSheetAt(0) ?: return emptyList()
                val headerRow = sheet.getRow(0) ?: return emptyList()

                val headers = (0 until headerRow.lastCellNum)
                    .map { col -> formatter.formatCellValue(headerRow.getCell(col.toInt())) }
                val indexes = canonicalIndices(headers)

                return (1..sheet.lastRowNum)
                    .mapNotNull { rowIndex ->
                        val row = sheet.getRow(rowIndex) ?: return@mapNotNull null
                        val values = (0 until headers.size)
                            .map { col -> formatter.formatCellValue(row.getCell(col)) }
                        toInputRow(rowIndex + 1, headers, values, indexes)
                    }
            }
        }
    }

    private fun canonicalIndices(headers: List<String>): Map<String, Int> {
        return headers.mapIndexedNotNull { index, header ->
            ColumnMappingConfig.resolveCanonical(header)?.let { canonical -> canonical to index }
        }.toMap()
    }

    private fun toInputRow(
        rowIndex: Int,
        headers: List<String>,
        values: List<String>,
        canonicalIndices: Map<String, Int>,
    ): StudentInputRow {
        val rawValues = headers.mapIndexed { index, header ->
            header to values.getOrNull(index)?.trim().takeUnless { it.isNullOrEmpty() }
        }.toMap(LinkedHashMap())

        fun pick(field: String): String? {
            val index = canonicalIndices[field] ?: return null
            return rawValues[headers.getOrNull(index)]
        }

        return StudentInputRow(
            rowIndex = rowIndex,
            name = pick("name"),
            matricule = pick("matricule"),
            ca = pick("ca"),
            exam = pick("exam"),
            total = pick("total"),
            rawValues = rawValues,
        )
    }

    private fun parseCsvRows(content: String): List<List<String>> {
        val firstLine = content.lineSequence().firstOrNull().orEmpty()
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','

        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val ch = content[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < content.length && content[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                ch == delimiter && !inQuotes -> {
                    currentRow += field.toString()
                    field.clear()
                }

                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && i + 1 < content.length && content[i + 1] == '\n') i++
                    currentRow += field.toString()
                    field.clear()
                    if (currentRow.any { it.isNotBlank() }) rows += currentRow.toList()
                    currentRow.clear()
                }

                else -> field.append(ch)
            }
            i++
        }

        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow += field.toString()
            if (currentRow.any { it.isNotBlank() }) rows += currentRow.toList()
        }

        return rows
    }
}
