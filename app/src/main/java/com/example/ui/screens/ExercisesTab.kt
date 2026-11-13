package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.ui.components.SvtEmptyStateCard
import com.example.ui.components.SvtHeroBannerCard
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtOceanSecondary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.SvtWarning
import com.example.ui.viewmodel.AiQuizState
import kotlinx.coroutines.delay

@Composable
fun ExercisesTab(
    isTeacher: Boolean,
    isOnline: Boolean,
    selectedClassId: String?,
    quizzes: List<Quiz>,
    attempts: List<QuizAttempt>,
    downloadedQuizIds: Set<String>,
    pendingAttemptQuizIds: Set<String>,
    aiQuizState: AiQuizState,
    onGenerateWithAi: (chapter: String, level: String, count: Int) -> Unit,
    onResetAiState: () -> Unit,
    onPublishQuiz: (
        classId: String,
        title: String,
        description: String,
        chapter: String,
        questions: List<QuizQuestion>,
        isDownloadable: Boolean
    ) -> Unit,
    onToggleQuizDownloadable: (Quiz) -> Unit,
    onDownloadQuizOffline: (Quiz) -> Unit,
    onRemoveOfflineQuiz: (String) -> Unit,
    onSubmitAttempt: (
        quiz: Quiz,
        selectedAnswers: List<Int>,
        perQuestionSeconds: List<Long>,
        totalSeconds: Long,
        onDone: (QuizAttempt) -> Unit
    ) -> Unit
) {
    var activeQuizToPlay by remember { mutableStateOf<Quiz?>(null) }
    var attemptToView by remember { mutableStateOf<QuizAttempt?>(null) }
    var showCreateQuizDialog by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }
    var statsQuiz by remember { mutableStateOf<Quiz?>(null) }
    var offlineOnlyFilter by remember { mutableStateOf(false) }

    val classQuizzes = remember(quizzes, selectedClassId, isTeacher) {
        if (isTeacher && selectedClassId != null) quizzes.filter { it.classId == selectedClassId }
        else quizzes
    }

    val visibleQuizzes = remember(classQuizzes, offlineOnlyFilter, downloadedQuizIds, isTeacher) {
        if (!isTeacher && offlineOnlyFilter) {
            classQuizzes.filter { downloadedQuizIds.contains(it.id) }
        } else {
            classQuizzes
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SvtHeroBannerCard(
                title = stringResource(id = R.string.tab_exercises),
                subtitle = stringResource(id = R.string.app_subtitle)
            )
        }

        if (isTeacher) {
            item {
                if (selectedClassId == null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_class_selected),
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCreateQuizDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_quiz_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.quiz_create))
                        }
                        OutlinedButton(
                            onClick = {
                                onResetAiState()
                                showAiGeneratorDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_generate_quiz_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.quiz_ai_generate))
                        }
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !offlineOnlyFilter,
                        onClick = { offlineOnlyFilter = false },
                        label = { Text(stringResource(id = R.string.resource_type_all)) }
                    )
                    FilterChip(
                        selected = offlineOnlyFilter,
                        onClick = { offlineOnlyFilter = true },
                        leadingIcon = {
                            Icon(Icons.Filled.OfflinePin, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(stringResource(id = R.string.offline_filter_downloaded, downloadedQuizIds.size))
                        },
                        modifier = Modifier.testTag("quiz_offline_filter_chip")
                    )
                }
            }
        }

        if (visibleQuizzes.isEmpty()) {
            item {
                SvtEmptyStateCard(
                    title = stringResource(id = R.string.quiz_empty)
                )
            }
        } else {
            items(visibleQuizzes, key = { it.id }) { quiz ->
                val studentAttempt = if (!isTeacher) {
                    attempts.firstOrNull { it.quizId == quiz.id }
                } else null
                val quizAttempts = if (isTeacher) {
                    attempts.filter { it.quizId == quiz.id }
                } else emptyList()
                val isDownloaded = downloadedQuizIds.contains(quiz.id)
                val isPendingSync = pendingAttemptQuizIds.contains(quiz.id)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quiz.chapterSvt,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = quiz.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (!isTeacher) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (studentAttempt != null) SvtSuccess.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (studentAttempt != null) {
                                            stringResource(
                                                id = R.string.quiz_completed_tag,
                                                studentAttempt.score,
                                                studentAttempt.maxScore
                                            )
                                        } else {
                                            stringResource(id = R.string.quiz_pending_tag)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (studentAttempt != null) SvtSuccess else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (!isTeacher && (isDownloaded || isPendingSync)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isDownloaded) {
                                    Surface(
                                        color = SvtSuccess.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.OfflinePin,
                                                contentDescription = null,
                                                tint = SvtSuccess,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = stringResource(id = R.string.offline_downloaded_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SvtSuccess
                                            )
                                        }
                                    }
                                }
                                if (isPendingSync) {
                                    Surface(
                                        color = SvtWarning.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.CloudUpload,
                                                contentDescription = null,
                                                tint = SvtWarning,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = stringResource(id = R.string.offline_pending_sync_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SvtWarning
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (quiz.description.isNotBlank()) {
                            Text(
                                text = quiz.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isTeacher) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${quiz.questions.size} QCM",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilterChip(
                                    selected = quiz.isDownloadable,
                                    onClick = { onToggleQuizDownloadable(quiz) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (quiz.isDownloadable) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            stringResource(
                                                id = if (quiz.isDownloadable) R.string.teacher_downloadable_on
                                                else R.string.teacher_downloadable_off
                                            )
                                        )
                                    },
                                    modifier = Modifier.testTag("toggle_quiz_downloadable_${quiz.id}")
                                )
                            }
                            OutlinedButton(
                                onClick = { statsQuiz = quiz },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quiz_stats_${quiz.id}")
                            ) {
                                Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${stringResource(id = R.string.quiz_stats_title)} (${quizAttempts.size})")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${quiz.questions.size} QCM",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.weight(1f))

                                if (isDownloaded) {
                                    IconButton(
                                        onClick = { onRemoveOfflineQuiz(quiz.id) },
                                        modifier = Modifier.testTag("remove_offline_quiz_${quiz.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = stringResource(id = R.string.offline_remove_download),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else if (quiz.isDownloadable) {
                                    OutlinedButton(
                                        onClick = { onDownloadQuizOffline(quiz) },
                                        modifier = Modifier.testTag("download_quiz_${quiz.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DownloadForOffline,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(id = R.string.offline_download_button))
                                    }
                                }

                                if (studentAttempt == null) {
                                    val canPlay = isOnline || isDownloaded
                                    Button(
                                        onClick = { activeQuizToPlay = quiz },
                                        enabled = canPlay,
                                        modifier = Modifier.testTag("start_quiz_${quiz.id}")
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(id = R.string.quiz_start))
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { attemptToView = studentAttempt },
                                        modifier = Modifier.testTag("view_result_${quiz.id}")
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(id = R.string.quiz_result_title))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeQuizToPlay?.let { quiz ->
        TimedQuizPlayerDialog(
            quiz = quiz,
            onDismiss = { activeQuizToPlay = null },
            onComplete = { answers, perQuestionSecs, totalSecs ->
                activeQuizToPlay = null
                onSubmitAttempt(quiz, answers, perQuestionSecs, totalSecs) { savedAttempt ->
                    attemptToView = savedAttempt
                }
            }
        )
    }

    attemptToView?.let { attempt ->
        val quiz = quizzes.firstOrNull { it.id == attempt.quizId }
        QuizResultDetailDialog(
            quiz = quiz,
            attempt = attempt,
            isPendingSync = pendingAttemptQuizIds.contains(attempt.quizId),
            onDismiss = { attemptToView = null }
        )
    }

    statsQuiz?.let { quiz ->
        val quizAttempts = attempts.filter { it.quizId == quiz.id }
        TeacherQuizStatsDialog(
            quiz = quiz,
            attempts = quizAttempts,
            onDismiss = { statsQuiz = null }
        )
    }

    if (showAiGeneratorDialog && selectedClassId != null) {
        AiQuizGeneratorDialog(
            aiQuizState = aiQuizState,
            onGenerate = onGenerateWithAi,
            onPublishDraft = { draftTitle, draftDesc, chapter, questions, isDownloadable ->
                onPublishQuiz(selectedClassId, draftTitle, draftDesc, chapter, questions, isDownloadable)
                showAiGeneratorDialog = false
            },
            onDismiss = { showAiGeneratorDialog = false }
        )
    }

    if (showCreateQuizDialog && selectedClassId != null) {
        ManualCreateQuizDialog(
            onPublish = { title, desc, chapter, questions, isDownloadable ->
                onPublishQuiz(selectedClassId, title, desc, chapter, questions, isDownloadable)
                showCreateQuizDialog = false
            },
            onDismiss = { showCreateQuizDialog = false }
        )
    }
}

@Composable
private fun TimedQuizPlayerDialog(
    quiz: Quiz,
    onDismiss: () -> Unit,
    onComplete: (selectedAnswers: List<Int>, perQuestionSeconds: List<Long>, totalSeconds: Long) -> Unit
) {
    val questions = quiz.questions
    var currentIndex by remember { mutableIntStateOf(0) }
    val selectedAnswers = remember(quiz.id) { mutableStateListOf(*Array(questions.size) { -1 }) }
    val perQuestionSeconds = remember(quiz.id) { mutableStateListOf(*Array(questions.size) { 0L }) }
    var totalSeconds by remember(quiz.id) { mutableLongStateOf(0L) }

    LaunchedEffect(quiz.id, currentIndex) {
        while (true) {
            delay(1000L)
            totalSeconds += 1L
            if (currentIndex in perQuestionSeconds.indices) {
                perQuestionSeconds[currentIndex] = perQuestionSeconds[currentIndex] + 1L
            }
        }
    }

    val currentQ = questions.getOrNull(currentIndex) ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.quiz_question_progress, currentIndex + 1, questions.size),
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(id = R.string.cancel))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.quiz_timer_question,
                                    formatDuration(perQuestionSeconds.getOrElse(currentIndex) { 0L })
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.quiz_timer_total, formatDuration(totalSeconds)),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / questions.size.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = currentQ.questionText,
                    style = MaterialTheme.typography.titleMedium
                )
                currentQ.options.forEachIndexed { optIdx, optionText ->
                    val isSelected = selectedAnswers[currentIndex] == optIdx
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = MaterialTheme.shapes.medium
                            )
                            .clickable { selectedAnswers[currentIndex] = optIdx }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedAnswers[currentIndex] = optIdx }
                            )
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val hasSelection = selectedAnswers[currentIndex] != -1
            if (currentIndex < questions.lastIndex) {
                Button(
                    onClick = { currentIndex += 1 },
                    enabled = hasSelection
                ) {
                    Text(stringResource(id = R.string.quiz_next))
                }
            } else {
                Button(
                    onClick = {
                        onComplete(
                            selectedAnswers.toList(),
                            perQuestionSeconds.toList(),
                            totalSeconds
                        )
                    },
                    enabled = hasSelection
                ) {
                    Text(stringResource(id = R.string.quiz_finish))
                }
            }
        }
    )
}

