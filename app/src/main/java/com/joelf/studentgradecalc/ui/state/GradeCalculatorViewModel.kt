package com.joelf.studentgradecalc.ui.state

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelf.studentgradecalc.data.io.FileImportService
import com.joelf.studentgradecalc.data.io.WorkbookExportService
import com.joelf.studentgradecalc.domain.usecase.ChartDataBuilder
import com.joelf.studentgradecalc.domain.usecase.GradingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GradeCalculatorViewModel : ViewModel() {
    private val importer = FileImportService()
    private val grader = GradingEngine()
    private val chartBuilder = ChartDataBuilder()
    private val exporter = WorkbookExportService()

    private val _uiState = MutableStateFlow(GradeCalculatorUiState())
    val uiState: StateFlow<GradeCalculatorUiState> = _uiState

    fun importFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, exportMessage = null) }
            runCatching {
                val rows = importer.parse(context, uri)
                val report = grader.batchGrade(rows)
                val chart = chartBuilder.buildGradeDistribution(report)
                val name = resolveDisplayName(context, uri)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sourceName = name,
                        report = report,
                        chart = chart,
                        error = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, error = throwable.message ?: "Import failed")
                }
            }
        }
    }

    fun exportToUri(context: Context, uri: Uri) {
        val report = _uiState.value.report ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val result = exporter.export(context, report, uri)
                val kb = (result.sizeBytes ?: 0L) / 1024
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        exportMessage = "Workbook exported (${kb}KB)",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, error = throwable.message ?: "Export failed")
                }
            }
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(error = null, exportMessage = null) }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        ) ?: return uri.lastPathSegment ?: "Imported file"

        return cursor.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) {
                it.getString(index)
            } else {
                uri.lastPathSegment ?: "Imported file"
            }
        }
    }
}
