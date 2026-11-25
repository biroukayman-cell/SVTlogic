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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import com.example.core.TeacherQuizDashboardSummary
import com.example.data.ai.QuizErrorsAiReport
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.SvtWarning
import com.example.ui.viewmodel.QuizAiErrorsState

@Composable
fun QuizFrequentErrorsDashboardCard(
    summary: TeacherQuizDashboardSummary,
    onAnalyzeErrorsWithAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quiz = summary.quiz
    val hasAttempts = summary.attemptsCount > 0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_quiz_errors_card_${quiz.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOfNotNull(
                        summary.className.takeIf { it.isNotBlank() },
                        quiz.chapterSvt.takeIf { it.isNotBlank() },
                        "${quiz.questions.size} QCM"
                    ).joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = (if (!hasAttempts) MaterialTheme.colorScheme.surfaceVariant else if (summary.averagePct >= 50) SvtSuccess else SvtWarning).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (hasAttempts) "${summary.averagePct}%" else "—",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!hasAttempts) MaterialTheme.colorScheme.onSurfaceVariant else if (summary.averagePct >= 50) SvtSuccess else SvtWarning,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = stringResource(
                    id = R.string.teacher_quiz_ai_errors_meta,
                    summary.attemptsCount,
                    summary.averagePct,
                    summary.errorRatePct
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasAttempts) {
                QuestionSuccessRatioBar(
                    succeededCount = summary.totalSucceededAnswers,
                    failedCount = summary.totalFailedAnswers
                )
            }

            FilledTonalButton(
                onClick = onAnalyzeErrorsWithAi,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_ai_errors_button_${quiz.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(id = R.string.teacher_quiz_ai_errors_btn))
            }
        }
    }
}