@Composable
private fun QuizResultDetailDialog(
    quiz: Quiz?,
    attempt: QuizAttempt,
    isPendingSync: Boolean,
    onDismiss: () -> Unit
) {
    val pct = if (attempt.maxScore > 0.0) ((attempt.score / attempt.maxScore) * 100.0).toInt() else 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(id = R.string.quiz_result_title))
                Text(
                    text = stringResource(id = R.string.quiz_result_score, attempt.score, attempt.maxScore, pct),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (pct >= 50) SvtSuccess else SvtCoralError
                )
                Text(
                    text = stringResource(id = R.string.quiz_result_time, formatDuration(attempt.totalDurationSeconds)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isPendingSync) {
                    Surface(
                        color = SvtWarning.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.offline_pending_sync_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = SvtWarning,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                attempt.answers.forEach { rec ->
                    val q = quiz?.questions?.getOrNull(rec.questionIndex)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (rec.isCorrect) SvtSuccess.copy(alpha = 0.1f)
                            else SvtCoralError.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Q${rec.questionIndex + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (rec.isCorrect) SvtSuccess else SvtCoralError
                                )
                                Text(
                                    text = stringResource(id = R.string.quiz_time_spent, rec.timeSpentSeconds),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (q != null) {
                                Text(q.questionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                val chosen = q.options.getOrNull(rec.selectedIndex) ?: "-"
                                val correct = q.options.getOrNull(q.correctIndex) ?: "-"
                                Text("→ $chosen", style = MaterialTheme.typography.bodySmall)
                                if (!rec.isCorrect) {
                                    Text("✓ $correct", style = MaterialTheme.typography.bodySmall, color = SvtSuccess)
                                }
                                if (q.explanation.isNotBlank()) {
                                    Text(
                                        text = q.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(id = R.string.close)) }
        }
    )
}

@Composable
private fun TeacherQuizStatsDialog(
    quiz: Quiz,
    attempts: List<QuizAttempt>,
    onDismiss: () -> Unit
) {
    val avgPct = if (attempts.isEmpty()) 0 else {
        attempts.map { if (it.maxScore > 0) (it.score / it.maxScore) * 100.0 else 0.0 }.average().toInt()
    }
    val avgTime = if (attempts.isEmpty()) 0 else attempts.map { it.totalDurationSeconds }.average().toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResource(id = R.string.quiz_stats_title)} — ${quiz.title}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMetricChip(
                        label = stringResource(id = R.string.quiz_stats_avg_score, avgPct),
                        color = SvtEmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricChip(
                        label = stringResource(id = R.string.quiz_stats_avg_time, avgTime),
                        color = SvtOceanSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = stringResource(id = R.string.quiz_stats_attempts, attempts.size),
                    style = MaterialTheme.typography.labelMedium
                )
                if (attempts.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.quiz_stats_by_student),
                        style = MaterialTheme.typography.titleSmall
                    )
                    attempts.forEach { att ->
                        val pct = if (att.maxScore > 0) ((att.score / att.maxScore) * 100).toInt() else 0
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(att.studentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${att.score}/${att.maxScore} ($pct%) • ${att.totalDurationSeconds}s", style = MaterialTheme.typography.labelMedium)
                                }
                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(id = R.string.close)) }
        }
    )
}

