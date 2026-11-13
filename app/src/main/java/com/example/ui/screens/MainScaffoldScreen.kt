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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.AppLanguage
import com.example.data.model.UserProfile
import com.example.ui.components.BadgeCelebrationDialog
import com.example.ui.components.LanguageSwitcherRow
import com.example.ui.components.NotificationsSheet
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtWarning
import com.example.ui.viewmodel.SvtMainViewModel

enum class SvtTab(
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    EXERCISES(R.string.tab_exercises, Icons.Filled.Quiz, Icons.Outlined.Quiz, "tab_exercises"),
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
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val isTeacher = profile.role == "teacher"
    var currentTab by rememberSaveable { mutableStateOf(SvtTab.EXERCISES) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showClassMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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
    val teacherQuizzes by viewModel.teacherQuizzes.collectAsStateWithLifecycle()
    val teacherAttempts by viewModel.teacherAttempts.collectAsStateWithLifecycle()
    val teacherResources by viewModel.teacherResources.collectAsStateWithLifecycle()
    val teacherVideoTracking by viewModel.teacherVideoTracking.collectAsStateWithLifecycle()
    val teacherAnnouncements by viewModel.teacherAnnouncements.collectAsStateWithLifecycle()
    val teacherMessages by viewModel.teacherMessages.collectAsStateWithLifecycle()

    val studentEnrollment by viewModel.studentEnrollment.collectAsStateWithLifecycle()
    val studentQuizzes by viewModel.studentQuizzes.collectAsStateWithLifecycle()
    val studentAttempts by viewModel.studentAttempts.collectAsStateWithLifecycle()
    val studentResources by viewModel.studentResources.collectAsStateWithLifecycle()
    val studentVideoTracking by viewModel.studentVideoTracking.collectAsStateWithLifecycle()
    val studentAnnouncements by viewModel.studentAnnouncements.collectAsStateWithLifecycle()
    val studentMessages by viewModel.studentMessages.collectAsStateWithLifecycle()
    val studentNotifications by viewModel.studentNotifications.collectAsStateWithLifecycle()

    val aiQuizState by viewModel.aiQuizState.collectAsStateWithLifecycle()
    val unlockedBadges by viewModel.newlyUnlockedBadges.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Request POST_NOTIFICATIONS politely on Android 13+ for badge unlock alerts
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(profile.userId) {
        if (!isTeacher) {
            viewModel.syncStreakAndBadgesIfNeeded(context)
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
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
                    NavigationBar {
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
                    NavigationRail {
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
                                label = { Text(stringResource(id = tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
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
                                quizzes = if (isTeacher) teacherQuizzes else studentQuizzes,
                                attempts = if (isTeacher) teacherAttempts else studentAttempts,
                                downloadedQuizIds = downloadedQuizzes.keys,
                                pendingAttemptQuizIds = pendingAttempts.map { it.quizId }.toSet(),
                                aiQuizState = aiQuizState,
                                onGenerateWithAi = { chapter, level, count ->
                                    viewModel.generateQuizWithAi(chapter, level, count, currentLanguage)
                                },
                                onResetAiState = { viewModel.resetAiState() },
                                onPublishQuiz = { clsId, title, desc, chapter, questions, isDownloadable ->
                                    val notifTitle = context.getString(R.string.tab_exercises)
                                    viewModel.publishQuiz(
                                        clsId,
                                        title,
                                        desc,
                                        chapter,
                                        questions,
                                        notifTitle,
                                        title,
                                        isDownloadable
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
                                onAddResource = { clsId, title, desc, type, url, dur, isDownloadable ->
                                    val notifTitle = context.getString(R.string.tab_resources)
                                    viewModel.addResource(
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
                                onToggleResourceDownloadable = { resource ->
                                    viewModel.toggleResourceDownloadable(resource)
                                },
                                onDownloadResourceOffline = { resource ->
                                    viewModel.downloadResourceOffline(context, resource)
                                },
                                onRemoveOfflineResource = { resourceId ->
                                    viewModel.removeOfflineResource(resourceId)
                                },
                                onRecordVideoProgress = { res, posSec, durSec ->
                                    viewModel.recordVideoProgress(context, res, posSec, durSec)
                                }
                            )

                            SvtTab.ANNOUNCEMENTS -> AnnouncementsTab(
                                isTeacher = isTeacher,
                                selectedClassId = selectedClassId,
                                announcements = if (isTeacher) teacherAnnouncements else studentAnnouncements,
                                onPublishAnnouncement = { clsId, title, content ->
                                    val notifTitle = context.getString(R.string.tab_announcements)
                                    viewModel.publishAnnouncement(clsId, title, content, notifTitle, title)
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
                                studentEnrollment = studentEnrollment,
                                studentAttempts = studentAttempts,
                                studentVideoTrackings = studentVideoTracking,
                                teacherClasses = teacherClasses,
                                teacherEnrollments = teacherEnrollments,
                                onUpdateProfile = { name, phone, avatar ->
                                    viewModel.updateProfile(name, phone, avatar)
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
                                onRejectStudent = { viewModel.rejectEnrollment(it) },
                                onDeleteStudent = { viewModel.deleteStudent(it) },
                                onSignOut = onSignOut
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
