package com.example.ui.components

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.core.AppLanguage
import com.example.core.ExternalAiAnalysisHelper
import com.example.core.TeacherAnalyticsLogic
import com.example.core.TeacherQuizDashboardSummary
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.SvtClass
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.SvtWarning
import kotlin.math.roundToInt

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
                    imageVector = Icons.Filled.ContentCopy,
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
    quiz: Quiz?,
    svtClass: SvtClass?,
    attempts: List<QuizAttempt>,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    if (quiz == null) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var pastedAiResponse by rememberSaveable(quiz.id) { mutableStateOf("") }
    var copiedNotice by remember(quiz.id) { mutableStateOf(false) }

    val analysisInput = remember(quiz, svtClass, attempts) {
        TeacherAnalyticsLogic.buildQuizFrequentErrorsInput(
            quiz = quiz,
            svtClass = svtClass,
            attempts = attempts
        )
    }

    val promptText = remember(analysisInput, currentLanguage) {
        if (analysisInput.attemptsCount == 0) {
            ""
        } else {
            ExternalAiAnalysisHelper.buildQuizFrequentErrorsPrompt(analysisInput, currentLanguage)
        }
    }

    val totalAnswers = analysisInput.totalSucceededAnswers + analysisInput.totalFailedAnswers
    val errorRatePct = if (totalAnswers > 0) {
        ((analysisInput.totalFailedAnswers.toDouble() / totalAnswers.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    } else 0

    val metaText = buildList {
        if (analysisInput.className.isNotBlank()) add(analysisInput.className)
        add(
            stringResource(
                id = R.string.teacher_quiz_ai_errors_meta,
                analysisInput.attemptsCount,
                analysisInput.averagePct,
                errorRatePct
            )
        )
    }.joinToString(" • ")

    val metricsLines = remember(analysisInput, metaText) {
        buildList {
            add("QCM : ${analysisInput.quizTitle}")
            if (analysisInput.chapterSvt.isNotBlank()) add("Chapitre : ${analysisInput.chapterSvt}")
            add(metaText)
            analysisInput.questionErrorMetrics.take(6).forEach { q ->
                add("• Q${q.questionNumber} (${q.errorRatePct}% erreur) : ${q.questionText}")
            }
        }
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        if (targetUri == null) return@rememberLauncherForActivityResult
        val docTitle = context.getString(R.string.teacher_quiz_ai_errors_dialog_title, quiz.title)
        val sections = AiAnalysisPdfExporter.buildExternalAnalysisSections(
            context = context,
            metricsTitle = context.getString(R.string.teacher_ai_pdf_metrics_section),
            metricsLines = metricsLines,
            pastedAiAnalysis = pastedAiResponse,
            promptText = promptText
        )
        AiAnalysisPdfExporter.writePdfToUri(
            context = context,
            targetUri = targetUri,
            documentTitle = docTitle,
            subtitle = metaText,
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
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = SvtEmeraldPrimary
                )
                Text(
                    text = stringResource(id = R.string.teacher_quiz_ai_errors_dialog_title, quiz.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            SecureDialogWindowEffect()
            if (analysisInput.attemptsCount == 0) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
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
                            text = stringResource(id = R.string.teacher_quiz_ai_errors_no_attempts),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.teacher_ai_prompt_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.teacher_ai_prompt_preview_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 170.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp)
                                    .testTag("quiz_ai_errors_prompt_preview")
                            )
                        }
                    }

                    if (copiedNotice) {
                        Text(
                            text = stringResource(id = R.string.teacher_ai_prompt_copied),
                            style = MaterialTheme.typography.labelMedium,
                            color = SvtEmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(promptText))
                                copiedNotice = true
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.teacher_ai_prompt_copied),
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quiz_ai_errors_copy_button")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.teacher_ai_prompt_copy),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, quiz.title)
                                    putExtra(Intent.EXTRA_TEXT, promptText)
                                }
                                val chooser = Intent.createChooser(
                                    sendIntent,
                                    context.getString(R.string.teacher_ai_prompt_share_chooser)
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(chooser) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quiz_ai_errors_share_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.teacher_ai_prompt_share),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = pastedAiResponse,
                        onValueChange = { pastedAiResponse = it },
                        label = { Text(stringResource(id = R.string.teacher_ai_paste_response_label)) },
                        placeholder = { Text(stringResource(id = R.string.teacher_ai_paste_response_hint)) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_ai_errors_paste_response_input")
                    )

                    FilledTonalButton(
                        onClick = {
                            val fileName = AiAnalysisPdfExporter.buildSuggestedFileName(
                                prefix = "Erreurs_QCM",
                                subjectName = quiz.title
                            )
                            val docTitle = context.getString(R.string.teacher_quiz_ai_errors_dialog_title, quiz.title)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val sections = AiAnalysisPdfExporter.buildExternalAnalysisSections(
                                    context = context,
                                    metricsTitle = context.getString(R.string.teacher_ai_pdf_metrics_section),
                                    metricsLines = metricsLines,
                                    pastedAiAnalysis = pastedAiResponse,
                                    promptText = promptText
                                )
                                AiAnalysisPdfExporter.exportToDownloadsAndGetUri(
                                    context = context,
                                    fileName = fileName,
                                    documentTitle = docTitle,
                                    subtitle = metaText,
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
                            .fillMaxWidth()
                            .testTag("quiz_ai_errors_download_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.teacher_ai_download_pdf))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("quiz_ai_errors_close_button")
            ) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}