@Composable
private fun StatMetricChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun AiQuizGeneratorDialog(
    aiQuizState: AiQuizState,
    onGenerate: (chapter: String, level: String, count: Int) -> Unit,
    onPublishDraft: (
        title: String,
        desc: String,
        chapter: String,
        questions: List<QuizQuestion>,
        isDownloadable: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var chapter by remember { mutableStateOf("La division cellulaire (Mitose & Méiose)") }
    var level by remember { mutableStateOf("2ème Bac SVT") }
    var countText by remember { mutableStateOf("4") }
    var isDownloadable by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(id = R.string.quiz_ai_title))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text(stringResource(id = R.string.quiz_ai_chapter)) }
                )
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text(stringResource(id = R.string.quiz_ai_level)) }
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(id = R.string.quiz_ai_count)) },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.teacher_mark_downloadable),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isDownloadable,
                        onCheckedChange = { isDownloadable = it }
                    )
                }

                when (aiQuizState) {
                    is AiQuizState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Text(stringResource(id = R.string.quiz_ai_generating))
                        }
                    }
                    is AiQuizState.Error -> {
                        Text(
                            text = stringResource(id = R.string.quiz_ai_error, aiQuizState.message),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is AiQuizState.Success -> {
                        val draft = aiQuizState.draft
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(draft.title, style = MaterialTheme.typography.titleSmall)
                                draft.questions.forEachIndexed { idx, q ->
                                    Text(
                                        text = "${idx + 1}. ${q.questionText}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    AiQuizState.Idle -> {}
                }
            }
        },
        confirmButton = {
            if (aiQuizState is AiQuizState.Success) {
                Button(
                    onClick = {
                        val draft = aiQuizState.draft
                        onPublishDraft(draft.title, draft.description, chapter, draft.questions, isDownloadable)
                    }
                ) {
                    Text(stringResource(id = R.string.quiz_publish))
                }
            } else {
                Button(
                    onClick = {
                        val count = (countText.toIntOrNull() ?: 4).coerceIn(1, 10)
                        onGenerate(chapter.trim(), level.trim(), count)
                    },
                    enabled = aiQuizState !is AiQuizState.Loading && chapter.isNotBlank()
                ) {
                    Text(stringResource(id = R.string.quiz_ai_generate))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun ManualCreateQuizDialog(
    onPublish: (
        title: String,
        desc: String,
        chapter: String,
        questions: List<QuizQuestion>,
        isDownloadable: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isDownloadable by remember { mutableStateOf(true) }
    var qText by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }
    val questionsList = remember { mutableStateListOf<QuizQuestion>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.quiz_create)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(id = R.string.quiz_title_label)) })
                OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text(stringResource(id = R.string.quiz_chapter_label)) })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(id = R.string.quiz_desc_label)) })

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.teacher_mark_downloadable),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isDownloadable,
                        onCheckedChange = { isDownloadable = it }
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text("${stringResource(id = R.string.quiz_add_question)} (${questionsList.size})", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(value = qText, onValueChange = { qText = it }, label = { Text(stringResource(id = R.string.quiz_question_text)) })
                OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text(stringResource(id = R.string.quiz_option_label, 1)) })
                OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text(stringResource(id = R.string.quiz_option_label, 2)) })
                OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text(stringResource(id = R.string.quiz_option_label, 3)) })

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(id = R.string.quiz_correct_option), style = MaterialTheme.typography.labelMedium)
                    (0..2).forEach { idx ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = correctIdx == idx, onClick = { correctIdx = idx })
                            Text("${idx + 1}")
                        }
                    }
                }
                OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text(stringResource(id = R.string.quiz_explanation_label)) })

                OutlinedButton(
                    onClick = {
                        val opts = listOf(opt1.trim(), opt2.trim(), opt3.trim()).filter { it.isNotEmpty() }
                        if (qText.isNotBlank() && opts.size >= 2) {
                            questionsList.add(
                                QuizQuestion(
                                    questionText = qText.trim(),
                                    options = opts,
                                    correctIndex = correctIdx.coerceIn(0, opts.lastIndex),
                                    explanation = explanation.trim()
                                )
                            )
                            qText = ""
                            opt1 = ""
                            opt2 = ""
                            opt3 = ""
                            explanation = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.quiz_add_question))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQuestions = questionsList.toMutableList()
                    val pendingOpts = listOf(opt1.trim(), opt2.trim(), opt3.trim()).filter { it.isNotEmpty() }
                    if (qText.isNotBlank() && pendingOpts.size >= 2) {
                        finalQuestions.add(
                            QuizQuestion(
                                questionText = qText.trim(),
                                options = pendingOpts,
                                correctIndex = correctIdx.coerceIn(0, pendingOpts.lastIndex),
                                explanation = explanation.trim()
                            )
                        )
                    }
                    if (title.trim().length >= 2 && finalQuestions.isNotEmpty()) {
                        onPublish(
                            title.trim(),
                            desc.trim(),
                            chapter.trim().ifBlank { "SVT" },
                            finalQuestions,
                            isDownloadable
                        )
                    }
                }
            ) {
                Text(stringResource(id = R.string.quiz_publish))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