@Composable
fun QuizAiErrorsDialog(
    state: QuizAiErrorsState,
    onRetry: (quizId: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (state is QuizAiErrorsState.Idle) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val quizTitle = when (state) {
        is QuizAiErrorsState.Loading -> state.quizTitle
        is QuizAiErrorsState.Success -> state.quizTitle
        is QuizAiErrorsState.Error -> state.quizTitle
        QuizAiErrorsState.Idle -> ""
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val successState = state as? QuizAiErrorsState.Success ?: return@rememberLauncherForActivityResult
        if (targetUri == null) return@rememberLauncherForActivityResult
        val docTitle = context.getString(R.string.teacher_quiz_ai_errors_dialog_title, successState.quizTitle)
        val subtitle = buildQuizPdfSubtitle(context, successState)
        val sections = AiAnalysisPdfExporter.buildQuizErrorsSections(context, successState.report)
        AiAnalysisPdfExporter.writePdfToUri(
            context = context,
            targetUri = targetUri,
            documentTitle = docTitle,
            subtitle = subtitle,
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
        modifier = Modifier.testTag("quiz_ai_errors_dialog"),
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
                    text = stringResource(id = R.string.teacher_quiz_ai_errors_dialog_title, quizTitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            SecureDialogWindowEffect()
            when (state) {
                is QuizAiErrorsState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("quiz_ai_errors_loading"))
                        Text(
                            text = stringResource(id = R.string.teacher_quiz_ai_errors_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is QuizAiErrorsState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_ai_errors_error_card")
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

                is QuizAiErrorsState.Success -> {
                    val docTitle = stringResource(id = R.string.teacher_quiz_ai_errors_dialog_title, state.quizTitle)
                    val summaryHeader = stringResource(id = R.string.teacher_quiz_ai_errors_summary_title)
                    val errorsHeader = stringResource(id = R.string.teacher_quiz_ai_errors_list_title)
                    val causesHeader = stringResource(id = R.string.teacher_quiz_ai_errors_causes_title)
                    val recHeader = stringResource(id = R.string.teacher_quiz_ai_errors_remediation_title)
                    val copiedToast = stringResource(id = R.string.teacher_ai_analysis_copied)
                    val totalAnswers = state.totalSucceededAnswers + state.totalFailedAnswers
                    val errorRatePct = if (totalAnswers > 0) {
                        ((state.totalFailedAnswers.toDouble() / totalAnswers.toDouble()) * 100.0)
                            .toInt()
                            .coerceIn(0, 100)
                    } else 0
                    val metaText = buildList {
                        if (state.className.isNotBlank()) add(state.className)
                        add(
                            stringResource(
                                id = R.string.teacher_quiz_ai_errors_meta,
                                state.attemptsCount,
                                state.averagePct,
                                errorRatePct
                            )
                        )
                    }.joinToString(" • ")

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = metaText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 380.dp)
                                    .verticalScroll(rememberScrollState())
                                    .testTag("quiz_ai_errors_content"),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (state.report.overallSynthesis.isNotBlank()) {
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
                                                text = state.report.overallSynthesis,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }

                                if (state.report.frequentErrors.isNotEmpty()) {
                                    QuizErrorSectionCard(
                                        title = errorsHeader,
                                        items = state.report.frequentErrors,
                                        icon = Icons.Filled.Warning,
                                        accentColor = SvtCoralError
                                    )
                                }

                                if (state.report.misconceptionCauses.isNotEmpty()) {
                                    QuizErrorSectionCard(
                                        title = causesHeader,
                                        items = state.report.misconceptionCauses,
                                        icon = Icons.Filled.Psychology,
                                        accentColor = SvtWarning
                                    )
                                }

                                if (state.report.remediationActions.isNotEmpty()) {
                                    QuizErrorSectionCard(
                                        title = recHeader,
                                        items = state.report.remediationActions,
                                        icon = Icons.Filled.Lightbulb,
                                        accentColor = SvtSuccess
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
                                    val formatted = formatQuizErrorsAsText(
                                        title = docTitle,
                                        metaLine = metaText,
                                        summaryHeader = summaryHeader,
                                        errorsHeader = errorsHeader,
                                        causesHeader = causesHeader,
                                        recHeader = recHeader,
                                        report = state.report
                                    )
                                    clipboardManager.setText(AnnotatedString(formatted))
                                    Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quiz_ai_errors_copy_button")
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
                                        prefix = "Erreurs_QCM",
                                        subjectName = state.quizTitle
                                    )
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        val sections = AiAnalysisPdfExporter.buildQuizErrorsSections(
                                            context = context,
                                            report = state.report
                                        )
                                        val subtitle = buildQuizPdfSubtitle(context, state)
                                        AiAnalysisPdfExporter.exportToDownloadsAndGetUri(
                                            context = context,
                                            fileName = fileName,
                                            documentTitle = docTitle,
                                            subtitle = subtitle,
                                            sections = sections
                                        ).onSuccess { result ->
                                            val toastText = if (result.savedToDownloads) {
                                                context.getString(R.string.teacher_ai_pdf_saved, result.fileName)
                                            } else {
                                                context.getString(R.string.teacher_ai_pdf_saved_generic)
                                            }
                                            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                                            AiAnalysisPdfExporter.openOrSharePdf(context, result, docTitle)
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
                                    .testTag("quiz_ai_errors_download_pdf_button")
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

                QuizAiErrorsState.Idle -> Unit
            }
        },
        confirmButton = {
            when (state) {
                is QuizAiErrorsState.Success -> {
                    OutlinedButton(
                        onClick = { onRetry(state.quizId) },
                        modifier = Modifier.testTag("quiz_ai_errors_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(id = R.string.teacher_ai_analysis_retry))
                    }
                }

                is QuizAiErrorsState.Error -> {
                    Button(
                        onClick = { onRetry(state.quizId) },
                        modifier = Modifier.testTag("quiz_ai_errors_retry_button")
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
                modifier = Modifier.testTag("quiz_ai_errors_close_button")
            ) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}

@Composable
private fun QuizErrorSectionCard(
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

private fun buildQuizPdfSubtitle(
    context: android.content.Context,
    state: QuizAiErrorsState.Success
): String {
    val totalAnswers = state.totalSucceededAnswers + state.totalFailedAnswers
    val errorRatePct = if (totalAnswers > 0) {
        ((state.totalFailedAnswers.toDouble() / totalAnswers.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    } else 0
    return listOfNotNull(
        state.className.takeIf { it.isNotBlank() },
        context.getString(
            R.string.teacher_quiz_ai_errors_meta,
            state.attemptsCount,
            state.averagePct,
            errorRatePct
        )
    ).joinToString(" • ")
}

private fun formatQuizErrorsAsText(
    title: String,
    metaLine: String,
    summaryHeader: String,
    errorsHeader: String,
    causesHeader: String,
    recHeader: String,
    report: QuizErrorsAiReport
): String = buildString {
    appendLine(title)
    if (metaLine.isNotBlank()) appendLine(metaLine)
    appendLine()
    if (report.overallSynthesis.isNotBlank()) {
        appendLine(summaryHeader)
        appendLine(report.overallSynthesis)
        appendLine()
    }
    if (report.frequentErrors.isNotEmpty()) {
        appendLine(errorsHeader)
        report.frequentErrors.forEach { appendLine("• $it") }
        appendLine()
    }
    if (report.misconceptionCauses.isNotEmpty()) {
        appendLine(causesHeader)
        report.misconceptionCauses.forEach { appendLine("• $it") }
        appendLine()
    }
    if (report.remediationActions.isNotEmpty()) {
        appendLine(recHeader)
        report.remediationActions.forEach { appendLine("• $it") }
    }
}.trim()
