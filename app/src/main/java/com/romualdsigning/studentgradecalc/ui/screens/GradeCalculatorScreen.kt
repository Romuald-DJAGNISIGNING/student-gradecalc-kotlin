package com.romualdsigning.studentgradecalc.ui.screens

import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.romualdsigning.studentgradecalc.domain.model.ChartPoint
import com.romualdsigning.studentgradecalc.domain.model.GradeResult
import com.romualdsigning.studentgradecalc.domain.model.IssueSeverity
import com.romualdsigning.studentgradecalc.domain.model.ProcessingSummary
import com.romualdsigning.studentgradecalc.domain.model.ValidationIssue
import com.romualdsigning.studentgradecalc.ui.state.GradeCalculatorViewModel

@Composable
fun GradeCalculatorScreen(viewModel: GradeCalculatorViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFromUri(context, it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { viewModel.exportToUri(context, it) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { HeroHeader() }
            item {
                ActionPanel(
                    sourceName = uiState.sourceName,
                    isLoading = uiState.isLoading,
                    canExport = uiState.report != null,
                    error = uiState.error,
                    exportMessage = uiState.exportMessage,
                    onImport = {
                        importLauncher.launch(
                            arrayOf("text/*", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        )
                    },
                    onExport = { exportLauncher.launch("student_grade_report.xlsx") },
                )
            }
            uiState.report?.let { report ->
                item { SummarySection(report.summary) }
                item { ChartSection(uiState.chart.points) }
                item { ResultsPreview(report.results) }
                item { IssuesSection(report.issues) }
            }
        }
    }
}

@Composable
private fun HeroHeader() {
    Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(ComposeColor(0xFF173E64), ComposeColor(0xFF3B6C9B), ComposeColor(0xFF5FA8D4)),
                    )
                )
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Student Grade Calculator",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ComposeColor.White,
                )
                Text(
                    text = "Kotlin Android edition - cleaner UI, strict rules, fast local processing.",
                    color = ComposeColor(0xFFDCEBFF),
                )
            }
        }
    }
}

@Composable
private fun ActionPanel(
    sourceName: String,
    isLoading: Boolean,
    canExport: Boolean,
    error: String?,
    exportMessage: String?,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Panel(title = "Import and Export") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "CSV/XLSX only. Latest duplicate row wins and every issue is listed.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onImport) { Text("Import File") }
                Button(onClick = onExport, enabled = canExport && !isLoading) { Text("Export Workbook") }
            }
            MetaChip(text = "Source: $sourceName", bg = ComposeColor(0xFFE6F0FF), fg = ComposeColor(0xFF174A7A))
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            error?.let { MetaChip(text = it, bg = ComposeColor(0xFFFFE4E4), fg = ComposeColor(0xFF8C1515)) }
            exportMessage?.let {
                MetaChip(text = it, bg = ComposeColor(0xFFE5F6EA), fg = ComposeColor(0xFF1D6F3B))
            }
        }
    }
}

@Composable
private fun SummarySection(summary: ProcessingSummary) {
    val cards = listOf(
        Triple("Rows", summary.totalRows.toString(), ComposeColor(0xFF174A7A)),
        Triple("Average", "%.2f".format(summary.average), ComposeColor(0xFF2A7A4F)),
        Triple("Median", "%.2f".format(summary.median), ComposeColor(0xFF9B5E00)),
        Triple("Pass Rate", "%.2f%%".format(summary.passRate), ComposeColor(0xFF6B2FA4)),
    )

    Panel(title = "Summary") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (label, value, color) ->
                        SummaryTile(label = label, value = value, color = color, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartSection(points: List<ChartPoint>) {
    Panel(title = "Grade Distribution") {
        if (points.isEmpty()) {
            Text("No chart data available.")
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                factory = { context ->
                    PieChart(context).apply {
                        description.isEnabled = false
                        setUsePercentValues(false)
                        setDrawEntryLabels(true)
                        legend.isEnabled = true
                    }
                },
                update = { chart ->
                    val entries = points.map { PieEntry(it.count.toFloat(), it.label) }
                    val dataSet = PieDataSet(entries, "Grades").apply {
                        colors = ColorTemplate.COLORFUL_COLORS.toList()
                        valueTextSize = 12f
                        valueTextColor = Color.DKGRAY
                    }
                    chart.data = PieData(dataSet)
                    chart.invalidate()
                },
            )
        }
    }
}

@Composable
private fun ResultsPreview(results: List<GradeResult>) {
    // I keep this preview compact so very large class lists don't stall the UI.
    val preview = results.take(15)
    val header = listOf("Row", "Name", "Matricule", "Score", "Letter", "Pass", "Source")

    Panel(title = "Results Preview") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            HeaderRow(header)
            preview.forEach { item ->
                HorizontalDivider()
                DataRow(
                    listOf(
                        item.rowIndex.toString(),
                        item.name.orEmpty().ifBlank { "-" },
                        item.matricule.orEmpty().ifBlank { "-" },
                        item.finalScore?.let { "%.2f".format(it) } ?: "-",
                        item.letter.label,
                        if (item.pass) "Yes" else "No",
                        item.source,
                    )
                )
            }
        }
    }
}

@Composable
private fun IssuesSection(issues: List<ValidationIssue>) {
    Panel(title = "Issues (${issues.size})") {
        if (issues.isEmpty()) {
            Text("No issues detected.")
            return@Panel
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            issues.take(25).forEach { issue ->
                val (bg, fg) = when (issue.severity) {
                    IssueSeverity.ERROR -> ComposeColor(0xFFFFE4E4) to ComposeColor(0xFF8C1515)
                    IssueSeverity.WARNING -> ComposeColor(0xFFFFF3D5) to ComposeColor(0xFF8A5A00)
                    IssueSeverity.INFO -> ComposeColor(0xFFE6F0FF) to ComposeColor(0xFF174A7A)
                }
                Card(colors = CardDefaults.cardColors(containerColor = bg)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("[Row ${issue.rowIndex}] ${issue.code}", color = fg, fontWeight = FontWeight.Bold)
                        Text(issue.message, color = fg)
                    }
                }
            }
            if (issues.size > 25) {
                Text(
                    text = "Showing first 25 issues.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, color: ComposeColor, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.95f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = ComposeColor.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
            Text(value, color = ComposeColor.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun HeaderRow(values: List<String>) {
    Row {
        values.forEach { value ->
            Text(
                text = value,
                modifier = Modifier.width(125.dp).padding(vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DataRow(values: List<String>) {
    Row {
        values.forEach { value ->
            Text(
                text = value,
                modifier = Modifier.width(125.dp).padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MetaChip(text: String, bg: ComposeColor, fg: ComposeColor) {
    Card(colors = CardDefaults.cardColors(containerColor = bg)) {
        Text(
            text = text,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Panel(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

