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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.core.AppLanguage
import com.example.core.ClassGlobalQuestionStats
import com.example.core.StudentClassQuestionStats
import com.example.core.StudentQcmResultSummary
import com.example.core.TeacherAnalyticsLogic
import com.example.core.TeacherDashboardOverview
import com.example.data.model.Announcement
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.Establishment
import com.example.data.model.PrivateMessage
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.RosterStudent
import com.example.data.model.SvtClass
import com.example.data.model.VideoTracking
import com.example.ui.components.ClassAiAnalysisDialog
import com.example.ui.components.CreateOrEditClassDialog
import com.example.ui.components.CreateOrEditEstablishmentDialog
import com.example.ui.components.OrderedStudentListSection
import com.example.ui.components.QuestionSuccessRatioBar
import com.example.ui.components.QuizFrequentErrorsDashboardCard
import com.example.ui.components.StudentListSortOption
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
    establishments: List<Establishment> = emptyList(),
    selectedEstablishmentId: String? = null,
    onSelectEstablishment: (String?) -> Unit = {},
    onCreateEstablishment: (name: String, city: String) -> Unit = { _, _ -> },
    onUpdateEstablishment: (Establishment, name: String, city: String) -> Unit = { _, _, _ -> },
    onDeleteEstablishment: (Establishment) -> Unit = {},
    onCreateClass: (name: String, level: String, academicYear: String, establishmentId: String, establishmentName: String) -> Unit = { _, _, _, _, _ -> },
    onUpdateClassDetails: (SvtClass, name: String, level: String, academicYear: String, establishmentId: String, establishmentName: String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteClass: (SvtClass) -> Unit = {},
    onSelectClass: (String) -> Unit = {},
    currentLanguage: AppLanguage = AppLanguage.FR,
    onAnalyzeQuizErrorsWithAi: (Quiz) -> Unit = {},
    onOpenPendingApprovals: () -> Unit = { onSelectSection(2) },
    onNavigateToTab: (SvtTab) -> Unit
) {
    val filteredClasses = remember(teacherClasses, selectedEstablishmentId) {
        TeacherAnalyticsLogic.filterClassesByEstablishment(teacherClasses, selectedEstablishmentId)
    }
    val filteredClassIds = remember(filteredClasses) {
        filteredClasses.map { it.id }.toSet()
    }
    val scopedEnrollments = remember(teacherEnrollments, filteredClassIds, selectedEstablishmentId) {
        if (selectedEstablishmentId == null) teacherEnrollments
        else teacherEnrollments.filter { it.classId in filteredClassIds }
    }
    val scopedRoster = remember(teacherRoster, filteredClassIds, selectedEstablishmentId) {
        if (selectedEstablishmentId == null) teacherRoster
        else teacherRoster.filter { it.classId in filteredClassIds }
    }
    val scopedQuizzes = remember(teacherQuizzes, filteredClassIds, selectedEstablishmentId) {
        if (selectedEstablishmentId == null) teacherQuizzes
        else teacherQuizzes.filter { it.classId in filteredClassIds }
    }
    val scopedAttempts = remember(teacherAttempts, filteredClassIds, selectedEstablishmentId) {
        if (selectedEstablishmentId == null) teacherAttempts
        else teacherAttempts.filter { it.classId in filteredClassIds }
    }
    val scopedResources = remember(teacherResources, filteredClassIds, selectedEstablishmentId) {
        if (selectedEstablishmentId == null) teacherResources
        else teacherResources.filter { it.classId in filteredClassIds }
    }
    val scopedVideoTracking = remember(teacherVideoTracking, filteredClassIds, selectedEstablishmentId) {
        if (selectedEstablishmentId == null) teacherVideoTracking
        else teacherVideoTracking.filter { it.classId in filteredClassIds }
    }
    val scopedAnnouncements = remember(teacherAnnouncements, filteredClassIds, selectedEstablishmentId) {
        if (selectedEstablishmentId == null) teacherAnnouncements
        else teacherAnnouncements.filter { it.classId in filteredClassIds }
    }
    val scopedMessages = remember(teacherMessages, filteredClassIds, selectedEstablishmentId) {
        if (selectedEstablishmentId == null) teacherMessages
        else teacherMessages.filter { it.classId in filteredClassIds }
    }

    val overview = remember(
        filteredClasses,
        scopedEnrollments,
        scopedRoster,
        scopedQuizzes,
        scopedAttempts,
        scopedResources,
        scopedVideoTracking,
        scopedAnnouncements,
        scopedMessages
    ) {
        TeacherAnalyticsLogic.computeDashboardOverview(
            classes = filteredClasses,
            enrollments = scopedEnrollments,
            quizzes = scopedQuizzes,
            attempts = scopedAttempts,
            resources = scopedResources,
            videoTrackings = scopedVideoTracking,
            announcements = scopedAnnouncements,
            messages = scopedMessages,
            roster = scopedRoster
        )
    }

    val advancedMetrics = remember(
        filteredClasses,
        scopedEnrollments,
        scopedRoster,
        scopedQuizzes,
        scopedAttempts
    ) {
        TeacherAnalyticsLogic.computeAdvancedMetrics(
            classes = filteredClasses,
            enrollments = scopedEnrollments,
            roster = scopedRoster,
            quizzes = scopedQuizzes,
            attempts = scopedAttempts
        )
    }

    var showClassPromptDialog by rememberSaveable { mutableStateOf(false) }
    var promptInitialClassId by rememberSaveable { mutableStateOf<String?>(null) }

    var showEstDialog by remember { mutableStateOf(false) }
    var editingEst by remember { mutableStateOf<Establishment?>(null) }
    var estToDelete by remember { mutableStateOf<Establishment?>(null) }

    var showClassDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<SvtClass?>(null) }
    var preselectedEstIdForNewClass by remember { mutableStateOf("") }
    var classToDelete by remember { mutableStateOf<SvtClass?>(null) }
    var qrDialogClass by remember { mutableStateOf<SvtClass?>(null) }
    var forcedQcmClassFilterId by rememberSaveable { mutableStateOf<String?>(null) }

    val unassignedClassCount = remember(teacherClasses) {
        teacherClasses.count { it.establishmentId.isBlank() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
            item {
                FilterChip(
                    selected = selectedSection == 2,
                    onClick = { onSelectSection(2) },
                    leadingIcon = { Icon(Icons.Filled.Apartment, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(stringResource(id = R.string.teacher_dashboard_manage_tab)) },
                    modifier = Modifier.testTag("teacher_dashboard_manage_chip")
                )
            }
        }

        if (establishments.isNotEmpty() || selectedSection == 2) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedEstablishmentId == null,
                        onClick = { onSelectEstablishment(null) },
                        label = {
                            Text(
                                stringResource(id = R.string.establishment_all_filter, teacherClasses.size),
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("dashboard_est_filter_all")
                    )
                }
                items(establishments, key = { it.id }) { est ->
                    FilterChip(
                        selected = selectedEstablishmentId == est.id,
                        onClick = { onSelectEstablishment(est.id) },
                        label = { Text(est.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("dashboard_est_filter_${est.id}")
                    )
                }
                if (unassignedClassCount > 0) {
                    item {
                        FilterChip(
                            selected = selectedEstablishmentId == TeacherAnalyticsLogic.UNASSIGNED_ESTABLISHMENT_FILTER,
                            onClick = { onSelectEstablishment(TeacherAnalyticsLogic.UNASSIGNED_ESTABLISHMENT_FILTER) },
                            label = {
                                Text(
                                    stringResource(id = R.string.establishment_unassigned_filter, unassignedClassCount),
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag("dashboard_est_filter_unassigned")
                        )
                    }
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            editingEst = null
                            showEstDialog = true
                        },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(stringResource(id = R.string.establishment_new_button), maxLines = 1) },
                        modifier = Modifier.testTag("dashboard_quick_new_est_chip")
                    )
                }
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
            when (sec) {
                0 -> TeacherOverviewSection(
                    overview = overview,
                    advancedMetrics = advancedMetrics,
                    hasClasses = filteredClasses.isNotEmpty(),
                    onOpenAllClassesPrompt = {
                        promptInitialClassId = null
                        showClassPromptDialog = true
                    },
                    onOpenClassPrompt = { cls ->
                        promptInitialClassId = cls.id
                        showClassPromptDialog = true
                    },
                    onOpenQcmResults = {
                        forcedQcmClassFilterId = null
                        onSelectSection(1)
                    },
                    onOpenEstablishmentsTab = { onSelectSection(2) },
                    onOpenPendingApprovals = onOpenPendingApprovals,
                    onNavigateToTab = onNavigateToTab
                )

                1 -> TeacherStudentQcmResultsSection(
                    classes = filteredClasses,
                    enrollments = scopedEnrollments,
                    roster = scopedRoster,
                    quizzes = scopedQuizzes,
                    attempts = scopedAttempts,
                    initialClassFilterId = forcedQcmClassFilterId ?: selectedClassId?.takeIf { it in filteredClassIds },
                    onAnalyzeClassWithAi = { cls ->
                        promptInitialClassId = cls.id
                        showClassPromptDialog = true
                    },
                    onAnalyzeQuizErrorsWithAi = onAnalyzeQuizErrorsWithAi
                )

                else -> TeacherEstablishmentsAndClassesSection(
                    establishments = establishments,
                    allClasses = teacherClasses,
                    filteredClasses = filteredClasses,
                    enrollments = teacherEnrollments,
                    selectedEstablishmentId = selectedEstablishmentId,
                    selectedClassId = selectedClassId,
                    onSelectEstablishment = onSelectEstablishment,
                    onCreateEstablishmentClick = {
                        editingEst = null
                        showEstDialog = true
                    },
                    onEditEstablishmentClick = { est ->
                        editingEst = est
                        showEstDialog = true
                    },
                    onDeleteEstablishmentClick = { est ->
                        estToDelete = est
                    },
                    onCreateClassClick = { estId ->
                        editingClass = null
                        preselectedEstIdForNewClass = estId
                        showClassDialog = true
                    },
                    onEditClassClick = { cls ->
                        editingClass = cls
                        preselectedEstIdForNewClass = cls.establishmentId
                        showClassDialog = true
                    },
                    onSelectClass = onSelectClass,
                    onShowQr = { cls -> qrDialogClass = cls },
                    onOpenClassResults = { cls ->
                        forcedQcmClassFilterId = cls.id
                        onSelectSection(1)
                    },
                    onDeleteClassClick = { cls -> classToDelete = cls }
                )
            }
        }
    }

    if (showClassPromptDialog && filteredClasses.isNotEmpty()) {
        ClassAiAnalysisDialog(
            initialClassId = promptInitialClassId,
            classes = filteredClasses,
            enrollments = scopedEnrollments,
            roster = scopedRoster,
            quizzes = scopedQuizzes,
            attempts = scopedAttempts,
            resources = scopedResources,
            videoTrackings = scopedVideoTracking,
            currentLanguage = currentLanguage,
            onDismiss = { showClassPromptDialog = false }
        )
    }

    if (showEstDialog) {
        CreateOrEditEstablishmentDialog(
            initialEstablishment = editingEst,
            onDismiss = { showEstDialog = false },
            onConfirm = { name, city ->
                val target = editingEst
                if (target == null) {
                    onCreateEstablishment(name, city)
                } else {
                    onUpdateEstablishment(target, name, city)
                }
                showEstDialog = false
            }
        )
    }

    estToDelete?.let { est ->
        AlertDialog(
            onDismissRequest = { estToDelete = null },
            title = { Text(stringResource(id = R.string.establishment_delete_confirm_title)) },
            text = { Text(stringResource(id = R.string.establishment_delete_confirm_body, est.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEstablishment(est)
                        estToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_est_dashboard_button")
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { estToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showClassDialog) {
        CreateOrEditClassDialog(
            establishments = establishments,
            initialClass = editingClass,
            preselectedEstablishmentId = preselectedEstIdForNewClass,
            onDismiss = { showClassDialog = false },
            onRequestQuickCreateEstablishment = {
                editingEst = null
                showEstDialog = true
            },
            onConfirm = { name, level, year, estId, estName ->
                val target = editingClass
                if (target == null) {
                    onCreateClass(name, level, year, estId, estName)
                } else {
                    onUpdateClassDetails(target, name, level, year, estId, estName)
                }
                showClassDialog = false
            }
        )
    }

    classToDelete?.let { cls ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_class_msg, cls.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClass(cls)
                        classToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_class_dashboard_button")
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    qrDialogClass?.let { cls ->
        ClassQrDialog(
            svtClass = cls,
            onDismiss = { qrDialogClass = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherOverviewSection(
    overview: TeacherDashboardOverview,
    advancedMetrics: TeacherAnalyticsLogic.TeacherAdvancedMetrics,
    hasClasses: Boolean,
    onOpenAllClassesPrompt: () -> Unit,
    onOpenClassPrompt: (SvtClass) -> Unit,
    onOpenQcmResults: () -> Unit,
    onOpenEstablishmentsTab: () -> Unit,
    onOpenPendingApprovals: () -> Unit,
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

        if (overview.totalPendingEnrollments > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teacher_pending_alert_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PendingActions,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = stringResource(
                                id = R.string.teacher_pending_alert_banner,
                                overview.totalPendingEnrollments
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = onOpenPendingApprovals,
                            modifier = Modifier.testTag("teacher_pending_alert_action_btn")
                        ) {
                            Text(stringResource(id = R.string.teacher_pending_alert_action))
                        }
                    }
                }
            }
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
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenEstablishmentsTab)
                    )
                    DashboardKpiCard(
                        icon = Icons.Filled.People,
                        title = stringResource(id = R.string.teacher_kpi_students),
                        value = "${overview.totalApprovedStudents}",
                        subtitle = stringResource(id = R.string.teacher_kpi_pending, overview.totalPendingEnrollments),
                        accentColor = if (overview.totalPendingEnrollments > 0) SvtWarning else SvtOceanSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenPendingApprovals)
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
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToTab(SvtTab.MESSAGING) }
                    )
                }

                DashboardKpiCard(
                    icon = Icons.Filled.TrendingUp,
                    title = stringResource(id = R.string.teacher_kpi_participation),
                    value = "${advancedMetrics.participationRatePct}%",
                    subtitle = stringResource(
                        id = R.string.teacher_kpi_participation_sub,
                        advancedMetrics.activeStudentsCount,
                        advancedMetrics.totalTrackedStudents,
                        formatDurationShort(advancedMetrics.averageQuizDurationSeconds)
                    ),
                    accentColor = if (advancedMetrics.participationRatePct >= 60) SvtSuccess else SvtOceanSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (advancedMetrics.totalTrackedStudents > 0) {
            item {
                TeacherAttentionCard(
                    metrics = advancedMetrics,
                    onContactStudent = { onNavigateToTab(SvtTab.MESSAGING) }
                )
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
                if (hasClasses) {
                    FilledTonalButton(
                        onClick = onOpenAllClassesPrompt,
                        modifier = Modifier.testTag("teacher_ai_analysis_shortcut_button")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.teacher_ai_analysis_btn))
                    }
                }
                OutlinedButton(
                    onClick = onOpenQcmResults,
                    modifier = Modifier.testTag("dashboard_shortcut_qcm_results")
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.teacher_dashboard_results_tab))
                }
                OutlinedButton(
                    onClick = onOpenEstablishmentsTab,
                    modifier = Modifier.testTag("dashboard_shortcut_establishments")
                ) {
                    Icon(Icons.Filled.Apartment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.teacher_dashboard_manage_tab))
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
                var breakdownSort by rememberSaveable(summary.svtClass.id) {
                    mutableStateOf(StudentListSortOption.NAME_ASC)
                }
                val sortedStudentStats = remember(summary.studentQuestionStats, breakdownSort) {
                    when (breakdownSort) {
                        StudentListSortOption.SCORE_DESC -> summary.studentQuestionStats.sortedWith(
                            compareByDescending<StudentClassQuestionStats> { it.successRatePct }
                                .thenBy { it.studentName.lowercase() }
                        )
                        StudentListSortOption.MASSAR_ASC -> summary.studentQuestionStats.sortedWith(
                            compareBy<StudentClassQuestionStats> { it.massarNumber.ifBlank { "ZZZZ" }.lowercase() }
                                .thenBy { it.studentName.lowercase() }
                        )
                        StudentListSortOption.NAME_ASC -> summary.studentQuestionStats.sortedBy {
                            it.studentName.lowercase()
                        }
                    }
                }

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
                                val subtitleParts = listOfNotNull(
                                    summary.svtClass.establishmentName.takeIf { it.isNotBlank() },
                                    summary.svtClass.level,
                                    summary.svtClass.academicYear
                                ).joinToString(" • ")
                                Text(
                                    text = subtitleParts,
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
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OrderedStudentListSection(
                                            items = sortedStudentStats,
                                            itemKey = { _, st -> "${summary.svtClass.id}_${st.studentId}" },
                                            maxListHeight = 280.dp,
                                            sortOption = breakdownSort,
                                            onSortChange = { breakdownSort = it },
                                            tagPrefix = "overview_students_${summary.svtClass.id}"
                                        ) { _, st ->
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

                            FilledTonalButton(
                                onClick = { onOpenClassPrompt(summary.svtClass) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("class_ai_analysis_button_${summary.svtClass.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(id = R.string.teacher_ai_analysis_btn))
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
    initialClassFilterId: String?,
    onAnalyzeClassWithAi: (SvtClass) -> Unit,
    onAnalyzeQuizErrorsWithAi: (Quiz) -> Unit
) {
    var filterClassId by rememberSaveable(initialClassFilterId) { mutableStateOf(initialClassFilterId) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortOption by rememberSaveable { mutableStateOf(StudentListSortOption.SCORE_DESC) }
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

    val quizSummaries = remember(classes, quizzes, attempts, filterClassId) {
        TeacherAnalyticsLogic.computeTeacherQuizDashboardSummaries(
            classes = classes,
            quizzes = quizzes,
            attempts = attempts,
            filterClassId = filterClassId
        )
    }

    val summaryByClassAndStudent = remember(baseResults) {
        baseResults.associateBy { "${it.classId}_${it.studentId}" }
    }

    val visibleResults = remember(baseResults, searchQuery, sortOption) {
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
        when (sortOption) {
            StudentListSortOption.SCORE_DESC -> filtered.sortedWith(
                compareByDescending<StudentQcmResultSummary> { it.overallPercentage }
                    .thenBy { it.studentName.lowercase() }
            )
            StudentListSortOption.MASSAR_ASC -> filtered.sortedWith(
                compareBy<StudentQcmResultSummary> { it.massarNumber.ifBlank { "ZZZZ" }.lowercase() }
                    .thenBy { it.studentName.lowercase() }
            )
            StudentListSortOption.NAME_ASC -> filtered.sortedBy { it.studentName.lowercase() }
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
                val targetClass = classes.firstOrNull { it.id == clsStat.classId }
                ClassGlobalQuestionStatsCard(
                    stats = clsStat,
                    defaultExpanded = classGlobalStats.size == 1,
                    onAnalyzeWithAi = targetClass?.let { cls -> { onAnalyzeClassWithAi(cls) } },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.teacher_quiz_ai_errors_section_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("teacher_quiz_ai_errors_section_header")
                )
                Text(
                    text = stringResource(id = R.string.teacher_quiz_ai_errors_section_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (quizSummaries.isEmpty()) {
            item {
                SvtEmptyStateCard(title = stringResource(id = R.string.teacher_quiz_ai_errors_empty_list))
            }
        } else {
            items(quizSummaries, key = { "quiz_err_${it.quiz.id}" }) { quizSummary ->
                QuizFrequentErrorsDashboardCard(
                    summary = quizSummary,
                    onAnalyzeErrorsWithAi = { onAnalyzeQuizErrorsWithAi(quizSummary.quiz) }
                )
            }
        }

        item {
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
                    .fillMaxWidth()
                    .testTag("teacher_qcm_search_field")
            )
        }

        if (visibleResults.isEmpty()) {
            item {
                SvtEmptyStateCard(title = stringResource(id = R.string.teacher_qcm_no_students))
            }
        } else {
            item {
                OrderedStudentListSection(
                    items = visibleResults,
                    itemKey = { _, item -> "${item.classId}_${item.studentId}" },
                    maxListHeight = 520.dp,
                    sortOption = sortOption,
                    onSortChange = { sortOption = it },
                    tagPrefix = "teacher_qcm_students"
                ) { _, studentSummary ->
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
    onAnalyzeWithAi: (() -> Unit)? = null,
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

            if (onAnalyzeWithAi != null) {
                FilledTonalButton(
                    onClick = onAnalyzeWithAi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qcm_class_ai_analysis_button_${stats.classId}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.teacher_ai_analysis_btn))
                }
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
                    var classStatsSort by rememberSaveable(stats.classId) {
                        mutableStateOf(StudentListSortOption.NAME_ASC)
                    }
                    val sortedClassStats = remember(stats.studentStats, classStatsSort) {
                        when (classStatsSort) {
                            StudentListSortOption.SCORE_DESC -> stats.studentStats.sortedWith(
                                compareByDescending<StudentClassQuestionStats> { it.successRatePct }
                                    .thenBy { it.studentName.lowercase() }
                            )
                            StudentListSortOption.MASSAR_ASC -> stats.studentStats.sortedWith(
                                compareBy<StudentClassQuestionStats> { it.massarNumber.ifBlank { "ZZZZ" }.lowercase() }
                                    .thenBy { it.studentName.lowercase() }
                            )
                            StudentListSortOption.NAME_ASC -> stats.studentStats.sortedBy {
                                it.studentName.lowercase()
                            }
                        }
                    }
                    OrderedStudentListSection(
                        items = sortedClassStats,
                        itemKey = { _, st -> "${stats.classId}_${st.studentId}" },
                        maxListHeight = 280.dp,
                        sortOption = classStatsSort,
                        onSortChange = { classStatsSort = it },
                        tagPrefix = "qcm_global_students_${stats.classId}"
                    ) { _, st ->
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

private fun formatDurationShort(seconds: Int): String {
    if (seconds <= 0) return "—"
    val mins = seconds / 60
    val rem = seconds % 60
    return if (mins > 0) "${mins}m ${rem}s" else "${rem}s"
}

@Composable
private fun TeacherAttentionCard(
    metrics: TeacherAnalyticsLogic.TeacherAdvancedMetrics,
    onContactStudent: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var sortOption by rememberSaveable { mutableStateOf(StudentListSortOption.SCORE_DESC) }

    val activeSource = if (selectedTab == 0) metrics.strugglingStudents else metrics.inactiveStudents
    val sortedItems = remember(activeSource, sortOption) {
        when (sortOption) {
            StudentListSortOption.SCORE_DESC -> activeSource.sortedWith(
                compareBy<TeacherAnalyticsLogic.TeacherAttentionStudent> { it.overallPercentage }
                    .thenBy { it.studentName.lowercase() }
            )
            StudentListSortOption.MASSAR_ASC -> activeSource.sortedWith(
                compareBy<TeacherAnalyticsLogic.TeacherAttentionStudent> { it.massarNumber.ifBlank { "ZZZZ" }.lowercase() }
                    .thenBy { it.studentName.lowercase() }
            )
            StudentListSortOption.NAME_ASC -> activeSource.sortedBy { it.studentName.lowercase() }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_attention_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (metrics.strugglingStudents.isEmpty() && metrics.inactiveStudents.isEmpty()) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.Warning
                    },
                    contentDescription = null,
                    tint = if (metrics.strugglingStudents.isEmpty() && metrics.inactiveStudents.isEmpty()) {
                        SvtSuccess
                    } else {
                        SvtWarning
                    }
                )
                Text(
                    text = stringResource(id = R.string.teacher_attention_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (metrics.strugglingStudents.isEmpty() && metrics.inactiveStudents.isEmpty()) {
                Surface(
                    color = SvtSuccess.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.teacher_attention_all_good),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SvtSuccess,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = {
                            Text(
                                text = "${stringResource(id = R.string.teacher_attention_struggling_title)} (${metrics.strugglingStudents.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attention_tab_struggling")
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = {
                            Text(
                                text = "${stringResource(id = R.string.teacher_attention_inactive_title)} (${metrics.inactiveStudents.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attention_tab_inactive")
                    )
                }

                if (sortedItems.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.teacher_attention_all_good),
                        style = MaterialTheme.typography.bodySmall,
                        color = SvtSuccess
                    )
                } else {
                    OrderedStudentListSection(
                        items = sortedItems,
                        itemKey = { _, st -> "${st.classId}_${st.studentId}" },
                        maxListHeight = 250.dp,
                        sortOption = sortOption,
                        onSortChange = { sortOption = it },
                        tagPrefix = "teacher_attention_students"
                    ) { _, st ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = st.studentName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val meta = listOfNotNull(
                                        st.className.takeIf { it.isNotBlank() },
                                        st.massarNumber.takeIf { it.isNotBlank() },
                                        if (st.isInactive) "0/${st.totalClassQuizzesCount} QCM"
                                        else "${st.overallPercentage}%"
                                    ).joinToString(" • ")
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (st.isInactive) SvtWarning else SvtCoralError
                                    )
                                }
                                TextButton(
                                    onClick = onContactStudent,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(id = R.string.teacher_attention_message_btn))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherEstablishmentsAndClassesSection(
    establishments: List<Establishment>,
    allClasses: List<SvtClass>,
    filteredClasses: List<SvtClass>,
    enrollments: List<Enrollment>,
    selectedEstablishmentId: String?,
    selectedClassId: String?,
    onSelectEstablishment: (String?) -> Unit,
    onCreateEstablishmentClick: () -> Unit,
    onEditEstablishmentClick: (Establishment) -> Unit,
    onDeleteEstablishmentClick: (Establishment) -> Unit,
    onCreateClassClick: (preselectedEstId: String) -> Unit,
    onEditClassClick: (SvtClass) -> Unit,
    onSelectClass: (String) -> Unit,
    onShowQr: (SvtClass) -> Unit,
    onOpenClassResults: (SvtClass) -> Unit,
    onDeleteClassClick: (SvtClass) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_dashboard_manage_list"),
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.establishments_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.establishments_section_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Button(
                        onClick = onCreateEstablishmentClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_new_establishment_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.establishment_new_button))
                    }
                }
            }
        }

        if (establishments.isEmpty()) {
            item {
                SvtEmptyStateCard(title = stringResource(id = R.string.establishment_empty_state))
            }
        } else {
            items(establishments, key = { "est_${it.id}" }) { est ->
                val estClassesCount = allClasses.count { it.establishmentId == est.id }
                val isSelectedEst = selectedEstablishmentId == est.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelectedEst) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_est_card_${est.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Apartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = est.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                val sub = listOfNotNull(
                                    est.city.takeIf { it.isNotBlank() },
                                    stringResource(id = R.string.establishment_classes_count, estClassesCount)
                                ).joinToString(" • ")
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onEditEstablishmentClick(est) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_edit_est_${est.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(id = R.string.establishment_edit_dialog_title),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { onDeleteEstablishmentClick(est) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_delete_est_${est.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(id = R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSelectEstablishment(if (isSelectedEst) null else est.id) },
                                modifier = Modifier.testTag("dashboard_filter_est_btn_${est.id}")
                            ) {
                                Text(
                                    stringResource(
                                        id = if (isSelectedEst) R.string.establishment_all_filter else R.string.establishment_classes_count,
                                        if (isSelectedEst) allClasses.size else estClassesCount
                                    )
                                )
                            }
                            FilledTonalButton(
                                onClick = { onCreateClassClick(est.id) },
                                modifier = Modifier.testTag("dashboard_add_class_est_${est.id}")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(id = R.string.establishment_add_class_inside))
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(id = R.string.teacher_classes_title)} (${filteredClasses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        val preEst = selectedEstablishmentId
                            ?.takeUnless { it == TeacherAnalyticsLogic.UNASSIGNED_ESTABLISHMENT_FILTER }
                            .orEmpty()
                        onCreateClassClick(preEst)
                    },
                    modifier = Modifier.testTag("dashboard_new_class_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.teacher_new_class))
                }
            }
        }

        if (filteredClasses.isEmpty()) {
            item {
                SvtEmptyStateCard(title = stringResource(id = R.string.no_class_selected))
            }
        } else {
            items(filteredClasses, key = { "cls_mgr_${it.id}" }) { svtClass ->
                val approvedCount = enrollments.count { it.classId == svtClass.id && it.status == "approved" }
                val pendingCount = enrollments.count { it.classId == svtClass.id && it.status == "pending" }
                val isSelectedClass = svtClass.id == selectedClassId

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_class_card_${svtClass.id}")
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
                                    text = svtClass.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${svtClass.level} • ${svtClass.academicYear}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (svtClass.establishmentName.isNotBlank()) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = svtClass.establishmentName.ifBlank {
                                        stringResource(id = R.string.class_unassigned_badge)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (svtClass.establishmentName.isNotBlank()) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "$approvedCount ${stringResource(id = R.string.teacher_kpi_students)} • ${stringResource(id = R.string.teacher_kpi_pending, pendingCount)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = isSelectedClass,
                                onClick = { onSelectClass(svtClass.id) },
                                label = { Text(svtClass.name) },
                                modifier = Modifier.testTag("dashboard_select_class_${svtClass.id}")
                            )
                            OutlinedButton(
                                onClick = { onEditClassClick(svtClass) },
                                modifier = Modifier.testTag("dashboard_edit_class_${svtClass.id}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(id = R.string.class_edit_button))
                            }
                            OutlinedButton(
                                onClick = { onShowQr(svtClass) },
                                modifier = Modifier.testTag("dashboard_qr_class_${svtClass.id}")
                            ) {
                                Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(id = R.string.teacher_show_qr))
                            }
                            OutlinedButton(
                                onClick = { onOpenClassResults(svtClass) },
                                modifier = Modifier.testTag("dashboard_results_class_${svtClass.id}")
                            ) {
                                Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(id = R.string.teacher_dashboard_results_tab))
                            }
                            IconButton(
                                onClick = { onDeleteClassClick(svtClass) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_delete_class_${svtClass.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(id = R.string.teacher_delete_class),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
