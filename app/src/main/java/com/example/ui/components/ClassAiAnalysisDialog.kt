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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.RosterStudent
import com.example.data.model.SvtClass
import com.example.data.model.VideoTracking
import com.example.ui.theme.SvtEmeraldPrimary
import kotlin.math.roundToInt

@Composable
fun ClassAiAnalysisDialog(
    initialClassId: String?,
    classes: List<SvtClass>,
    enrollments: List<Enrollment>,
    roster: List<RosterStudent>,
    quizzes: List<Quiz>,
    attempts: List<QuizAttempt>,
    resources: List<CourseResource>,
    videoTrackings: List<VideoTracking>,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedClassId by rememberSaveable(initialClassId) { mutableStateOf(initialClassId) }
    var pastedAiResponse by rememberSaveable { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf(false) }

    val targetClasses = remember(classes, selectedClassId) {
        if (selectedClassId.isNullOrBlank()) {
            classes
        } else {
            classes.filter { it.id == selectedClassId }.ifEmpty { classes }
        }
    }

    val analysisInputs = remember(
        targetClasses,
        enrollments,
        roster,
        quizzes,
        attempts,
        resources,
        videoTrackings
    ) {
        targetClasses.map { cls ->
            TeacherAnalyticsLogic.buildClassAiAnalysisInput(
                svtClass = cls,
                enrollments = enrollments,
                roster = roster,
                quizzes = quizzes,
                attempts = attempts,
                resources = resources,
                videoTrackings = videoTrackings
            )
        }
    }

    val promptText = remember(analysisInputs, currentLanguage) {
        if (analysisInputs.isEmpty()) {
            ""
        } else {
            ExternalAiAnalysisHelper.buildAllClassesAnalysisPrompt(analysisInputs, currentLanguage)
        }
    }

    val scopeDisplayName = remember(selectedClassId, targetClasses, classes) {
        if (selectedClassId.isNullOrBlank() && classes.size > 1) {
            context.getString(R.string.teacher_ai_prompt_all_classes, classes.size)
        } else {
            targetClasses.firstOrNull()?.name.orEmpty().ifBlank { "SVT" }
        }
    }

    val metricsLines = remember(analysisInputs) {
        if (analysisInputs.isEmpty()) {
            emptyList()
        } else {
            val totalStudents = analysisInputs.sumOf { it.totalStudentsCount }
            val activeStudents = analysisInputs.sumOf { it.activeQcmStudentsCount }
            val avgPct = analysisInputs.map { it.classAveragePct }.average().roundToInt()
            val totalOk = analysisInputs.sumOf { it.totalSucceededQuestions }
            val totalKo = analysisInputs.sumOf { it.totalFailedQuestions }
            val totalAns = totalOk + totalKo
            val qRate = if (totalAns > 0) ((totalOk.toDouble() / totalAns) * 100.0).roundToInt() else 0
            buildList {
                add("Classes analysées : ${analysisInputs.joinToString(", ") { it.className }}")
                add("Élèves suivis : $totalStudents (Actifs QCM : $activeStudents)")
                add("Moyenne QCM : $avgPct% • Réussite questions : $qRate% ($totalOk✓ / $totalKo✗)")
                analysisInputs.forEach { inp ->
                    add("• ${inp.className} : moy. ${inp.classAveragePct}%, ${inp.totalSucceededQuestions}✓ / ${inp.totalFailedQuestions}✗ (${inp.questionSuccessRatePct}%)")
                }
            }
        }
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        if (targetUri == null) return@rememberLauncherForActivityResult
        val docTitle = context.getString(R.string.teacher_ai_analysis_dialog_title, scopeDisplayName)
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
            subtitle = scopeDisplayName,
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
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = SvtEmeraldPrimary
                )
                Text(
                    text = stringResource(id = R.string.teacher_ai_analysis_dialog_title, scopeDisplayName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            SecureDialogWindowEffect()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.teacher_ai_prompt_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (classes.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedClassId == null,
                                onClick = {
                                    selectedClassId = null
                                    copiedNotice = false
                                },
                                label = {
                                    Text(stringResource(id = R.string.teacher_ai_prompt_all_classes, classes.size))
                                },
                                modifier = Modifier.testTag("class_prompt_all_classes_chip")
                            )
                        }
                        items(classes, key = { it.id }) { cls ->
                            FilterChip(
                                selected = selectedClassId == cls.id,
                                onClick = {
                                    selectedClassId = cls.id
                                    copiedNotice = false
                                },
                                label = {
                                    Text(cls.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                modifier = Modifier.testTag("class_prompt_chip_${cls.id}")
                            )
                        }
                    }
                }

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
                            text = promptText.ifBlank { stringResource(id = R.string.no_class_selected) },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 170.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                                .testTag("class_ai_prompt_preview_text")
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
                        enabled = promptText.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("class_ai_analysis_copy_button")
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
                                putExtra(Intent.EXTRA_SUBJECT, scopeDisplayName)
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
                        enabled = promptText.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("class_ai_prompt_share_button")
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
                        .testTag("class_ai_paste_response_input")
                )

                FilledTonalButton(
                    onClick = {
                        val fileName = AiAnalysisPdfExporter.buildSuggestedFileName(
                            prefix = "Analyse_Classe",
                            subjectName = scopeDisplayName
                        )
                        val docTitle = context.getString(R.string.teacher_ai_analysis_dialog_title, scopeDisplayName)
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
                                subtitle = scopeDisplayName,
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
                    enabled = promptText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("class_ai_analysis_download_pdf_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.teacher_ai_download_pdf))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("class_ai_analysis_close_button")
            ) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}
