package com.joelf.studentgradecalc.ui.screens

import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.joelf.studentgradecalc.domain.model.GradeResult
import com.joelf.studentgradecalc.domain.model.IssueSeverity
import com.joelf.studentgradecalc.domain.model.ProcessingSummary
import com.joelf.studentgradecalc.domain.model.ValidationIssue
import com.joelf.studentgradecalc.ui.state.GradeCalculatorViewModel

@Composable
fun GradeCalculatorScreen(viewModel: GradeCalculatorViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importFromUri(context, uri)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ),
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(context, uri)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeroHeader()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Import and Process",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Supported: CSV and XLSX. Duplicate identifiers keep the latest row and log a warning.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = {
                            importLauncher.launch(arrayOf(
                                "text/*",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            ))
                        }) {
                            Text("Import File")
                        }

                        Button(
                            onClick = {
                                exportLauncher.launch("student_grade_report.xlsx")
                            },
                            enabled = uiState.report != null,
                        ) {
                            Text("Export Workbook")
                        }
                    }

                    Text(
                        text = "Source: ${uiState.sourceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (uiState.isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    uiState.error?.let { message ->
                        MessageBox(
                            message = message,
                            background = ComposeColor(0xFFFFE4E4),
                            textColor = ComposeColor(0xFF8C1515),
                        )
                    }

                    uiState.exportMessage?.let { message ->
                        MessageBox(
                            message = message,
                            background = ComposeColor(0xFFE5F6EA),
                            textColor = ComposeColor(0xFF1D6F3B),
                        )
                    }
                }
            }

            uiState.report?.let { report ->
                SummarySection(report.summary)
                ChartSection(uiState.chart.points)
                ResultsPreview(report.results)
                IssuesSection(report.issues)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeroHeader() {
    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(ComposeColor(0xFF173E64), ComposeColor(0xFF406A93))
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
                    text = "Kotlin Android Edition - clean academic UX and strict grading logic.",
                    color = ComposeColor(0xFFDCEBFF),
                )
            }
        }
    }
}

@Composable
private fun SummarySection(summary: ProcessingSummary) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTile("Rows", summary.totalRows.toString(), ComposeColor(0xFF174A7A), Modifier.weight(1f))
                SummaryTile("Average", "%.2f".format(summary.average), ComposeColor(0xFF2A7A4F), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTile("Median", "%.2f".format(summary.median), ComposeColor(0xFF885200), Modifier.weight(1f))
                SummaryTile("Pass Rate", "%.2f%%".format(summary.passRate), ComposeColor(0xFF6B2FA4), Modifier.weight(1f))
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
            Text(label, color = ComposeColor.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            Text(value, color = ComposeColor.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ChartSection(points: List<com.joelf.studentgradecalc.domain.model.ChartPoint>) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Grade Distribution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                            colors = ColorTemplate.MATERIAL_COLORS.toList()
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
}

@Composable
private fun ResultsPreview(results: List<GradeResult>) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Results Preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            HeaderRow(listOf("Row", "Name", "Matricule", "Score", "Letter", "Pass", "Source"))
            results.take(12).forEach { item ->
                Divider()
                DataRow(
                    values = listOf(
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
private fun HeaderRow(values: List<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        values.forEach { value ->
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DataRow(values: List<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        values.forEach { value ->
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun IssuesSection(issues: List<ValidationIssue>) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Issues", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (issues.isEmpty()) {
                Text("No issues detected.")
            } else {
                issues.forEach { issue ->
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
            }
        }
    }
}

@Composable
private fun MessageBox(message: String, background: ComposeColor, textColor: ComposeColor) {
    Surface(
        color = background,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            color = textColor,
            modifier = Modifier.padding(10.dp),
        )
    }
}


