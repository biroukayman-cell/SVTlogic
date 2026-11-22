package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.ClassGlobalQuestionStats
import com.example.core.StudentClassQuestionStats
import com.example.core.StudentQcmResultSummary
import com.example.core.TeacherAnalyticsLogic
import com.example.core.TeacherDashboardOverview
import com.example.data.model.Announcement
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.RosterStudent
import com.example.data.model.SvtClass
import com.example.data.model.VideoTracking
import com.example.ui.components.QuestionSuccessRatioBar
import com.example.ui.components.StudentQuizAnswersReviewDialog
import com.example.ui.components.StudentQuizReviewEntry
import com.example.ui.components.SvtEmptyStateCard
import com.example.ui.components.SvtHeroBannerCard
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtOceanSecondary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.SvtWarning
import java.util.Locale

@Composable
fun TeacherDashboardTab(
    selectedSection: Int,
    onSelectSection: (Int) -> Unit,
    selectedClassId: String?,
    teacherClasses: List<SvtClass>,
    teacherEnrollments: List<Enrollment>,
    teacherRoster: List<RosterStudent>,
    teacherQuizzes: List<Quiz>,
    teacherAttempts: List<QuizAttempt>,
    teacherResources: List<CourseResource>,
    teacherVideoTracking: List<VideoTracking>,
    teacherAnnouncements: List<Announcement>,
    teacherMessages: List<PrivateMessage>,
    onNavigateToTab: (SvtTab) -> Unit
) {
    val overview = remember(
        teacherClasses,
        teacherEnrollments,
        teacherRoster,
        teacherQuizzes,
        teacherAttempts,
        teacherResources,
        teacherVideoTracking,
        teacherAnnouncements,
        teacherMessages
    ) {
        TeacherAnalyticsLogic.computeDashboardOverview(
            classes = teacherClasses,
            enrollments = teacherEnrollments,
            quizzes = teacherQuizzes,
            attempts = teacherAttempts,
            resources = teacherResources,
            videoTrackings = teacherVideoTracking,
            announcements = teacherAnnouncements,
            messages = teacherMessages,
            roster = teacherRoster
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { onSelectSection(0) },
                    leadingIcon = { Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(stringResource(id = R.string.teacher_dashboard_overview_tab)) },
                    modifier = Modifier.testTag("teacher_dashboard_overview_chip")
                )
            }
            item {
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { onSelectSection(1) },
                    leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(stringResource(id = R.string.teacher_dashboard_results_tab)) },
                    modifier = Modifier.testTag("teacher_dashboard_results_chip")
                )
            }
        }

        HorizontalDivider()

        AnimatedContent(
            targetState = selectedSection,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "teacher_dashboard_section_transition"
        ) { sec ->
            if (sec == 0) {
                TeacherOverviewSection(
                    overview = overview,
                    onOpenQcmResults = { onSelectSection(1) },
                    onNavigateToTab = onNavigateToTab
                )
            } else {
                TeacherStudentQcmResultsSection(
                    classes = teacherClasses,
                    enrollments = teacherEnrollments,
                    roster = teacherRoster,
                    quizzes = teacherQuizzes,
                    attempts = teacherAttempts,
                    initialClassFilterId = selectedClassId
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherOverviewSection(
    overview: TeacherDashboardOverview,
    onOpenQcmResults: () -> Unit,
    onNavigateToTab: (SvtTab) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_dashboard_overview_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SvtHeroBannerCard(
                title = stringResource(id = R.string.teacher_dashboard_title),
                subtitle = stringResource(id = R.string.teacher_dashboard_subtitle),
                backgroundResId = R.drawable.img_tab_exercises
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardKpiCard(
                        icon = Icons.Filled.Class,
                        title = stringResource(id = R.string.teacher_kpi_classes),
                        value = "${overview.totalClasses}",
                        subtitle = "${overview.totalAnnouncements} ${stringResource(id = R.string.tab_announcements)}",
                        accentColor = SvtEmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardKpiCard(
                        icon = Icons.Filled.People,
                        title = stringResource(id = R.string.teacher_kpi_students),
                        value = "${overview.totalApprovedStudents}",
                        subtitle = stringResource(id = R.string.teacher_kpi_pending, overview.totalPendingEnrollments),
                        accentColor = if (overview.totalPendingEnrollments > 0) SvtWarning else SvtOceanSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardKpiCard(
                        icon = Icons.Filled.Quiz,
                        title = stringResource(id = R.string.teacher_kpi_qcm_avg),
                        value = "${overview.globalAverageQcmPct}%",
                        subtitle = stringResource(id = R.string.teacher_kpi_copies, overview.totalAttempts, overview.totalQuizzes),
                        accentColor = if (overview.globalAverageQcmPct >= 50) SvtSuccess else SvtWarning,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenQcmResults)
                    )
                    DashboardKpiCard(
                        icon = Icons.Filled.MenuBook,
                        title = stringResource(id = R.string.teacher_kpi_courses_video),
                        value = "${overview.totalResources}",
                        subtitle = "${overview.videoCompletionRatePct}% • ${stringResource(id = R.string.teacher_kpi_unread_msgs, overview.unreadStudentMessages)}",
                        accentColor = SvtOceanSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(id = R.string.teacher_dashboard_shortcuts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenQcmResults,
                    modifier = Modifier.testTag("dashboard_shortcut_qcm_results")
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.teacher_dashboard_results_tab))
                }
                OutlinedButton(onClick = { onNavigateToTab(SvtTab.EXERCISES) }) {
                    Icon(Icons.Filled.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.tab_exercises))
                }
                OutlinedButton(onClick = { onNavigateToTab(SvtTab.RESOURCES) }) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.tab_resources))
                }
                OutlinedButton(onClick = { onNavigateToTab(SvtTab.ANNOUNCEMENTS) }) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.tab_announcements))
                }
                OutlinedButton(onClick = { onNavigateToTab(SvtTab.MESSAGING) }) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.tab_messaging))
                }
            }
        }

        item {
            Text(
                text = stringResource(id = R.string.teacher_dashboard_class_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (overview.classSummaries.isEmpty()) {
            item {
                SvtEmptyStateCard(title = stringResource(id = R.string.no_class_selected))
            }
        } else {
            items(overview.classSummaries, key = { it.svtClass.id }) { summary ->
                var showStudentBreakdown by rememberSaveable(summary.svtClass.id) { mutableStateOf(false) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
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
                                    text = summary.svtClass.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${summary.svtClass.level} • ${summary.svtClass.academicYear}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = (if (summary.classAveragePct >= 50) SvtSuccess else SvtWarning).copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${summary.classAveragePct}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.classAveragePct >= 50) SvtSuccess else SvtWarning,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (summary.classAveragePct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${summary.approvedStudentsCount} ${stringResource(id = R.string.teacher_kpi_students)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${summary.quizzesCount} QCM • ${summary.attemptsCount} copies • ${summary.resourcesCount} cours",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.teacher_class_questions_stats_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(
                                        id = R.string.teacher_questions_success_rate,
                                        summary.questionSuccessRatePct
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.questionSuccessRatePct >= 50) SvtSuccess else SvtWarning
                                )
                            }

                            QuestionSuccessRatioBar(
                                succeededCount = summary.totalSucceededQuestions,
                                failedCount = summary.totalFailedQuestions
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuestionCountPill(
                                    text = stringResource(
                                        id = R.string.teacher_questions_succeeded_count,
                                        summary.totalSucceededQuestions
                                    ),
                                    color = SvtSuccess,
                                    modifier = Modifier.weight(1f)
                                )
                                QuestionCountPill(
                                    text = stringResource(
                                        id = R.string.teacher_questions_failed_count,
                                        summary.totalFailedQuestions
                                    ),
                                    color = SvtCoralError,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (summary.studentQuestionStats.isNotEmpty()) {
                                TextButton(
                                    onClick = { showStudentBreakdown = !showStudentBreakdown },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("overview_class_students_toggle_${summary.svtClass.id}")
                                ) {
                                    Text(
                                        text = stringResource(
                                            id = R.string.teacher_class_student_questions_toggle,
                                            summary.studentQuestionStats.size
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (showStudentBreakdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null
                                    )
                                }

                                AnimatedVisibility(visible = showStudentBreakdown) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        summary.studentQuestionStats.forEach { st ->
                                            StudentQuestionBreakdownRow(studentStats = st)
                                        }
                                        OutlinedButton(
                                            onClick = onOpenQcmResults,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.FactCheck,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(stringResource(id = R.string.teacher_student_all_answers_btn))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCountPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.13f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StudentQuestionBreakdownRow(
    studentStats: StudentClassQuestionStats,
    onInspectClick: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onInspectClick != null && studentStats.totalQuestionsAnswered > 0) {
                    Modifier.clickable(onClick = onInspectClick)
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studentStats.studentName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (studentStats.massarNumber.isNotBlank()) {
                        Text(
                            text = studentStats.massarNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓ ${studentStats.succeededQuestions}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SvtSuccess
                    )
                    Text(
                        text = "✗ ${studentStats.failedQuestions}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SvtCoralError
                    )
                    Text(
                        text = "(${studentStats.successRatePct}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            QuestionSuccessRatioBar(
                succeededCount = studentStats.succeededQuestions,
                failedCount = studentStats.failedQuestions
            )
            if (onInspectClick != null && studentStats.totalQuestionsAnswered > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = stringResource(id = R.string.teacher_student_answers_btn),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardKpiCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TeacherStudentQcmResultsSection(
    classes: List<SvtClass>,
    enrollments: List<Enrollment>,
    roster: List<RosterStudent>,
    quizzes: List<Quiz>,
    attempts: List<QuizAttempt>,
    initialClassFilterId: String?
) {
    var filterClassId by rememberSaveable { mutableStateOf(initialClassFilterId) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortByPctDescending by rememberSaveable { mutableStateOf(true) }
    var reviewTarget by remember { mutableStateOf<Pair<StudentQcmResultSummary, String?>?>(null) }

    val baseResults = remember(classes, enrollments, roster, quizzes, attempts, filterClassId) {
        TeacherAnalyticsLogic.computeStudentQcmResults(
            classes = classes,
            enrollments = enrollments,
            roster = roster,
            quizzes = quizzes,
            attempts = attempts,
            filterClassId = filterClassId
        )
    }

    val classGlobalStats = remember(classes, enrollments, roster, quizzes, attempts, filterClassId) {
        TeacherAnalyticsLogic.computeClassGlobalQuestionStats(
            classes = classes,
            enrollments = enrollments,
            roster = roster,
            quizzes = quizzes,
            attempts = attempts,
            filterClassId = filterClassId
        )
    }

    val summaryByClassAndStudent = remember(baseResults) {
        baseResults.associateBy { "${it.classId}_${it.studentId}" }
    }

    val visibleResults = remember(baseResults, searchQuery, sortByPctDescending) {
        val cleanQuery = searchQuery.trim().lowercase()
        val filtered = if (cleanQuery.isBlank()) {
            baseResults
        } else {
            baseResults.filter { item ->
                item.studentName.lowercase().contains(cleanQuery) ||
                    item.massarNumber.lowercase().contains(cleanQuery) ||
                    item.className.lowercase().contains(cleanQuery)
            }
        }
        if (sortByPctDescending) {
            filtered.sortedWith(
                compareByDescending<StudentQcmResultSummary> { it.overallPercentage }
                    .thenBy { it.studentName.lowercase() }
            )
        } else {
            filtered.sortedBy { it.studentName.lowercase() }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_qcm_results_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.teacher_qcm_results_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.teacher_qcm_results_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterClassId == null,
                        onClick = { filterClassId = null },
                        label = { Text(stringResource(id = R.string.teacher_qcm_all_classes, classes.size)) },
                        modifier = Modifier.testTag("results_filter_all_classes")
                    )
                }
                items(classes, key = { it.id }) { cls ->
                    FilterChip(
                        selected = filterClassId == cls.id,
                        onClick = { filterClassId = cls.id },
                        label = { Text(cls.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("results_filter_class_${cls.id}")
                    )
                }
            }
        }

        if (classGlobalStats.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.teacher_class_global_stats_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("class_global_question_stats_header")
                )
            }
            items(classGlobalStats, key = { "global_${it.classId}" }) { clsStat ->
                ClassGlobalQuestionStatsCard(
                    stats = clsStat,
                    defaultExpanded = classGlobalStats.size == 1,
                    onInspectStudent = { studentStat ->
                        val summary = summaryByClassAndStudent["${clsStat.classId}_${studentStat.studentId}"]
                        if (summary != null && summary.perQuizResults.any { it.attempt != null }) {
                            reviewTarget = summary to null
                        }
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(id = R.string.teacher_qcm_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(id = R.string.cancel))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("teacher_qcm_search_field")
                )
                FilterChip(
                    selected = sortByPctDescending,
                    onClick = { sortByPctDescending = !sortByPctDescending },
                    leadingIcon = { Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = {
                        Text(
                            text = stringResource(
                                id = if (sortByPctDescending) R.string.teacher_qcm_sort_pct
                                else R.string.teacher_qcm_sort_name
                            ),
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("teacher_qcm_sort_chip")
                )
            }
        }

        if (visibleResults.isEmpty()) {
            item {
                SvtEmptyStateCard(title = stringResource(id = R.string.teacher_qcm_no_students))
            }
        } else {
            items(visibleResults, key = { "${it.classId}_${it.studentId}" }) { studentSummary ->
                StudentQcmResultCard(
                    summary = studentSummary,
                    onViewAllAnswers = {
                        reviewTarget = studentSummary to null
                    },
                    onViewQuizAnswers = { quizId ->
                        reviewTarget = studentSummary to quizId
                    }
                )
            }
        }
    }

    reviewTarget?.let { (studentSummary, initialQuizId) ->
        val entries = studentSummary.perQuizResults.mapNotNull { entry ->
            entry.attempt?.let { att ->
                StudentQuizReviewEntry(quiz = entry.quiz, attempt = att)
            }
        }
        val subtitle = listOfNotNull(
            studentSummary.className.takeIf { it.isNotBlank() },
            studentSummary.massarNumber.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        StudentQuizAnswersReviewDialog(
            studentName = studentSummary.studentName,
            studentSubtitle = subtitle,
            entries = entries,
            initialQuizId = initialQuizId,
            onDismiss = { reviewTarget = null }
        )
    }
}

@Composable
private fun ClassGlobalQuestionStatsCard(
    stats: ClassGlobalQuestionStats,
    defaultExpanded: Boolean,
    onInspectStudent: (StudentClassQuestionStats) -> Unit
) {
    var expanded by rememberSaveable(stats.classId, defaultExpanded) { mutableStateOf(defaultExpanded) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("class_global_stats_card_${stats.classId}")
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
                        text = stats.className,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            id = R.string.teacher_questions_answered_total,
                            stats.totalQuestionsAnswered,
                            stats.activeStudentsCount,
                            stats.totalStudents
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = (if (stats.successRatePct >= 50) SvtSuccess else SvtWarning).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.teacher_questions_success_rate, stats.successRatePct),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.successRatePct >= 50) SvtSuccess else SvtWarning,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            QuestionSuccessRatioBar(
                succeededCount = stats.totalSucceededQuestions,
                failedCount = stats.totalFailedQuestions
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuestionCountPill(
                    text = stringResource(id = R.string.teacher_questions_succeeded_count, stats.totalSucceededQuestions),
                    color = SvtSuccess,
                    modifier = Modifier.weight(1f)
                )
                QuestionCountPill(
                    text = stringResource(id = R.string.teacher_questions_failed_count, stats.totalFailedQuestions),
                    color = SvtCoralError,
                    modifier = Modifier.weight(1f)
                )
            }

            if (stats.studentStats.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("class_global_stats_expand_${stats.classId}")
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.teacher_class_student_questions_toggle,
                            stats.studentStats.size
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        stats.studentStats.forEach { st ->
                            StudentQuestionBreakdownRow(
                                studentStats = st,
                                onInspectClick = { onInspectStudent(st) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentQcmResultCard(
    summary: StudentQcmResultSummary,
    onViewAllAnswers: () -> Unit,
    onViewQuizAnswers: (quizId: String) -> Unit
) {
    var expanded by rememberSaveable(summary.classId, summary.studentId) { mutableStateOf(false) }
    val pctColor = when {
        summary.quizzesTakenCount == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        summary.overallPercentage >= 65 -> SvtSuccess
        summary.overallPercentage >= 45 -> SvtWarning
        else -> SvtCoralError
    }
    val hasAnyAttempt = summary.perQuizResults.any { it.attempt != null }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
            .testTag("student_qcm_card_${summary.studentId}")
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
                        text = summary.studentName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val infoLine = listOfNotNull(
                        summary.className.takeIf { it.isNotBlank() },
                        summary.massarNumber.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (infoLine.isNotBlank()) {
                        Text(
                            text = infoLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = pctColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.teacher_qcm_overall_pct, summary.overallPercentage),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = pctColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (summary.overallPercentage / 100f).coerceIn(0f, 1f) },
                color = pctColor,
                trackColor = pctColor.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Text(
                text = stringResource(
                    id = R.string.teacher_qcm_taken_count,
                    summary.quizzesTakenCount,
                    summary.totalClassQuizzesCount,
                    formatScoreValue(summary.totalScore),
                    formatScoreValue(summary.totalMaxScore)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuestionCountPill(
                    text = stringResource(id = R.string.teacher_questions_succeeded_count, summary.succeededQuestionsCount),
                    color = SvtSuccess,
                    modifier = Modifier.weight(1f)
                )
                QuestionCountPill(
                    text = stringResource(id = R.string.teacher_questions_failed_count, summary.failedQuestionsCount),
                    color = SvtCoralError,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(visible = expanded && summary.perQuizResults.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    if (hasAnyAttempt) {
                        FilledTonalButton(
                            onClick = onViewAllAnswers,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("view_student_all_answers_${summary.studentId}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FactCheck,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.teacher_student_all_answers_btn))
                        }
                    }
                    summary.perQuizResults.forEach { entry ->
                        val att = entry.attempt
                        val itemPct = entry.percentage
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.quiz.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    if (att != null && itemPct != null) {
                                        Text(
                                            text = "${formatScoreValue(att.score)}/${formatScoreValue(att.maxScore)} ($itemPct%)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (itemPct >= 50) SvtSuccess else SvtCoralError
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(id = R.string.teacher_qcm_not_taken),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (att != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = stringResource(
                                                    id = R.string.teacher_questions_succeeded_count,
                                                    entry.succeededQuestionsCount
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SvtSuccess,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = stringResource(
                                                    id = R.string.teacher_questions_failed_count,
                                                    entry.failedQuestionsCount
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SvtCoralError,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        TextButton(
                                            onClick = { onViewQuizAnswers(entry.quiz.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag(
                                                "view_student_quiz_answers_${summary.studentId}_${entry.quiz.id}"
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.FactCheck,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(id = R.string.teacher_student_answers_btn),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatScoreValue(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}
