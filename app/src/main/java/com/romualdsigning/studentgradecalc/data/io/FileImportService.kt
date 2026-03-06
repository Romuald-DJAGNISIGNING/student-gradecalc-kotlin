package com.romualdsigning.studentgradecalc.data.io

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.romualdsigning.studentgradecalc.domain.model.StudentInputRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class FileImportService {
    suspend fun parse(context: Context, uri: Uri): List<StudentInputRow> = withContext(Dispatchers.IO) {
        val extension = detectExtension(context, uri)
        when (extension) {
            "csv" -> parseCsv(context, uri)
            "xlsx" -> parseXlsx(context, uri)
            else -> throw IllegalArgumentException("Unsupported file. Only CSV/XLSX are accepted.")
        }
    }

    private fun parseCsv(context: Context, uri: Uri): List<StudentInputRow> {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return emptyList()

        val rows = parseCsvRows(content)
        if (rows.isEmpty()) return emptyList()

        val headers = rows.first()
        return rows.drop(1).mapIndexed { index, row ->
            toInputRow(rowIndex = index + 2, headers = headers, values = row)
        }
    }

    private fun parseXlsx(context: Context, uri: Uri): List<StudentInputRow> {
        val formatter = DataFormatter()

        context.contentResolver.openInputStream(uri)?.use { input ->
            XSSFWorkbook(input).use { workbook ->
                val sheet = workbook.getSheetAt(0) ?: return emptyList()
                val headerRow = sheet.getRow(0) ?: return emptyList()
                val headers = (0 until headerRow.lastCellNum)
                    .map { cellIndex -> formatter.formatCellValue(headerRow.getCell(cellIndex.toInt())) }

                val output = mutableListOf<StudentInputRow>()
                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    val values = (0 until headers.size)
                        .map { cellIndex -> formatter.formatCellValue(row.getCell(cellIndex)) }
                    output += toInputRow(rowIndex = rowIndex + 1, headers = headers, values = values)
                }
                return output
            }
        }

        return emptyList()
    }

    private fun toInputRow(
        rowIndex: Int,
        headers: List<String>,
        values: List<String>,
    ): StudentInputRow {
        val raw = linkedMapOf<String, String?>()
        headers.forEachIndexed { index, header ->
            val value = values.getOrNull(index)?.trim().takeUnless { it.isNullOrEmpty() }
            raw[header] = value
        }

        fun find(canonical: String): String? {
            return raw.entries.firstOrNull { (header, _) ->
                ColumnMappingConfig.resolveCanonical(header) == canonical
            }?.value
        }

        return StudentInputRow(
            rowIndex = rowIndex,
            name = find("name"),
            matricule = find("matricule"),
            ca = find("ca"),
            exam = find("exam"),
            total = find("total"),
            rawValues = raw,
        )
    }

    private fun detectExtension(context: Context, uri: Uri): String {
        val displayName = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }

        val fileName = displayName ?: uri.lastPathSegment.orEmpty()
        return fileName.substringAfterLast('.', "").lowercase()
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
                    if (ch == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                        i++
                    }
                    currentRow += field.toString()
                    field.clear()
                    if (currentRow.any { it.isNotBlank() }) {
                        rows += currentRow.toList()
                    }
                    currentRow.clear()
                }

                else -> field.append(ch)
            }
            i++
        }

        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow += field.toString()
            if (currentRow.any { it.isNotBlank() }) {
                rows += currentRow.toList()
            }
        }

        return rows
    }
}

