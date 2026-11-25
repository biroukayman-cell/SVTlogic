package com.example.ui.components

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AiAnalysisPdfExporter
import com.example.data.ai.ClassAiAnalysisReport
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.SvtWarning
import com.example.ui.viewmodel.ClassAiAnalysisState

@Composable
fun ClassAiAnalysisDialog(
    state: ClassAiAnalysisState,
    onRetry: (classId: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (state is ClassAiAnalysisState.Idle) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val className = when (state) {
        is ClassAiAnalysisState.Loading -> state.className
        is ClassAiAnalysisState.Success -> state.className
        is ClassAiAnalysisState.Error -> state.className
        ClassAiAnalysisState.Idle -> ""
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val successState = state as? ClassAiAnalysisState.Success ?: return@rememberLauncherForActivityResult
        if (targetUri == null) return@rememberLauncherForActivityResult
        val docTitle = context.getString(R.string.teacher_ai_analysis_dialog_title, successState.className)
        val sections = AiAnalysisPdfExporter.buildClassAnalysisSections(context, successState.report)
        AiAnalysisPdfExporter.writePdfToUri(
            context = context,
            targetUri = targetUri,
            documentTitle = docTitle,
            subtitle = successState.className,
            sections = sections
        ).onSuccess {
            Toast.makeText(
                context,
                context.getString(R.string.teacher_ai_pdf_saved_generic),
                Toast.LENGTH_SHORT
            ).show()
        }.onFailure { err ->
            Toast.makeText(
                context,
                context.getString(R.string.teacher_ai_pdf_error, err.localizedMessage ?: "PDF"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("class_ai_analysis_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = SvtEmeraldPrimary
                )
                Text(
                    text = stringResource(id = R.string.teacher_ai_analysis_dialog_title, className),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            SecureDialogWindowEffect()
            when (state) {
                is ClassAiAnalysisState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("class_ai_analysis_loading"))
                        Text(
                            text = stringResource(id = R.string.teacher_ai_analysis_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is ClassAiAnalysisState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("class_ai_analysis_error_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                is ClassAiAnalysisState.Success -> {
                    val summaryTitle = stringResource(id = R.string.teacher_ai_analysis_dialog_title, state.className)
                    val summaryHeader = stringResource(id = R.string.teacher_ai_analysis_summary_title)
                    val strengthsHeader = stringResource(id = R.string.teacher_ai_analysis_strengths_title)
                    val gapsHeader = stringResource(id = R.string.teacher_ai_analysis_gaps_title)
                    val recHeader = stringResource(id = R.string.teacher_ai_analysis_recommendations_title)
                    val copiedToast = stringResource(id = R.string.teacher_ai_analysis_copied)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                                    .verticalScroll(rememberScrollState())
                                    .testTag("class_ai_analysis_content"),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (state.report.textualSummary.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = summaryHeader,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = state.report.textualSummary,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }

                                if (state.report.strengths.isNotEmpty()) {
                                    AnalysisSectionCard(
                                        title = strengthsHeader,
                                        items = state.report.strengths,
                                        icon = Icons.Filled.CheckCircle,
                                        accentColor = SvtSuccess
                                    )
                                }

                                if (state.report.gaps.isNotEmpty()) {
                                    AnalysisSectionCard(
                                        title = gapsHeader,
                                        items = state.report.gaps,
                                        icon = Icons.Filled.Warning,
                                        accentColor = SvtCoralError
                                    )
                                }

                                if (state.report.recommendations.isNotEmpty()) {
                                    AnalysisSectionCard(
                                        title = recHeader,
                                        items = state.report.recommendations,
                                        icon = Icons.Filled.Lightbulb,
                                        accentColor = SvtWarning
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val formatted = formatReportAsText(
                                        title = summaryTitle,
                                        summaryHeader = summaryHeader,
                                        strengthsHeader = strengthsHeader,
                                        gapsHeader = gapsHeader,
                                        recHeader = recHeader,
                                        report = state.report
                                    )
                                    clipboardManager.setText(AnnotatedString(formatted))
                                    Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("class_ai_analysis_copy_button")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(id = R.string.teacher_ai_analysis_copy),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    val fileName = AiAnalysisPdfExporter.buildSuggestedFileName(
                                        prefix = "Analyse_Classe",
                                        subjectName = state.className
                                    )
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        val sections = AiAnalysisPdfExporter.buildClassAnalysisSections(
                                            context = context,
                                            report = state.report
                                        )
                                        AiAnalysisPdfExporter.exportToDownloadsAndGetUri(
                                            context = context,
                                            fileName = fileName,
                                            documentTitle = summaryTitle,
                                            subtitle = state.className,
                                            sections = sections
                                        ).onSuccess { result ->
                                            val toastText = if (result.savedToDownloads) {
                                                context.getString(R.string.teacher_ai_pdf_saved, result.fileName)
                                            } else {
                                                context.getString(R.string.teacher_ai_pdf_saved_generic)
                                            }
                                            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                                            AiAnalysisPdfExporter.openOrSharePdf(context, result, summaryTitle)
                                        }.onFailure { err ->
                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    R.string.teacher_ai_pdf_error,
                                                    err.localizedMessage ?: "PDF"
                                                ),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        createPdfLauncher.launch(fileName)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("class_ai_analysis_download_pdf_button")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(id = R.string.teacher_ai_download_pdf),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                ClassAiAnalysisState.Idle -> Unit
            }
        },
        confirmButton = {
            when (state) {
                is ClassAiAnalysisState.Success -> {
                    OutlinedButton(
                        onClick = { onRetry(state.classId) },
                        modifier = Modifier.testTag("class_ai_analysis_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(id = R.string.teacher_ai_analysis_retry))
                    }
                }

                is ClassAiAnalysisState.Error -> {
                    Button(
                        onClick = { onRetry(state.classId) },
                        modifier = Modifier.testTag("class_ai_analysis_retry_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.teacher_ai_analysis_retry))
                    }
                }

                else -> Unit
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("class_ai_analysis_close_button")
            ) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}

@Composable
private fun AnalysisSectionCard(
    title: String,
    items: List<String>,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        color = accentColor.copy(alpha = 0.09f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun formatReportAsText(
    title: String,
    summaryHeader: String,
    strengthsHeader: String,
    gapsHeader: String,
    recHeader: String,
    report: ClassAiAnalysisReport
): String = buildString {
    appendLine(title)
    appendLine()
    if (report.textualSummary.isNotBlank()) {
        appendLine(summaryHeader)
        appendLine(report.textualSummary)
        appendLine()
    }
    if (report.strengths.isNotEmpty()) {
        appendLine(strengthsHeader)
        report.strengths.forEach { appendLine("• $it") }
        appendLine()
    }
    if (report.gaps.isNotEmpty()) {
        appendLine(gapsHeader)
        report.gaps.forEach { appendLine("• $it") }
        appendLine()
    }
    if (report.recommendations.isNotEmpty()) {
        appendLine(recHeader)
        report.recommendations.forEach { appendLine("• $it") }
    }
}.trim()
