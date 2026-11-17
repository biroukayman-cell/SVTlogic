package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import com.example.BuildConfig
import com.example.R
import com.example.core.AppThemePalette
import com.example.core.ExcelParseErrorReason
import com.example.core.ExcelParseResult
import com.example.core.ExcelRosterParser
import com.example.core.ImportedStudentRow
import com.example.core.QrCodeUtils
import com.example.core.SecurityLockManager
import com.example.core.ThemeConfig
import com.example.core.ThemeMode
import com.example.ui.components.ProfileAvatarImage
import com.example.ui.components.SetupSecurityPinDialog
import com.example.data.badges.BadgeCategory
import com.example.data.badges.BadgeEngine
import com.example.data.badges.BadgeProgressItem
import com.example.data.model.AppUpdateRelease
import com.example.data.model.Enrollment
import com.example.data.model.QuizAttempt
import com.example.data.model.RosterStudent
import com.example.data.model.SvtClass
import com.example.data.model.UserProfile
import com.example.data.model.VideoTracking
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtGoldTertiary
import com.example.ui.theme.SvtOceanSecondary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.previewSwatchColors
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAndBadgesTab(
    profile: UserProfile,
    selectedClassId: String?,
    currentTheme: ThemeConfig,
    onThemeChanged: (ThemeConfig) -> Unit,
    studentEnrollment: Enrollment?,
    studentAttempts: List<QuizAttempt>,
    studentVideoTrackings: List<VideoTracking>,
    teacherClasses: List<SvtClass>,
    teacherEnrollments: List<Enrollment>,
    teacherRoster: List<RosterStudent>,
    appUpdates: List<AppUpdateRelease>,
    isImportingRoster: Boolean,
    onUpdateProfile: (fullName: String, phone: String, avatarUrl: String?, birthDate: String?) -> Unit,
    onUpdateAvatarUri: (Uri) -> Unit = {},
    onChangePassword: (currentPassword: String?, newPassword: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onCreateClass: (name: String, level: String, year: String) -> Unit,
    onDeleteClass: (SvtClass) -> Unit,
    onApproveStudent: (Enrollment) -> Unit,
    onApproveAllPending: (List<Enrollment>) -> Unit,
    onRejectStudent: (Enrollment) -> Unit,
    onDeleteStudent: (Enrollment) -> Unit,
    onImportRoster: (SvtClass, List<ImportedStudentRow>) -> Unit,
    onAddManualStudent: (SvtClass, String, String) -> Unit,
    onDeleteRosterStudent: (RosterStudent) -> Unit,
    onExportRosterPdf: (SvtClass, List<RosterStudent>) -> Unit,
    onPublishAppUpdate: (classId: String, versionCode: Int, versionName: String, releaseNotes: String, downloadUrl: String) -> Unit,
    onDeleteAppUpdate: (String) -> Unit,
    onUpdateClassLabDomains: (classId: String, domains: List<String>) -> Unit,
    onResetStudentDevice: (studentId: String) -> Unit,
    onShowError: (String) -> Unit,
    onSignOut: () -> Unit,
    isGuestMode: Boolean = false,
    isSavingAvatar: Boolean = false,
    isOwnerAdminEmail: Boolean = false,
    isAdminVerified: Boolean = false,
    allTeachersForAdmin: List<UserProfile> = emptyList(),
    onApproveTeacher: (UserProfile) -> Unit = {},
    onRejectTeacher: (UserProfile) -> Unit = {},
    onSendAdminVerification: () -> Unit = {},
    onRefreshAdminStatus: () -> Unit = {},
    onGuestBlocked: () -> Unit = {}
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showNewClassDialog by remember { mutableStateOf(false) }
    var showPublishUpdateDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var qrDialogClass by remember { mutableStateOf<SvtClass?>(null) }
    var classToDelete by remember { mutableStateOf<SvtClass?>(null) }
    var studentToDelete by remember { mutableStateOf<Enrollment?>(null) }
    var studentToResetDevice by remember { mutableStateOf<Enrollment?>(null) }
    var updateToDelete by remember { mutableStateOf<AppUpdateRelease?>(null) }
    var selectedBadgeCategory by remember { mutableStateOf<BadgeCategory?>(null) }

    var pendingExcelTargetClass by remember { mutableStateOf<SvtClass?>(null) }
    var parsedPreviewPair by remember { mutableStateOf<Pair<SvtClass, List<ImportedStudentRow>>?>(null) }
    var manualStudentTargetClass by remember { mutableStateOf<SvtClass?>(null) }
    var expandedRosterClassIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var isPinEnabled by remember(profile.userId, isGuestMode) {
        mutableStateOf(!isGuestMode && SecurityLockManager.isPinEnabled(context, profile.userId))
    }
    var hasNotifPermission by remember(isGuestMode) {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    val badgeItems = remember(profile, studentAttempts, studentVideoTrackings) {
        BadgeEngine.computeProgressItems(profile, studentAttempts, studentVideoTrackings)
    }
    val filteredBadges = remember(badgeItems, selectedBadgeCategory) {
        if (selectedBadgeCategory == null) badgeItems
        else badgeItems.filter { it.definition.category == selectedBadgeCategory }
    }
    val levelInfo = remember(profile.xp) { BadgeEngine.computeLevelInfo(profile.xp) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            if (!isGuestMode) {
                onUpdateAvatarUri(it)
            }
        }
    }

    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val targetClass = pendingExcelTargetClass
        if (uri != null && targetClass != null) {
            when (val res = ExcelRosterParser.parseUri(context, uri)) {
                is ExcelParseResult.Success -> {
                    parsedPreviewPair = targetClass to res.students
                }
                is ExcelParseResult.Error -> {
                    val errMsg = when (res.reason) {
                        ExcelParseErrorReason.BINARY_XLS_UNSUPPORTED -> context.getString(R.string.excel_binary_xls_error)
                        ExcelParseErrorReason.EMPTY_OR_NO_STUDENTS -> context.getString(R.string.excel_empty_error)
                        ExcelParseErrorReason.READ_FAILED -> context.getString(R.string.excel_read_error)
                    }
                    onShowError(errMsg)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Personal Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(enabled = !isSavingAvatar) {
                                    if (isGuestMode) {
                                        onGuestBlocked()
                                    } else {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileAvatarImage(
                                userId = profile.userId,
                                avatarUrl = profile.avatarUrl,
                                fullName = profile.fullName,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSavingAvatar) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.fullName,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = stringResource(
                                    id = if (profile.role == "teacher") R.string.profile_role_teacher
                                    else R.string.profile_role_student
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (profile.massarNumber.isNotBlank()) {
                                Text(
                                    text = "Massar : ${profile.massarNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (profile.birthDate.isNotBlank()) {
                                Text(
                                    text = "${stringResource(id = R.string.field_birth_date)} : ${profile.birthDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (profile.email.isNotBlank()) {
                                Text(
                                    text = profile.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (profile.phoneNumber.isNotBlank()) {
                                Text(
                                    text = profile.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (profile.establishment.isNotBlank()) {
                                Text(
                                    text = "${stringResource(id = R.string.field_establishment_label)} : ${profile.establishment}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (studentEnrollment != null) {
                                Text(
                                    text = "${stringResource(id = R.string.profile_class_label)} : ${studentEnrollment.className}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (isGuestMode) onGuestBlocked() else showEditDialog = true
                            },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(id = R.string.profile_edit)
                            )
                        }
                    }

                    if (profile.mustChangePassword) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showChangePasswordDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.password_provisional_banner),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isGuestMode) {
                                    onGuestBlocked()
                                } else if (!isSavingAvatar) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                            enabled = !isSavingAvatar,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(id = R.string.profile_change_photo))
                        }
                        OutlinedButton(
                            onClick = {
                                if (isGuestMode) onGuestBlocked() else showChangePasswordDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_password_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.password_change_button))
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(
                                id = if (isGuestMode) R.string.guest_exit_button else R.string.auth_sign_out
                            )
                        )
                    }
                }
            }
        }

        // 1a. Theme & Appearance Card
        item {
            ThemeSelectorCard(
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged
            )
        }

        // 1a-bis. 2nd Security Layer (PIN Lock) & Background Notifications Cards
        if (!isGuestMode) {
            item {
                SecurityPinSettingsCard(
                    isPinEnabled = isPinEnabled,
                    onConfigureClick = { showPinSetupDialog = true }
                )
            }

            item {
                BackgroundNotificationsSettingsCard(
                    hasPermission = hasNotifPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            if (isOwnerAdminEmail) {
                item {
                    AdminTeacherApprovalsCard(
                        currentUserId = profile.userId,
                        isAdminVerified = isAdminVerified,
                        teachers = allTeachersForAdmin,
                        onSendVerification = onSendAdminVerification,
                        onRefreshStatus = onRefreshAdminStatus,
                        onApproveTeacher = onApproveTeacher,
                        onRejectTeacher = onRejectTeacher
                    )
                }
            }
        }

        // 1b. App Updates Card (Play-policy compliant HTTPS update link + Teacher publisher)
        item {
            AppUpdatesCard(
                isTeacher = profile.role == "teacher",
                hasTeacherClasses = teacherClasses.isNotEmpty(),
                teacherClasses = teacherClasses,
                appUpdates = appUpdates,
                onOpenUpdateUrl = { url -> openAppUpdateLink(context, url) },
                onPublishClick = { showPublishUpdateDialog = true },
                onDeleteUpdate = { updateToDelete = it }
            )
        }

        // 2. Student Badges & Rewards Section
        if (profile.role == "student") {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SvtEmeraldPrimary, SvtOceanSecondary)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.badges_section_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = stringResource(
                                            id = R.string.badges_level_label,
                                            levelInfo.level,
                                            stringResource(id = levelInfo.titleRes)
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Surface(
                                    color = SvtGoldTertiary,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.badges_xp_label, profile.xp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { levelInfo.progressToNext },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = Color(0xFFFFD166),
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD166),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.badges_streak_label, profile.loginStreak),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                                val unlockedCount = badgeItems.count { it.isUnlocked }
                                Text(
                                    text = stringResource(
                                        id = R.string.badges_unlocked_count,
                                        unlockedCount,
                                        badgeItems.size
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedBadgeCategory == null,
                            onClick = { selectedBadgeCategory = null },
                            label = { Text(stringResource(id = R.string.badges_cat_all)) }
                        )
                    }
                    items(BadgeCategory.entries) { category ->
                        FilterChip(
                            selected = selectedBadgeCategory == category,
                            onClick = { selectedBadgeCategory = category },
                            label = { Text(stringResource(id = category.labelRes)) }
                        )
                    }
                }
            }

            items(filteredBadges, key = { it.definition.id }) { item ->
                SvtBadgeRowCard(item = item)
            }
        }

        // 3. Teacher Class Management, QR Codes & Student Approvals
        if (profile.role == "teacher") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.teacher_classes_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(
                        onClick = { showNewClassDialog = true },
                        modifier = Modifier.testTag("new_class_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.teacher_new_class))
                    }
                }
            }

            if (isImportingRoster) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.teacher_importing_progress),
                                style = MaterialTheme.typography.labelLarge
                            )
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            items(teacherClasses, key = { it.id }) { svtClass ->
                val classEnrollments = teacherEnrollments.filter { it.classId == svtClass.id }
                val pending = classEnrollments.filter { it.status == "pending" }
                val approved = classEnrollments.filter { it.status == "approved" }
                val classRoster = teacherRoster.filter { it.classId == svtClass.id }
                val rosterExpanded = svtClass.id in expandedRosterClassIds

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = svtClass.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${svtClass.level} • ${svtClass.academicYear}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                OutlinedButton(
                                    onClick = { qrDialogClass = svtClass },
                                    modifier = Modifier.testTag("show_qr_${svtClass.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCode2,
                                        contentDescription = stringResource(id = R.string.teacher_show_qr),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(id = R.string.teacher_show_qr))
                                }
                                IconButton(
                                    onClick = { classToDelete = svtClass },
                                    modifier = Modifier.testTag("delete_class_${svtClass.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(id = R.string.teacher_delete_class),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Excel Import, Manual Add & PDF Credentials Export Actions
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    pendingExcelTargetClass = svtClass
                                    excelPickerLauncher.launch(
                                        arrayOf(
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "application/vnd.ms-excel",
                                            "*/*"
                                        )
                                    )
                                },
                                enabled = !isImportingRoster,
                                modifier = Modifier.testTag("import_excel_${svtClass.id}")
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(id = R.string.teacher_import_excel))
                            }

                            OutlinedButton(
                                onClick = { manualStudentTargetClass = svtClass },
                                enabled = !isImportingRoster,
                                modifier = Modifier.testTag("add_manual_student_${svtClass.id}")
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(id = R.string.teacher_add_student_manual))
                            }

                            Button(
                                onClick = { onExportRosterPdf(svtClass, classRoster) },
                                enabled = classRoster.isNotEmpty(),
                                modifier = Modifier.testTag("export_pdf_${svtClass.id}")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(id = R.string.teacher_export_pdf))
                            }
                        }

                        TeacherClassLabThemesSelector(
                            svtClass = svtClass,
                            onUpdateClassLabDomains = onUpdateClassLabDomains
                        )

                        // Generated Roster & Provisional Passwords Section
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedRosterClassIds = if (rosterExpanded) {
                                                expandedRosterClassIds - svtClass.id
                                            } else {
                                                expandedRosterClassIds + svtClass.id
                                            }
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.teacher_roster_count, classRoster.size),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = if (rosterExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null
                                    )
                                }

                                if (classRoster.isEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.teacher_roster_empty),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (rosterExpanded) {
                                    classRoster.forEach { roster ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = roster.fullName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${roster.massarNumber} • ${stringResource(id = R.string.teacher_roster_password_label, roster.provisionalPassword)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteRosterStudent(roster) },
                                                modifier = Modifier.testTag("delete_roster_${roster.massarNumber}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = stringResource(id = R.string.delete),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (pending.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.teacher_pending_requests, pending.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                TextButton(
                                    onClick = { onApproveAllPending(pending) },
                                    modifier = Modifier.testTag("approve_all_${svtClass.id}")
                                ) {
                                    Text(stringResource(id = R.string.teacher_approve_all))
                                }
                            }
                            pending.forEach { enrollment ->
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = enrollment.studentName,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            val subtitleText = listOfNotNull(
                                                enrollment.massarNumber.takeIf { it.isNotBlank() },
                                                enrollment.studentPhone.takeIf { it.isNotBlank() }
                                            ).joinToString(" • ")
                                            if (subtitleText.isNotBlank()) {
                                                Text(
                                                    text = subtitleText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { onApproveStudent(enrollment) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("approve_${enrollment.studentId}")
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(stringResource(id = R.string.teacher_approve))
                                            }
                                            OutlinedButton(
                                                onClick = { onRejectStudent(enrollment) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(stringResource(id = R.string.teacher_reject))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.teacher_students_list, approved.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        approved.forEach { enrollment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(enrollment.studentName, style = MaterialTheme.typography.bodyMedium)
                                    val sub = listOfNotNull(
                                        enrollment.massarNumber.takeIf { it.isNotBlank() },
                                        enrollment.studentPhone.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ")
                                    if (sub.isNotBlank()) {
                                        Text(
                                            sub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { studentToResetDevice = enrollment },
                                        modifier = Modifier.testTag("reset_device_${enrollment.studentId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LockOpen,
                                            contentDescription = stringResource(id = R.string.device_lock_reset_button),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { studentToDelete = enrollment },
                                        modifier = Modifier.testTag("delete_student_${enrollment.studentId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = stringResource(id = R.string.teacher_delete_student),
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

        // 4. Legal Notice Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("legal_notice_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(id = R.string.legal_notice_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.legal_notice_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.legal_notice_footer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5. Privacy Policy & Data Protection Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_policy_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(id = R.string.privacy_policy_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.privacy_policy_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_privacy_policy_button")
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.privacy_policy_button))
                    }
                }
            }
        }

        // 6. Terms of Use Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("terms_of_use_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(id = R.string.terms_of_use_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.terms_of_use_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showTermsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_terms_of_use_button")
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.terms_of_use_button))
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            isStudent = profile.role == "student",
            currentName = profile.fullName,
            currentPhone = profile.phoneNumber,
            currentBirthDate = profile.birthDate,
            onSave = { name, phone, birthDate ->
                onUpdateProfile(name, phone, profile.avatarUrl, birthDate)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onSubmit = { currentPass, newPass, onDone ->
                onChangePassword(currentPass, newPass, onDone)
            },
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    parsedPreviewPair?.let { (svtClass, rows) ->
        ExcelImportPreviewDialog(
            svtClass = svtClass,
            rows = rows,
            onConfirm = {
                parsedPreviewPair = null
                expandedRosterClassIds = expandedRosterClassIds + svtClass.id
                onImportRoster(svtClass, rows)
            },
            onDismiss = { parsedPreviewPair = null }
        )
    }

    manualStudentTargetClass?.let { svtClass ->
        ManualRosterStudentDialog(
            svtClass = svtClass,
            onAdd = { name, massar ->
                manualStudentTargetClass = null
                expandedRosterClassIds = expandedRosterClassIds + svtClass.id
                onAddManualStudent(svtClass, name, massar)
            },
            onDismiss = { manualStudentTargetClass = null }
        )
    }

    if (showNewClassDialog) {
        NewClassDialog(
            onCreate = { name, level, year ->
                onCreateClass(name, level, year)
                showNewClassDialog = false
            },
            onDismiss = { showNewClassDialog = false }
        )
    }

    qrDialogClass?.let { svtClass ->
        ClassQrDialog(
            svtClass = svtClass,
            onDismiss = { qrDialogClass = null }
        )
    }

    classToDelete?.let { svtClass ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_class_msg, svtClass.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClass(svtClass)
                        classToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    studentToDelete?.let { enrollment ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_student_msg, enrollment.studentName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStudent(enrollment)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    studentToResetDevice?.let { enrollment ->
        AlertDialog(
            onDismissRequest = { studentToResetDevice = null },
            title = { Text(stringResource(id = R.string.device_lock_reset_button)) },
            text = { Text(stringResource(id = R.string.device_lock_reset_confirm, enrollment.studentName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetStudentDevice(enrollment.studentId)
                        studentToResetDevice = null
                    },
                    modifier = Modifier.testTag("confirm_reset_device_${enrollment.studentId}")
                ) {
                    Text(stringResource(id = R.string.device_lock_reset_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToResetDevice = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showPublishUpdateDialog) {
        val defaultClassId = selectedClassId ?: teacherClasses.firstOrNull()?.id.orEmpty()
        PublishAppUpdateDialog(
            teacherClasses = teacherClasses,
            initialClassId = defaultClassId,
            onPublish = { clsId, vCode, vName, notes, url ->
                onPublishAppUpdate(clsId, vCode, vName, notes, url)
                showPublishUpdateDialog = false
            },
            onDismiss = { showPublishUpdateDialog = false }
        )
    }

    updateToDelete?.let { upd ->
        AlertDialog(
            onDismissRequest = { updateToDelete = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.app_update_delete_confirm, upd.versionName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAppUpdate(upd.id)
                        updateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { updateToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showTermsDialog) {
        TermsOfUseDialog(onDismiss = { showTermsDialog = false })
    }

    if (showPinSetupDialog && !isGuestMode) {
        SetupSecurityPinDialog(
            hasExistingPin = isPinEnabled,
            userId = profile.userId,
            onSaved = { enabled ->
                isPinEnabled = enabled
                showPinSetupDialog = false
                Toast.makeText(
                    context,
                    context.getString(
                        if (enabled) R.string.security_pin_saved_msg else R.string.security_pin_removed_msg
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showPinSetupDialog = false }
        )
    }
}

@Composable
private fun SecurityPinSettingsCard(
    isPinEnabled: Boolean,
    onConfigureClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("security_pin_settings_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = if (isPinEnabled) SvtSuccess else MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.security_pin_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            id = if (isPinEnabled) R.string.security_pin_card_enabled
                            else R.string.security_pin_card_disabled
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPinEnabled) SvtSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = onConfigureClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("configure_security_pin_button")
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(
                        id = if (isPinEnabled) R.string.security_pin_manage_btn
                        else R.string.security_pin_configure_btn
                    )
                )
            }
        }
    }
}

@Composable
private fun BackgroundNotificationsSettingsCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bg_notifications_settings_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = if (hasPermission) SvtSuccess else MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.bg_notif_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            id = if (hasPermission) R.string.bg_notif_enabled_desc
                            else R.string.bg_notif_disabled_desc
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasPermission) SvtSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!hasPermission) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enable_bg_notifications_button")
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.bg_notif_enable_btn))
                }
            }
        }
    }
}

@Composable
private fun AdminTeacherApprovalsCard(
    currentUserId: String,
    isAdminVerified: Boolean,
    teachers: List<UserProfile>,
    onSendVerification: () -> Unit,
    onRefreshStatus: () -> Unit,
    onApproveTeacher: (UserProfile) -> Unit,
    onRejectTeacher: (UserProfile) -> Unit
) {
    val visibleTeachers = remember(teachers, currentUserId) {
        teachers.filter { it.userId != currentUserId }
            .sortedWith(
                compareBy<UserProfile>(
                    {
                        when (it.teacherStatus) {
                            "pending" -> 0
                            "approved" -> 1
                            else -> 2
                        }
                    },
                    { it.fullName.lowercase(Locale.getDefault()) }
                )
            )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_teacher_approvals_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.admin_teacher_approvals_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isAdminVerified) {
                Text(
                    text = stringResource(id = R.string.admin_verify_banner_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSendVerification,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.MarkEmailRead, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(id = R.string.admin_send_verification_btn), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onRefreshStatus,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(id = R.string.teacher_refresh_status_btn), maxLines = 1)
                    }
                }
            } else if (visibleTeachers.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.admin_teacher_approvals_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                visibleTeachers.forEach { teacher ->
                    val statusLabel = when (teacher.teacherStatus) {
                        "pending" -> stringResource(id = R.string.admin_teacher_status_pending)
                        "approved" -> stringResource(id = R.string.admin_teacher_status_approved)
                        else -> stringResource(id = R.string.admin_teacher_status_rejected)
                    }
                    val statusColor = when (teacher.teacherStatus) {
                        "approved" -> SvtSuccess
                        "pending" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = teacher.fullName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (teacher.establishment.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Business,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = teacher.establishment,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    val contact = listOfNotNull(
                                        teacher.email.takeIf { it.isNotBlank() },
                                        teacher.phoneNumber.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ")
                                    if (contact.isNotBlank()) {
                                        Text(
                                            text = contact,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (teacher.teacherStatus != "approved") {
                                    Button(
                                        onClick = { onApproveTeacher(teacher) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_approve_teacher_${teacher.userId}")
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(id = R.string.teacher_approve))
                                    }
                                }
                                if (teacher.teacherStatus != "rejected") {
                                    OutlinedButton(
                                        onClick = { onRejectTeacher(teacher) },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_reject_teacher_${teacher.userId}")
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(id = R.string.teacher_reject))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSelectorCard(
    currentTheme: ThemeConfig,
    onThemeChanged: (ThemeConfig) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.theme_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = stringResource(id = R.string.theme_mode_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    Triple(ThemeMode.SYSTEM, R.string.theme_mode_system, Icons.Filled.BrightnessAuto),
                    Triple(ThemeMode.LIGHT, R.string.theme_mode_light, Icons.Filled.LightMode),
                    Triple(ThemeMode.DARK, R.string.theme_mode_dark, Icons.Filled.DarkMode)
                )
                modes.forEach { (mode, labelRes, icon) ->
                    val selected = currentTheme.mode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onThemeChanged(currentTheme.copy(mode = mode)) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(
                                text = stringResource(id = labelRes),
                                maxLines = 1
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_mode_${mode.key}")
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.theme_palette_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                val palettes = listOf(
                    AppThemePalette.EMERALD to R.string.theme_palette_emerald,
                    AppThemePalette.OCEAN to R.string.theme_palette_ocean,
                    AppThemePalette.EARTH to R.string.theme_palette_earth,
                    AppThemePalette.BOTANICAL to R.string.theme_palette_botanical
                )
                palettes.forEach { (palette, labelRes) ->
                    val selected = currentTheme.palette == palette
                    val (primarySwatch, secondarySwatch) = palette.previewSwatchColors()
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                } else Modifier
                            )
                            .clickable { onThemeChanged(currentTheme.copy(palette = palette)) }
                            .testTag("theme_palette_${palette.key}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(primarySwatch)
                                        .border(1.dp, Color.White, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(secondarySwatch)
                                        .border(1.dp, Color.White, CircleShape)
                                )
                            }
                            Text(
                                text = stringResource(id = labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUpdatesCard(
    isTeacher: Boolean,
    hasTeacherClasses: Boolean,
    teacherClasses: List<SvtClass>,
    appUpdates: List<AppUpdateRelease>,
    onOpenUpdateUrl: (String) -> Unit,
    onPublishClick: () -> Unit,
    onDeleteUpdate: (AppUpdateRelease) -> Unit
) {
    val latestUpdate = appUpdates.maxByOrNull { it.versionCode }
    val hasNewerVersion = latestUpdate != null && latestUpdate.versionCode > BuildConfig.VERSION_CODE

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.app_update_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            id = R.string.app_update_current_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hasNewerVersion && latestUpdate != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.app_update_available_title,
                                latestUpdate.versionName,
                                latestUpdate.versionCode
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (latestUpdate.releaseNotes.isNotBlank()) {
                            Text(
                                text = latestUpdate.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(
                            onClick = { onOpenUpdateUrl(latestUpdate.downloadUrl) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_app_update_button")
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.app_update_download_btn))
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(id = R.string.app_update_up_to_date),
                    style = MaterialTheme.typography.bodySmall,
                    color = SvtSuccess
                )
            }

            Text(
                text = stringResource(id = R.string.app_update_no_uninstall_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isTeacher) {
                OutlinedButton(
                    onClick = onPublishClick,
                    enabled = hasTeacherClasses,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_app_update_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.app_update_publish_btn))
                }

                if (appUpdates.isNotEmpty()) {
                    appUpdates.take(5).forEach { upd ->
                        val clsName = teacherClasses.firstOrNull { it.id == upd.classId }?.name
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "v${upd.versionName} (code ${upd.versionCode})" +
                                            (clsName?.let { " • $it" } ?: ""),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (upd.releaseNotes.isNotBlank()) {
                                        Text(
                                            text = upd.releaseNotes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = { onOpenUpdateUrl(upd.downloadUrl) }) {
                                        Icon(
                                            imageVector = Icons.Filled.OpenInNew,
                                            contentDescription = stringResource(id = R.string.app_update_download_btn),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { onDeleteUpdate(upd) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = stringResource(id = R.string.delete),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
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

@Composable
private fun PublishAppUpdateDialog(
    teacherClasses: List<SvtClass>,
    initialClassId: String,
    onPublish: (classId: String, versionCode: Int, versionName: String, releaseNotes: String, downloadUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedClassId by remember { mutableStateOf(initialClassId) }
    var versionCodeText by remember { mutableStateOf((BuildConfig.VERSION_CODE + 1).toString()) }
    var versionName by remember { mutableStateOf("1.${BuildConfig.VERSION_CODE + 1}") }
    var releaseNotes by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val invalidUrlMsg = stringResource(id = R.string.app_update_invalid_url)
    val invalidVersionMsg = stringResource(id = R.string.app_update_invalid_version)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.app_update_publish_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (teacherClasses.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(teacherClasses, key = { it.id }) { cls ->
                            FilterChip(
                                selected = selectedClassId == cls.id,
                                onClick = { selectedClassId = cls.id },
                                label = { Text(cls.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = versionCodeText,
                    onValueChange = { versionCodeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(id = R.string.app_update_version_code_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text(stringResource(id = R.string.app_update_version_name_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text(stringResource(id = R.string.app_update_url_label)) },
                    placeholder = { Text("https://github.com/.../releases/latest") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true
                )
                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text(stringResource(id = R.string.app_update_notes_label)) }
                )
                if (!localError.isNullOrBlank()) {
                    Text(
                        text = localError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val vCode = versionCodeText.toIntOrNull() ?: 0
                    val cleanUrl = downloadUrl.trim()
                    when {
                        vCode < 1 || versionName.trim().isEmpty() -> localError = invalidVersionMsg
                        !cleanUrl.startsWith("https://", ignoreCase = true) || cleanUrl.length < 10 ->
                            localError = invalidUrlMsg
                        else -> {
                            localError = null
                            onPublish(selectedClassId, vCode, versionName.trim(), releaseNotes.trim(), cleanUrl)
                        }
                    }
                },
                enabled = selectedClassId.isNotBlank()
            ) {
                Text(stringResource(id = R.string.announcement_publish))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

internal fun openAppUpdateLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.app_update_invalid_url),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun SvtBadgeRowCard(item: BadgeProgressItem) {
    val def = item.definition
    val title = stringResource(id = def.titleRes)
    val desc = stringResource(id = def.descriptionRes)
    val categoryColor = when (def.category) {
        BadgeCategory.PERFORMANCE -> SvtGoldTertiary
        BadgeCategory.REGULARITY -> SvtEmeraldPrimary
        BadgeCategory.ATTENDANCE -> SvtOceanSecondary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isUnlocked) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.isUnlocked) Modifier.border(
                    1.5.dp,
                    categoryColor.copy(alpha = 0.6f),
                    MaterialTheme.shapes.medium
                ) else Modifier.alpha(0.85f)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isUnlocked) categoryColor.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIconForKey(def.iconKey),
                    contentDescription = title,
                    tint = if (item.isUnlocked) categoryColor else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = if (item.isUnlocked) SvtSuccess.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isUnlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (item.isUnlocked) SvtSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (item.isUnlocked) "+${def.xpReward} XP" else item.progressLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.isUnlocked) SvtSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!item.isUnlocked) {
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = categoryColor
                    )
                }
            }
        }
    }
}

private fun badgeIconForKey(key: String): ImageVector = when (key) {
    "science" -> Icons.Filled.Science
    "biotech" -> Icons.Filled.Biotech
    "star" -> Icons.Filled.Star
    "bolt" -> Icons.Filled.Bolt
    "local_fire" -> Icons.Filled.LocalFireDepartment
    "workspace_premium" -> Icons.Filled.WorkspacePremium
    "menu_book" -> Icons.Filled.MenuBook
    "school" -> Icons.Filled.School
    "play_circle" -> Icons.Filled.PlayCircle
    "video_library" -> Icons.Filled.VideoLibrary
    else -> Icons.Filled.EmojiEvents
}

@Composable
private fun EditProfileDialog(
    isStudent: Boolean,
    currentName: String,
    currentPhone: String,
    currentBirthDate: String,
    onSave: (String, String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }
    var birthDate by remember { mutableStateOf(currentBirthDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.profile_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.field_full_name)) },
                    singleLine = true
                )
                if (isStudent) {
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text(stringResource(id = R.string.field_birth_date)) },
                        placeholder = { Text(stringResource(id = R.string.field_birth_date_hint)) },
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(id = R.string.field_phone)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().length >= 2) {
                        onSave(name.trim(), phone.trim(), if (isStudent) birthDate.trim() else null)
                    }
                }
            ) {
                Text(stringResource(id = R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onSubmit: (currentPassword: String?, newPassword: String, onDone: (Boolean, String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val weakErr = stringResource(id = R.string.auth_error_weak_password)
    val mismatchErr = stringResource(id = R.string.auth_error_password_mismatch)

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(id = R.string.password_change_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPass,
                    onValueChange = { currentPass = it },
                    label = { Text(stringResource(id = R.string.password_current_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text(stringResource(id = R.string.password_new_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text(stringResource(id = R.string.field_password_confirm)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newPass.length < 6 -> errorMsg = weakErr
                        newPass != confirmPass -> errorMsg = mismatchErr
                        else -> {
                            errorMsg = null
                            isSaving = true
                            onSubmit(currentPass.takeIf { it.isNotBlank() }, newPass) { ok, err ->
                                isSaving = false
                                if (ok) onDismiss() else errorMsg = err
                            }
                        }
                    }
                },
                enabled = !isSaving
            ) {
                Text(stringResource(id = R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun ExcelImportPreviewDialog(
    svtClass: SvtClass,
    rows: List<ImportedStudentRow>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasGeneratedMassar = rows.any { it.generatedMassar }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.roster_preview_title, rows.size)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.roster_preview_desc, svtClass.name),
                    style = MaterialTheme.typography.bodySmall
                )
                if (hasGeneratedMassar) {
                    Text(
                        text = stringResource(id = R.string.roster_preview_auto_massar),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rows.forEach { row ->
                        Text(
                            text = "• ${row.fullName} (${row.massarNumber})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_excel_import_button")
            ) {
                Text(stringResource(id = R.string.roster_confirm_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun ManualRosterStudentDialog(
    svtClass: SvtClass,
    onAdd: (name: String, massar: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var massar by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResource(id = R.string.roster_manual_title)} — ${svtClass.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(stringResource(id = R.string.field_full_name)) },
                    singleLine = true,
                    modifier = Modifier.testTag("manual_roster_name_input")
                )
                OutlinedTextField(
                    value = massar,
                    onValueChange = { massar = it.uppercase(Locale.US) },
                    label = { Text(stringResource(id = R.string.field_massar)) },
                    placeholder = { Text(stringResource(id = R.string.field_massar_hint)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false
                    ),
                    singleLine = true,
                    modifier = Modifier.testTag("manual_roster_massar_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.trim().length >= 2 && massar.trim().length >= 4) {
                        onAdd(fullName.trim(), massar.trim())
                    }
                },
                modifier = Modifier.testTag("confirm_manual_roster_button")
            ) {
                Text(stringResource(id = R.string.roster_confirm_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun NewClassDialog(
    onCreate: (name: String, level: String, year: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("2ème Bac SVT") }
    var year by remember { mutableStateOf("2026-2027") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.teacher_new_class)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.teacher_class_name)) },
                    singleLine = true,
                    modifier = Modifier.testTag("new_class_name_input")
                )
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text(stringResource(id = R.string.teacher_class_level)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text(stringResource(id = R.string.teacher_class_year)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().length >= 2) {
                        onCreate(name.trim(), level.trim().ifBlank { "SVT" }, year.trim().ifBlank { "2026-2027" })
                    }
                },
                modifier = Modifier.testTag("confirm_create_class_button")
            ) {
                Text(stringResource(id = R.string.teacher_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun ClassQrDialog(
    svtClass: SvtClass,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(svtClass.qrToken) {
        QrCodeUtils.generateQrBitmap("BIOTERRE:${svtClass.qrToken}", 512)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${stringResource(id = R.string.teacher_qr_dialog_title)} — ${svtClass.name}",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = svtClass.qrToken,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(8.dp)
                )
                Text(
                    text = stringResource(id = R.string.teacher_qr_code_label, svtClass.qrToken),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            QrCodeUtils.copyTextToClipboard(context, svtClass.name, svtClass.qrToken)
                            Toast.makeText(
                                context,
                                context.getString(R.string.qr_code_copied, svtClass.qrToken),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_class_qr_code_button")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(id = R.string.qr_copy_code))
                    }
                    OutlinedButton(
                        onClick = {
                            val shareText = "${svtClass.name} — ${svtClass.qrToken}\nBIOTERRE:${svtClass.qrToken}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(intent, svtClass.name))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_class_qr_code_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(id = R.string.qr_share_code))
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
internal fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.privacy_policy_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.privacy_policy_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}

@Composable
internal fun TermsOfUseDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.terms_of_use_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.terms_of_use_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}
