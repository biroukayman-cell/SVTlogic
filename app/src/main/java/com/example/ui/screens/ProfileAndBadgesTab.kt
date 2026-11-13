package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.QrCodeUtils
import com.example.data.badges.BadgeCategory
import com.example.data.badges.BadgeEngine
import com.example.data.badges.BadgeProgressItem
import com.example.data.model.Enrollment
import com.example.data.model.QuizAttempt
import com.example.data.model.SvtClass
import com.example.data.model.UserProfile
import com.example.data.model.VideoTracking
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtGoldTertiary
import com.example.ui.theme.SvtOceanSecondary
import com.example.ui.theme.SvtSuccess

@Composable
fun ProfileAndBadgesTab(
    profile: UserProfile,
    studentEnrollment: Enrollment?,
    studentAttempts: List<QuizAttempt>,
    studentVideoTrackings: List<VideoTracking>,
    teacherClasses: List<SvtClass>,
    teacherEnrollments: List<Enrollment>,
    onUpdateProfile: (fullName: String, phone: String, avatarUrl: String?) -> Unit,
    onCreateClass: (name: String, level: String, year: String) -> Unit,
    onDeleteClass: (SvtClass) -> Unit,
    onApproveStudent: (Enrollment) -> Unit,
    onRejectStudent: (Enrollment) -> Unit,
    onDeleteStudent: (Enrollment) -> Unit,
    onSignOut: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showNewClassDialog by remember { mutableStateOf(false) }
    var qrDialogClass by remember { mutableStateOf<SvtClass?>(null) }
    var classToDelete by remember { mutableStateOf<SvtClass?>(null) }
    var studentToDelete by remember { mutableStateOf<Enrollment?>(null) }
    var selectedBadgeCategory by remember { mutableStateOf<BadgeCategory?>(null) }

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
            onUpdateProfile(profile.fullName, profile.phoneNumber, it.toString())
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
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profile.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = profile.avatarUrl,
                                    contentDescription = profile.fullName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = profile.fullName,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
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
                            if (profile.phoneNumber.isNotBlank()) {
                                Text(
                                    text = profile.phoneNumber,
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
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(id = R.string.profile_edit)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(id = R.string.profile_change_photo))
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.auth_sign_out))
                        }
                    }
                }
            }
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

            items(teacherClasses, key = { it.id }) { svtClass ->
                val classEnrollments = teacherEnrollments.filter { it.classId == svtClass.id }
                val pending = classEnrollments.filter { it.status == "pending" }
                val approved = classEnrollments.filter { it.status == "approved" }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    text = svtClass.name,
                                    style = MaterialTheme.typography.titleMedium
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

                        if (pending.isNotEmpty()) {
                            Text(
                                text = stringResource(id = R.string.teacher_pending_requests, pending.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
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
                                            Text(
                                                text = enrollment.studentPhone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
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
                                    Text(
                                        enrollment.studentPhone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showEditDialog) {
        EditProfileDialog(
            currentName = profile.fullName,
            currentPhone = profile.phoneNumber,
            onSave = { name, phone ->
                onUpdateProfile(name, phone, profile.avatarUrl)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
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
    currentName: String,
    currentPhone: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }

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
                onClick = { if (name.trim().length >= 2) onSave(name.trim(), phone.trim()) }
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
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(id = R.string.close)) }
        }
    )
}
