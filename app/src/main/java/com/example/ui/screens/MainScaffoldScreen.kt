package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.core.AppLanguage
import com.example.core.StudentCredentialsPdfExporter
import com.example.core.ThemeConfig
import com.example.data.demo.GuestDemoData
import com.example.data.model.AppUpdateRelease
import com.example.data.model.UserProfile
import com.example.data.svt.SvtDomain
import com.example.ui.components.BadgeCelebrationDialog
import com.example.ui.components.GuestReadOnlyBanner
import com.example.ui.components.LanguageSwitcherRow
import com.example.ui.components.NotificationsSheet
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtWarning
import com.example.ui.viewmodel.AiQuizState
import com.example.ui.viewmodel.LabSvtSearchState
import com.example.ui.viewmodel.SvtMainViewModel
import kotlinx.coroutines.launch

enum class SvtTab(
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    EXERCISES(R.string.tab_exercises, Icons.Filled.Quiz, Icons.Outlined.Quiz, "tab_exercises"),
    SVT_LAB(R.string.tab_svt_lab, Icons.Filled.Biotech, Icons.Outlined.Biotech, "tab_svt_lab"),
    RESOURCES(R.string.tab_resources, Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "tab_resources"),
    ANNOUNCEMENTS(R.string.tab_announcements, Icons.Filled.Campaign, Icons.Outlined.Campaign, "tab_announcements"),
    MESSAGING(R.string.tab_messaging, Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, "tab_messaging"),
    PROFILE(R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    profile: UserProfile,
    currentUserId: String,
    viewModel: SvtMainViewModel,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    currentTheme: ThemeConfig,
    onThemeChanged: (ThemeConfig) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val isTeacher = profile.role == "teacher"
    var currentTab by rememberSaveable { mutableStateOf(SvtTab.EXERCISES) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showClassMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val downloadedResources by viewModel.downloadedResources.collectAsStateWithLifecycle()
    val downloadedQuizzes by viewModel.downloadedQuizzes.collectAsStateWithLifecycle()
    val downloadingProgress by viewModel.downloadingProgress.collectAsStateWithLifecycle()
    val pendingAttempts by viewModel.pendingAttempts.collectAsStateWithLifecycle()
    val pendingVideoTrackings by viewModel.pendingVideoTrackings.collectAsStateWithLifecycle()
    val isSyncingOffline by viewModel.isSyncingOffline.collectAsStateWithLifecycle()

    val teacherClasses by viewModel.teacherClasses.collectAsStateWithLifecycle()
    val selectedClassId by viewModel.selectedClassId.collectAsStateWithLifecycle()
    val teacherEnrollments by viewModel.teacherEnrollments.collectAsStateWithLifecycle()
    val teacherRoster by viewModel.teacherRoster.collectAsStateWithLifecycle()
    val isImportingRoster by viewModel.isImportingRoster.collectAsStateWithLifecycle()
    val teacherQuizzes by viewModel.teacherQuizzes.collectAsStateWithLifecycle()
    val teacherAttempts by viewModel.teacherAttempts.collectAsStateWithLifecycle()
    val teacherResources by viewModel.teacherResources.collectAsStateWithLifecycle()
    val teacherVideoTracking by viewModel.teacherVideoTracking.collectAsStateWithLifecycle()
    val teacherAnnouncements by viewModel.teacherAnnouncements.collectAsStateWithLifecycle()
    val teacherMessages by viewModel.teacherMessages.collectAsStateWithLifecycle()

    val studentEnrollment by viewModel.studentEnrollment.collectAsStateWithLifecycle()
    val studentAllowedLabDomains by viewModel.studentAllowedLabDomains.collectAsStateWithLifecycle()
    val studentQuizzes by viewModel.studentQuizzes.collectAsStateWithLifecycle()
    val studentAttempts by viewModel.studentAttempts.collectAsStateWithLifecycle()
    val studentResources by viewModel.studentResources.collectAsStateWithLifecycle()
    val studentVideoTracking by viewModel.studentVideoTracking.collectAsStateWithLifecycle()
    val studentVideoOnlyTracking by viewModel.studentVideoOnlyTracking.collectAsStateWithLifecycle()
    val studentAnnouncements by viewModel.studentAnnouncements.collectAsStateWithLifecycle()
    val studentMessages by viewModel.studentMessages.collectAsStateWithLifecycle()
    val studentNotifications by viewModel.studentNotifications.collectAsStateWithLifecycle()

    val aiQuizState by viewModel.aiQuizState.collectAsStateWithLifecycle()
    val labSvtSearchState by viewModel.labSvtSearchState.collectAsStateWithLifecycle()
    val unlockedBadges by viewModel.newlyUnlockedBadges.collectAsStateWithLifecycle()
    val isUploadingResource by viewModel.isUploadingResource.collectAsStateWithLifecycle()
    val appUpdates by viewModel.appUpdates.collectAsStateWithLifecycle()
    val isSavingAvatar by viewModel.isSavingAvatar.collectAsStateWithLifecycle()
    val isOwnerAdminEmail by viewModel.isOwnerAdminEmail.collectAsStateWithLifecycle()
    val isAdminVerified by viewModel.isVerifiedOwnerAdmin.collectAsStateWithLifecycle()
    val allTeachersForAdmin by viewModel.allTeachersForAdmin.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    var dismissedUpdateId by rememberSaveable { mutableStateOf<String?>(null) }

    // Request POST_NOTIFICATIONS politely on Android 13+ for all signed-in users (students & teachers)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(profile.userId) {
        if (!isTeacher) {
            viewModel.syncStreakAndBadgesIfNeeded(context)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(actionError) {
        if (!actionError.isNullOrBlank()) {
            snackbarHostState.showSnackbar(actionError!!)
            viewModel.clearError()
        }
    }

    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusMessage!!)
            viewModel.clearStatusMessage()
        }
    }

    val unreadNotifications = studentNotifications.count { !it.isRead }
    val selectedClass = teacherClasses.firstOrNull { it.id == selectedClassId }
    val pendingSyncCount = pendingAttempts.size + pendingVideoTrackings.size
    val latestUpdate = appUpdates.maxByOrNull { it.versionCode }
    val availableUpdate = latestUpdate?.takeIf {
        it.versionCode > BuildConfig.VERSION_CODE && it.id != dismissedUpdateId
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    ),
                    title = {
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isTeacher && selectedClass != null) {
                                Box {
                                    TextButton(
                                        onClick = { showClassMenu = true },
                                        modifier = Modifier.testTag("class_selector_button")
                                    ) {
                                        Text(
                                            text = "${selectedClass.name} ▾",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showClassMenu,
                                        onDismissRequest = { showClassMenu = false }
                                    ) {
                                        teacherClasses.forEach { cls ->
                                            DropdownMenuItem(
                                                text = { Text(cls.name) },
                                                onClick = {
                                                    viewModel.selectClass(cls.id)
                                                    showClassMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else if (!isTeacher && studentEnrollment != null) {
                                Text(
                                    text = studentEnrollment!!.className,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    },
                    actions = {
                        LanguageSwitcherRow(
                            currentLanguage = currentLanguage,
                            onLanguageSelected = onLanguageSelected,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        if (!isTeacher) {
                            IconButton(
                                onClick = { showNotificationsSheet = true },
                                modifier = Modifier.testTag("notifications_bell_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifications > 0) {
                                            Badge { Text("$unreadNotifications") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = stringResource(id = R.string.notifications_title)
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        SvtTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = tab.labelRes),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        SvtTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = tab.labelRes),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    AnimatedVisibility(visible = availableUpdate != null) {
                        availableUpdate?.let { upd ->
                            AppUpdateAvailableBanner(
                                update = upd,
                                onDownloadClick = { openAppUpdateLink(context, upd.downloadUrl) },
                                onDismiss = { dismissedUpdateId = upd.id }
                            )
                        }
                    }

                    AnimatedVisibility(visible = !isOnline || pendingSyncCount > 0 || isSyncingOffline) {
                        OfflineStatusBanner(
                            isOnline = isOnline,
                            pendingSyncCount = pendingSyncCount,
                            isSyncing = isSyncingOffline,
                            onSyncNow = { viewModel.syncPendingOfflineData(context) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentTab) {
                            SvtTab.EXERCISES -> ExercisesTab(
                                isTeacher = isTeacher,
                                isOnline = isOnline,
                                selectedClassId = selectedClassId,
                                teacherClasses = teacherClasses,
                                quizzes = if (isTeacher) teacherQuizzes else studentQuizzes,
                                attempts = if (isTeacher) teacherAttempts else studentAttempts,
                                downloadedQuizIds = downloadedQuizzes.keys,
                                pendingAttemptQuizIds = pendingAttempts.map { it.quizId }.toSet(),
                                aiQuizState = aiQuizState,
                                onGenerateWithAi = { chapter, level, count ->
                                    viewModel.generateQuizWithAi(chapter, level, count, currentLanguage)
                                },
                                onResetAiState = { viewModel.resetAiState() },
                                onPublishQuiz = { clsIds, title, desc, chapter, questions, isDownloadable ->
                                    val notifTitle = context.getString(R.string.tab_exercises)
                                    viewModel.publishQuizToClasses(
                                        uiContext = context,
                                        classIds = clsIds,
                                        title = title,
                                        description = desc,
                                        chapterSvt = chapter,
                                        questions = questions,
                                        notifTitle = notifTitle,
                                        notifBody = title,
                                        isDownloadable = isDownloadable
                                    )
                                },
                                onToggleQuizDownloadable = { quiz ->
                                    viewModel.toggleQuizDownloadable(quiz)
                                },
                                onDownloadQuizOffline = { quiz ->
                                    viewModel.downloadQuizOffline(context, quiz)
                                },
                                onRemoveOfflineQuiz = { quizId ->
                                    viewModel.removeOfflineQuiz(quizId)
                                },
                                onSubmitAttempt = { quiz, selectedAnswers, perQSecs, totalSecs, onDone ->
                                    viewModel.submitQuizAttempt(context, quiz, selectedAnswers, perQSecs, totalSecs, onDone)
                                },
                                onDeleteQuiz = { quiz ->
                                    viewModel.deleteQuiz(context, quiz)
                                },
                                currentLanguage = currentLanguage
                            )

                            SvtTab.SVT_LAB -> SvtLabTabScreen(
                                isTeacher = isTeacher,
                                isOnline = isOnline,
                                currentLanguage = currentLanguage,
                                selectedTeacherClass = selectedClass,
                                studentAllowedDomains = studentAllowedLabDomains,
                                labSvtSearchState = labSvtSearchState,
                                onSearchLabSvtTerm = { query ->
                                    viewModel.searchLabSvtTerm(query, currentLanguage)
                                },
                                onClearLabSvtSearch = {
                                    viewModel.clearLabSvtSearch()
                                },
                                onUpdateClassLabDomains = { classId, domains ->
                                    viewModel.updateClassLabDomains(context, classId, domains)
                                }
                            )

                            SvtTab.RESOURCES -> ResourcesTab(
                                isTeacher = isTeacher,
                                isOnline = isOnline,
                                selectedClassId = selectedClassId,
                                resources = if (isTeacher) teacherResources else studentResources,
                                videoTrackings = if (isTeacher) teacherVideoTracking else studentVideoTracking,
                                classEnrollments = teacherEnrollments,
                                downloadedResources = downloadedResources,
                                downloadingProgress = downloadingProgress,
                                isUploadingResource = isUploadingResource,
                                onAddResource = { clsId, title, desc, type, url, dur, isDownloadable ->
                                    val notifTitle = context.getString(R.string.tab_resources)
                                    viewModel.addResource(
                                        context,
                                        clsId,
                                        title,
                                        desc,
                                        type,
                                        url,
                                        dur,
                                        notifTitle,
                                        title,
                                        isDownloadable
                                    )
                                },
                                onDeleteResource = { resource ->
                                    viewModel.deleteResource(resource)
                                },
                                onPrepareResourceForOpen = { resource, currentUri, onReady ->
                                    viewModel.prepareResourceUriForViewing(context, resource, currentUri, onReady)
                                },
                                onToggleResourceDownloadable = { resource ->
                                    viewModel.toggleResourceDownloadable(resource)
                                },
                                onDownloadResourceOffline = { resource ->
                                    viewModel.downloadResourceOffline(context, resource)
                                },
                                onRemoveOfflineResource = { resourceId ->
                                    viewModel.removeOfflineResource(resourceId)
                                },
                                onRecordVideoProgress = { res, posSec, durSec, pctOverride ->
                                    viewModel.recordVideoProgress(context, res, posSec, durSec, pctOverride)
                                },
                                onRecordFileOpened = { res ->
                                    viewModel.recordFileOpened(context, res)
                                }
                            )

                            SvtTab.ANNOUNCEMENTS -> AnnouncementsTab(
                                isTeacher = isTeacher,
                                selectedClassId = selectedClassId,
                                announcements = if (isTeacher) teacherAnnouncements else studentAnnouncements,
                                onPublishAnnouncement = { clsId, title, content ->
                                    val notifTitle = context.getString(R.string.tab_announcements)
                                    viewModel.publishAnnouncement(clsId, title, content, notifTitle, title)
                                },
                                onDeleteAnnouncement = { ann ->
                                    viewModel.deleteAnnouncement(context, ann)
                                }
                            )

                            SvtTab.MESSAGING -> MessagingTab(
                                isTeacher = isTeacher,
                                currentUserId = currentUserId,
                                selectedClassId = selectedClassId,
                                teacherEnrollments = teacherEnrollments,
                                messages = if (isTeacher) teacherMessages else studentMessages,
                                onSendAsStudent = { viewModel.sendMessageAsStudent(it) },
                                onSendAsTeacher = { clsId, stId, stName, content ->
                                    val notifTitle = context.getString(R.string.tab_messaging)
                                    viewModel.sendMessageAsTeacher(clsId, stId, stName, content, notifTitle)
                                },
                                onMarkThreadRead = { viewModel.markThreadRead(it) }
                            )

                            SvtTab.PROFILE -> ProfileAndBadgesTab(
                                profile = profile,
                                selectedClassId = selectedClassId,
                                currentTheme = currentTheme,
                                onThemeChanged = onThemeChanged,
                                studentEnrollment = studentEnrollment,
                                studentAttempts = studentAttempts,
                                studentVideoTrackings = studentVideoOnlyTracking,
                                teacherClasses = teacherClasses,
                                teacherEnrollments = teacherEnrollments,
                                teacherRoster = teacherRoster,
                                appUpdates = appUpdates,
                                isImportingRoster = isImportingRoster,
                                onUpdateProfile = { name, phone, avatar, birthDate, email ->
                                    viewModel.updateProfile(name, phone, avatar, birthDate, email, context)
                                },
                                onUpdateAvatarUri = { uri ->
                                    viewModel.updateProfileAvatar(context, uri)
                                },
                                onChangePassword = { currentPass, newPass, onDone ->
                                    viewModel.changeUserPassword(context, currentPass, newPass, onDone)
                                },
                                onCreateClass = { name, level, year ->
                                    viewModel.createClass(profile.fullName, name, level, year)
                                },
                                onDeleteClass = { viewModel.deleteClass(it) },
                                onApproveStudent = { enrollment ->
                                    val notifTitle = context.getString(R.string.app_name)
                                    val notifBody = enrollment.className
                                    viewModel.approveEnrollment(enrollment, notifTitle, notifBody)
                                },
                                onApproveAllPending = { pending ->
                                    val notifTitle = context.getString(R.string.app_name)
                                    val notifBody = pending.firstOrNull()?.className.orEmpty()
                                    viewModel.approveAllPending(pending, notifTitle, notifBody)
                                },
                                onRejectStudent = { viewModel.rejectEnrollment(it) },
                                onDeleteStudent = { viewModel.deleteStudent(it) },
                                onImportRoster = { svtClass, rows ->
                                    viewModel.importRosterStudentsToClass(context, svtClass, rows)
                                },
                                onAddManualStudent = { svtClass, name, massar ->
                                    viewModel.addManualRosterStudent(context, svtClass, name, massar)
                                },
                                onDeleteRosterStudent = { roster ->
                                    viewModel.deleteRosterStudent(roster)
                                },
                                onExportRosterPdf = { svtClass, students ->
                                    StudentCredentialsPdfExporter.exportAndSharePdf(context, svtClass, students)
                                        .onFailure { err ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(
                                                        R.string.pdf_export_error,
                                                        err.localizedMessage ?: "Error"
                                                    )
                                                )
                                            }
                                        }
                                },
                                onPublishAppUpdate = { clsId, vCode, vName, notes, url ->
                                    viewModel.publishAppUpdate(
                                        context,
                                        clsId,
                                        profile.fullName,
                                        vCode,
                                        vName,
                                        notes,
                                        url
                                    )
                                },
                                onPublishAppUpdateToClasses = { clsIds, vCode, vName, notes, url ->
                                    viewModel.publishAppUpdateToClasses(
                                        context,
                                        clsIds,
                                        profile.fullName,
                                        vCode,
                                        vName,
                                        notes,
                                        url
                                    )
                                },
                                onDeleteAppUpdate = { updateId ->
                                    viewModel.deleteAppUpdate(updateId)
                                },
                                onUpdateClassLabDomains = { classId, domains ->
                                    viewModel.updateClassLabDomains(context, classId, domains)
                                },
                                onResetStudentDevice = { studentId ->
                                    viewModel.resetStudentBoundDevice(context, studentId)
                                },
                                onShowError = { msg ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                },
                                onSignOut = onSignOut,
                                isGuestMode = false,
                                isSavingAvatar = isSavingAvatar,
                                isOwnerAdminEmail = isOwnerAdminEmail,
                                isAdminVerified = isAdminVerified,
                                allTeachersForAdmin = allTeachersForAdmin,
                                onApproveTeacher = { teacher -> viewModel.approveTeacher(context, teacher) },
                                onRejectTeacher = { teacher -> viewModel.rejectTeacher(context, teacher) },
                                onSendAdminVerification = { viewModel.sendOwnerAdminVerificationEmail(context) },
                                onRefreshAdminStatus = { viewModel.refreshAuthAndProfile(context) },
                                onForgotPassword = { cb ->
                                    viewModel.forgotPasswordWhileSignedIn(context, profile.phoneNumber, profile.email, cb)
                                },
                                onResetStudentPassword = { roster, enrollment ->
                                    viewModel.resetStudentProvisionalPassword(context, roster) { ok, newProvisional, err ->
                                        if (ok && !newProvisional.isNullOrBlank()) {
                                            val phone = enrollment?.studentPhone?.trim().orEmpty()
                                            val smsBody = context.getString(
                                                R.string.password_forgot_sms_body,
                                                roster.fullName,
                                                newProvisional,
                                                roster.massarNumber
                                            )
                                            launchSmsComposerOrShare(context, phone, smsBody)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (phone.isNotBlank()) {
                                                        context.getString(
                                                            R.string.password_forgot_provisional_phone_body,
                                                            phone,
                                                            newProvisional
                                                        )
                                                    } else {
                                                        context.getString(
                                                            R.string.password_forgot_provisional_no_phone_body,
                                                            newProvisional
                                                        )
                                                    }
                                                )
                                            }
                                        } else if (!err.isNullOrBlank()) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(err)
                                            }
                                        }
                                    }
                                },
                                onResetTeacherPassword = { teacher ->
                                    viewModel.sendAdminTeacherPasswordReset(context, teacher) { phone, smsBody ->
                                        launchSmsComposerOrShare(context, phone, smsBody)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        NotificationsSheet(
            notifications = studentNotifications,
            onMarkAllRead = { viewModel.markAllNotificationsRead() },
            onDismiss = { showNotificationsSheet = false }
        )
    }

    if (unlockedBadges.isNotEmpty()) {
        BadgeCelebrationDialog(
            badges = unlockedBadges,
            onDismiss = { viewModel.dismissUnlockedBadges() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestMainDashboardScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    currentTheme: ThemeConfig,
    onThemeChanged: (ThemeConfig) -> Unit,
    onExitGuest: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(SvtTab.EXERCISES) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val onGuestBlocked: () -> Unit = {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.guest_readonly_blocked_msg))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    ),
                    title = {
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = GuestDemoData.enrollment.className,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    actions = {
                        LanguageSwitcherRow(
                            currentLanguage = currentLanguage,
                            onLanguageSelected = onLanguageSelected,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        SvtTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = tab.labelRes),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("guest_${tab.testTag}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        SvtTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = tab.labelRes),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("guest_${tab.testTag}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    GuestReadOnlyBanner(onExitGuest = onExitGuest)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (currentTab) {
                            SvtTab.EXERCISES -> ExercisesTab(
                                isTeacher = false,
                                isOnline = false,
                                selectedClassId = GuestDemoData.GUEST_CLASS_ID,
                                teacherClasses = emptyList(),
                                quizzes = GuestDemoData.quizzes,
                                attempts = emptyList(),
                                downloadedQuizIds = emptySet(),
                                pendingAttemptQuizIds = emptySet(),
                                aiQuizState = AiQuizState.Idle,
                                onGenerateWithAi = { _, _, _ -> onGuestBlocked() },
                                onResetAiState = {},
                                onPublishQuiz = { _, _, _, _, _, _ -> onGuestBlocked() },
                                onToggleQuizDownloadable = { onGuestBlocked() },
                                onDownloadQuizOffline = { onGuestBlocked() },
                                onRemoveOfflineQuiz = { onGuestBlocked() },
                                onSubmitAttempt = { _, _, _, _, _ -> onGuestBlocked() },
                                currentLanguage = currentLanguage,
                                isGuestMode = true,
                                onGuestBlocked = onGuestBlocked
                            )

                            SvtTab.SVT_LAB -> SvtLabTabScreen(
                                isTeacher = false,
                                isOnline = false,
                                currentLanguage = currentLanguage,
                                selectedTeacherClass = null,
                                studentAllowedDomains = SvtDomain.entries.toSet(),
                                labSvtSearchState = LabSvtSearchState.Idle,
                                onSearchLabSvtTerm = { onGuestBlocked() },
                                onClearLabSvtSearch = {},
                                onUpdateClassLabDomains = { _, _ -> onGuestBlocked() },
                                isGuestMode = true,
                                onGuestBlocked = onGuestBlocked
                            )

                            SvtTab.RESOURCES -> ResourcesTab(
                                isTeacher = false,
                                isOnline = false,
                                selectedClassId = GuestDemoData.GUEST_CLASS_ID,
                                resources = GuestDemoData.resources,
                                videoTrackings = emptyList(),
                                classEnrollments = emptyList(),
                                downloadedResources = emptyMap(),
                                downloadingProgress = emptyMap(),
                                isUploadingResource = false,
                                onAddResource = { _, _, _, _, _, _, _ -> onGuestBlocked() },
                                onDeleteResource = { onGuestBlocked() },
                                onPrepareResourceForOpen = { _, _, _ -> onGuestBlocked() },
                                onToggleResourceDownloadable = { onGuestBlocked() },
                                onDownloadResourceOffline = { onGuestBlocked() },
                                onRemoveOfflineResource = { onGuestBlocked() },
                                onRecordVideoProgress = { _, _, _, _ -> onGuestBlocked() },
                                onRecordFileOpened = { onGuestBlocked() },
                                isGuestMode = true,
                                onGuestBlocked = onGuestBlocked
                            )

                            SvtTab.ANNOUNCEMENTS -> AnnouncementsTab(
                                isTeacher = false,
                                selectedClassId = GuestDemoData.GUEST_CLASS_ID,
                                announcements = GuestDemoData.announcements,
                                onPublishAnnouncement = { _, _, _ -> onGuestBlocked() },
                                isGuestMode = true,
                                onGuestBlocked = onGuestBlocked
                            )

                            SvtTab.MESSAGING -> MessagingTab(
                                isTeacher = false,
                                currentUserId = GuestDemoData.GUEST_USER_ID,
                                selectedClassId = GuestDemoData.GUEST_CLASS_ID,
                                teacherEnrollments = emptyList(),
                                messages = GuestDemoData.messages,
                                onSendAsStudent = { onGuestBlocked() },
                                onSendAsTeacher = { _, _, _, _ -> onGuestBlocked() },
                                onMarkThreadRead = {},
                                isGuestMode = true,
                                onGuestBlocked = onGuestBlocked
                            )

                            SvtTab.PROFILE -> ProfileAndBadgesTab(
                                profile = GuestDemoData.profile,
                                selectedClassId = GuestDemoData.GUEST_CLASS_ID,
                                currentTheme = currentTheme,
                                onThemeChanged = onThemeChanged,
                                studentEnrollment = GuestDemoData.enrollment,
                                studentAttempts = emptyList(),
                                studentVideoTrackings = emptyList(),
                                teacherClasses = emptyList(),
                                teacherEnrollments = emptyList(),
                                teacherRoster = emptyList(),
                                appUpdates = emptyList(),
                                isImportingRoster = false,
                                onUpdateProfile = { _, _, _, _, _ -> onGuestBlocked() },
                                onChangePassword = { _, _, onDone ->
                                    onDone(false, context.getString(R.string.guest_readonly_blocked_msg))
                                },
                                onCreateClass = { _, _, _ -> onGuestBlocked() },
                                onDeleteClass = { onGuestBlocked() },
                                onApproveStudent = { onGuestBlocked() },
                                onApproveAllPending = { onGuestBlocked() },
                                onRejectStudent = { onGuestBlocked() },
                                onDeleteStudent = { onGuestBlocked() },
                                onImportRoster = { _, _ -> onGuestBlocked() },
                                onAddManualStudent = { _, _, _ -> onGuestBlocked() },
                                onDeleteRosterStudent = { onGuestBlocked() },
                                onExportRosterPdf = { _, _ -> onGuestBlocked() },
                                onPublishAppUpdate = { _, _, _, _, _ -> onGuestBlocked() },
                                onDeleteAppUpdate = { onGuestBlocked() },
                                onUpdateClassLabDomains = { _, _ -> onGuestBlocked() },
                                onResetStudentDevice = { onGuestBlocked() },
                                onShowError = { onGuestBlocked() },
                                onSignOut = onExitGuest,
                                isGuestMode = true,
                                onGuestBlocked = onGuestBlocked
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineStatusBanner(
    isOnline: Boolean,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onSyncNow: () -> Unit
) {
    val bgColor = if (!isOnline) SvtWarning.copy(alpha = 0.16f) else SvtEmeraldPrimary.copy(alpha = 0.14f)
    val contentColor = if (!isOnline) SvtWarning else SvtEmeraldPrimary

    Surface(
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("offline_status_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    Icon(
                        imageVector = if (!isOnline) Icons.Filled.CloudOff else Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    if (!isOnline) {
                        Text(
                            text = stringResource(id = R.string.offline_mode_banner),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (pendingSyncCount > 0) {
                        Text(
                            text = if (isSyncing) {
                                stringResource(id = R.string.offline_syncing)
                            } else {
                                stringResource(id = R.string.offline_pending_sync_banner, pendingSyncCount)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isOnline && pendingSyncCount > 0 && !isSyncing) {
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onSyncNow,
                    modifier = Modifier.testTag("sync_now_button")
                ) {
                    Text(stringResource(id = R.string.offline_sync_now))
                }
            }
        }
    }
}

@Composable
private fun AppUpdateAvailableBanner(
    update: AppUpdateRelease,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_update_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        id = R.string.app_update_available_title,
                        update.versionName,
                        update.versionCode
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (update.releaseNotes.isNotBlank()) {
                    Text(
                        text = update.releaseNotes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = onDownloadClick) {
                Text(stringResource(id = R.string.offline_download_button))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        }
    }
}
