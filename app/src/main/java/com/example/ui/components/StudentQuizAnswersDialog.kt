package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.QuestionAnswerFilter
import com.example.core.TeacherAnalyticsLogic
import com.example.data.model.QuestionAnswerRecord
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtSuccess

data class StudentQuizReviewEntry(
    val quiz: Quiz?,
    val attempt: QuizAttempt
)

@Composable
fun QuestionSuccessRatioBar(
    succeededCount: Int,
    failedCount: Int,
    modifier: Modifier = Modifier
) {
    val total = succeededCount + failedCount
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (total > 0) {
            Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                if (succeededCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(succeededCount.toFloat())
                            .height(8.dp)
                            .background(SvtSuccess)
                    )
                }
                if (failedCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(failedCount.toFloat())
                            .height(8.dp)
                            .background(SvtCoralError)
                    )
                }
            }
        }
    }
}

private data class FlattenedStudentAnswer(
    val quizTitle: String,
    val quiz: Quiz?,
    val record: QuestionAnswerRecord
)

@Composable
fun StudentQuizAnswersReviewDialog(
    studentName: String,
    studentSubtitle: String,
    entries: List<StudentQuizReviewEntry>,
    initialQuizId: String? = null,
    onDismiss: () -> Unit
) {
    var selectedQuizId by rememberSaveable(studentName, initialQuizId) {
        mutableStateOf(initialQuizId)
    }
    var activeFilter by rememberSaveable(studentName, initialQuizId) {
        mutableStateOf(QuestionAnswerFilter.ALL)
    }

    val scopedEntries = remember(entries, selectedQuizId) {
        if (selectedQuizId.isNullOrBlank()) {
            entries
        } else {
            entries.filter { it.attempt.quizId == selectedQuizId }.ifEmpty { entries }
        }
    }

    val allScopedAnswers = remember(scopedEntries) {
        scopedEntries.flatMap { entry ->
            val title = entry.quiz?.title?.takeIf { it.isNotBlank() } ?: entry.attempt.quizTitle
            entry.attempt.answers.map { rec ->
                FlattenedStudentAnswer(
                    quizTitle = title,
                    quiz = entry.quiz,
                    record = rec
                )
            }
        }
    }

    val counts = remember(scopedEntries) {
        var ok = 0
        var ko = 0
        scopedEntries.forEach { entry ->
            val (s, f) = TeacherAnalyticsLogic.extractAttemptQuestionCounts(entry.attempt, entry.quiz)
            ok += s
            ko += f
        }
        ok to ko
    }
    val totalCorrectCount = counts.first
    val totalIncorrectCount = counts.second
    val totalAllCount = totalCorrectCount + totalIncorrectCount

    val filteredAnswers = remember(allScopedAnswers, activeFilter) {
        when (activeFilter) {
            QuestionAnswerFilter.ALL -> allScopedAnswers
            QuestionAnswerFilter.CORRECT -> allScopedAnswers.filter { it.record.isCorrect }
            QuestionAnswerFilter.INCORRECT -> allScopedAnswers.filter { !it.record.isCorrect }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            SecureDialogWindowEffect()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.teacher_answers_dialog_title, studentName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (studentSubtitle.isNotBlank()) {
                    Text(
                        text = studentSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SvtSuccess.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.teacher_questions_succeeded_count, totalCorrectCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SvtSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = SvtCoralError.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.teacher_questions_failed_count, totalIncorrectCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SvtCoralError,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (entries.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedQuizId == null,
                                onClick = { selectedQuizId = null },
                                label = {
                                    Text(stringResource(id = R.string.teacher_answers_all_quizzes_chip, entries.size))
                                },
                                modifier = Modifier.testTag("answers_quiz_filter_all")
                            )
                        }
                        items(entries, key = { it.attempt.quizId }) { entry ->
                            val qTitle = entry.quiz?.title?.takeIf { it.isNotBlank() } ?: entry.attempt.quizTitle
                            FilterChip(
                                selected = selectedQuizId == entry.attempt.quizId,
                                onClick = { selectedQuizId = entry.attempt.quizId },
                                label = { Text(qTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("answers_quiz_filter_${entry.attempt.quizId}")
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = activeFilter == QuestionAnswerFilter.ALL,
                            onClick = { activeFilter = QuestionAnswerFilter.ALL },
                            label = { Text(stringResource(id = R.string.teacher_answers_filter_all, totalAllCount)) },
                            modifier = Modifier.testTag("answers_filter_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == QuestionAnswerFilter.CORRECT,
                            onClick = { activeFilter = QuestionAnswerFilter.CORRECT },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = SvtSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(stringResource(id = R.string.teacher_answers_filter_correct, totalCorrectCount))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SvtSuccess.copy(alpha = 0.16f),
                                selectedLabelColor = SvtSuccess
                            ),
                            modifier = Modifier.testTag("answers_filter_correct")
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == QuestionAnswerFilter.INCORRECT,
                            onClick = { activeFilter = QuestionAnswerFilter.INCORRECT },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = SvtCoralError,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(stringResource(id = R.string.teacher_answers_filter_incorrect, totalIncorrectCount))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SvtCoralError.copy(alpha = 0.16f),
                                selectedLabelColor = SvtCoralError
                            ),
                            modifier = Modifier.testTag("answers_filter_incorrect")
                        )
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (allScopedAnswers.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.teacher_answers_empty_detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else if (filteredAnswers.isEmpty()) {
                        val emptyMsg = when (activeFilter) {
                            QuestionAnswerFilter.CORRECT -> stringResource(id = R.string.teacher_answers_empty_correct)
                            QuestionAnswerFilter.INCORRECT -> stringResource(id = R.string.teacher_answers_empty_incorrect)
                            QuestionAnswerFilter.ALL -> stringResource(id = R.string.teacher_answers_empty_detail)
                        }
                        Text(
                            text = emptyMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        filteredAnswers.forEach { item ->
                            val rec = item.record
                            val q = item.quiz?.questions?.getOrNull(rec.questionIndex)
                            val accent = if (rec.isCorrect) SvtSuccess else SvtCoralError
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = accent.copy(alpha = 0.09f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = accent.copy(alpha = 0.35f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (selectedQuizId == null && entries.size > 1) {
                                        Text(
                                            text = item.quizTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Q${rec.questionIndex + 1}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = accent
                                            )
                                            Surface(
                                                color = accent.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(
                                                        id = if (rec.isCorrect) R.string.teacher_answers_status_correct
                                                        else R.string.teacher_answers_status_incorrect
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = accent,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Timer,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = stringResource(id = R.string.quiz_time_spent, rec.timeSpentSeconds),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (q != null) {
                                        if (q.imageUrl.isNotBlank()) {
                                            QuizQuestionImageBanner(
                                                imageUrl = q.imageUrl,
                                                contentDescription = stringResource(id = R.string.quiz_question_image_desc),
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = q.questionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        val chosenText = if (rec.selectedIndex >= 0) {
                                            q.options.getOrNull(rec.selectedIndex)
                                                ?: stringResource(id = R.string.teacher_answers_no_selection)
                                        } else {
                                            stringResource(id = R.string.teacher_answers_no_selection)
                                        }
                                        Text(
                                            text = stringResource(id = R.string.teacher_answers_student_choice, chosenText),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = accent,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (!rec.isCorrect) {
                                            val correctText = q.options.getOrNull(q.correctIndex) ?: "-"
                                            Text(
                                                text = stringResource(
                                                    id = R.string.teacher_answers_correct_choice,
                                                    correctText
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SvtSuccess,
                                                fontWeight = FontWeight.SemiBold
                                            )
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
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_student_answers_dialog_button")
            ) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}
