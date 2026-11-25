package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.TeacherAnalyticsLogic
import com.example.data.model.Announcement
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.data.model.RosterStudent
import com.example.ui.components.OrderedStudentListSection
import com.example.ui.components.StudentListSortOption
import com.example.ui.components.SvtEmptyStateCard
import com.example.ui.components.SvtHeroBannerCard
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AnnouncementsTab(
    isTeacher: Boolean,
    selectedClassId: String?,
    announcements: List<Announcement>,
    onPublishAnnouncement: (classId: String, title: String, content: String) -> Unit,
    onDeleteAnnouncement: (Announcement) -> Unit = {},
    isGuestMode: Boolean = false,
    onGuestBlocked: () -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var announcementToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    val visibleAnnouncements = remember(announcements, selectedClassId, isTeacher) {
        if (isTeacher && selectedClassId != null) announcements.filter { it.classId == selectedClassId }
        else announcements
    }
    val announcementToDelete = remember(announcementToDeleteId, announcements) {
        announcementToDeleteId?.let { id -> announcements.firstOrNull { it.id == id } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SvtHeroBannerCard(
                title = stringResource(id = R.string.tab_announcements),
                subtitle = stringResource(id = R.string.app_subtitle),
                backgroundResId = R.drawable.img_tab_announcements
            )
        }
        if (isTeacher) {
            item {
                if (selectedClassId == null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text(
                            text = stringResource(id = R.string.no_class_selected),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_announcement_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.announcement_new))
                    }
                }
            }
        }

        if (visibleAnnouncements.isEmpty()) {
            item {
                SvtEmptyStateCard(title = stringResource(id = R.string.announcements_empty))
            }
        } else {
            items(visibleAnnouncements, key = { it.id }) { ann ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ann.title, style = MaterialTheme.typography.titleMedium)
                                ann.createdAt?.toDate()?.let { date ->
                                    val formatted = SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()).format(date)
                                    Text(
                                        text = formatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isTeacher) {
                                IconButton(
                                    onClick = { announcementToDeleteId = ann.id },
                                    modifier = Modifier.testTag("delete_announcement_${ann.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(id = R.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Text(
                            text = ann.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog && selectedClassId != null) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(id = R.string.announcement_new)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(id = R.string.announcement_title_label)) }
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(stringResource(id = R.string.announcement_content_label)) },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.trim().length >= 2 && content.isNotBlank()) {
                            onPublishAnnouncement(selectedClassId, title.trim(), content.trim())
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.announcement_publish))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    announcementToDelete?.let { ann ->
        AlertDialog(
            onDismissRequest = { announcementToDeleteId = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_announcement_msg, ann.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAnnouncement(ann)
                        announcementToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_announcement_button")
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { announcementToDeleteId = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun MessagingTab(
    isTeacher: Boolean,
    currentUserId: String,
    selectedClassId: String?,
    teacherEnrollments: List<Enrollment>,
    teacherRoster: List<RosterStudent> = emptyList(),
    messages: List<PrivateMessage>,
    onSendAsStudent: (String) -> Unit,
    onSendAsTeacher: (classId: String, studentId: String, studentName: String, content: String) -> Unit,
    onMarkThreadRead: (List<PrivateMessage>) -> Unit,
    onDeleteMessage: (PrivateMessage) -> Unit = {},
    isGuestMode: Boolean = false,
    onGuestBlocked: () -> Unit = {}
) {
    if (!isTeacher) {
        // Student strictly isolated private channel with their Teacher ONLY (never show delete button)
        LaunchedEffect(messages, isGuestMode) {
            if (!isGuestMode) {
                val unreadFromTeacher = messages.filter { it.senderRole == "teacher" && !it.isRead }
                if (unreadFromTeacher.isNotEmpty()) onMarkThreadRead(unreadFromTeacher)
            }
        }
        PrivateChatThread(
            headerTitle = stringResource(id = R.string.messaging_student_header),
            notice = stringResource(id = R.string.messaging_student_notice),
            messages = messages,
            currentUserId = currentUserId,
            isGuestMode = isGuestMode,
            onGuestBlocked = onGuestBlocked,
            onDeleteMessage = null,
            onSend = onSendAsStudent
        )
    } else {
        // Teacher inbox grouped strictly per student, ordered & arrow-scrollable
        var activeStudentEnrollment by remember { mutableStateOf<Enrollment?>(null) }
        var sortOption by rememberSaveable { mutableStateOf(StudentListSortOption.NAME_ASC) }

        val classStudents = remember(teacherEnrollments, teacherRoster, selectedClassId, sortOption) {
            val approved = teacherEnrollments.filter { it.status == "approved" }
            val scoped = if (selectedClassId != null) approved.filter { it.classId == selectedClassId } else approved
            when (sortOption) {
                StudentListSortOption.MASSAR_ASC -> scoped.sortedWith(
                    compareBy<Enrollment> { it.massarNumber.ifBlank { "ZZZZ" }.lowercase() }
                        .thenBy {
                            TeacherAnalyticsLogic.resolveOfficialStudentName(
                                studentId = it.studentId,
                                massarNumber = it.massarNumber,
                                classId = it.classId,
                                fallbackName = it.studentName,
                                roster = teacherRoster,
                                enrollments = teacherEnrollments
                            ).lowercase()
                        }
                )
                else -> scoped.sortedBy {
                    TeacherAnalyticsLogic.resolveOfficialStudentName(
                        studentId = it.studentId,
                        massarNumber = it.massarNumber,
                        classId = it.classId,
                        fallbackName = it.studentName,
                        roster = teacherRoster,
                        enrollments = teacherEnrollments
                    ).lowercase()
                }
            }
        }

        if (activeStudentEnrollment == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val unreadTotal = messages.count { it.senderRole == "student" && !it.isRead }
                SvtHeroBannerCard(
                    title = stringResource(id = R.string.tab_messaging),
                    subtitle = stringResource(id = R.string.messaging_teacher_inbox, unreadTotal),
                    backgroundResId = R.drawable.img_tab_messaging
                )

                if (classStudents.isEmpty()) {
                    SvtEmptyStateCard(title = stringResource(id = R.string.messaging_empty))
                } else {
                    OrderedStudentListSection(
                        items = classStudents,
                        itemKey = { _, student -> student.studentId },
                        modifier = Modifier.weight(1f),
                        maxListHeight = 1200.dp,
                        sortOption = sortOption,
                        onSortChange = { sortOption = it },
                        availableSorts = listOf(
                            StudentListSortOption.NAME_ASC,
                            StudentListSortOption.MASSAR_ASC
                        ),
                        tagPrefix = "messaging_inbox"
                    ) { _, student ->
                        val studentMsgs = messages.filter { it.studentId == student.studentId }
                        val unreadCount = studentMsgs.count { it.senderRole == "student" && !it.isRead }
                        val lastMsg = studentMsgs.lastOrNull()
                        val officialName = TeacherAnalyticsLogic.resolveOfficialStudentName(
                            studentId = student.studentId,
                            massarNumber = student.massarNumber,
                            classId = student.classId,
                            fallbackName = student.studentName,
                            roster = teacherRoster,
                            enrollments = teacherEnrollments
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeStudentEnrollment = student }
                                .testTag("messaging_thread_card_${student.studentId}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(officialName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = lastMsg?.content ?: student.className,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                if (unreadCount > 0) {
                                    Badge { Text("$unreadCount") }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val student = activeStudentEnrollment!!
            val officialName = TeacherAnalyticsLogic.resolveOfficialStudentName(
                studentId = student.studentId,
                massarNumber = student.massarNumber,
                classId = student.classId,
                fallbackName = student.studentName,
                roster = teacherRoster,
                enrollments = teacherEnrollments
            )
            BackHandler { activeStudentEnrollment = null }
            val threadMessages = remember(messages, student.studentId) {
                messages.filter { it.studentId == student.studentId }
            }
            LaunchedEffect(threadMessages) {
                val unreadFromStudent = threadMessages.filter { it.senderRole == "student" && !it.isRead }
                if (unreadFromStudent.isNotEmpty()) onMarkThreadRead(unreadFromStudent)
            }
            PrivateChatThread(
                headerTitle = officialName,
                notice = student.className,
                messages = threadMessages,
                currentUserId = currentUserId,
                onBack = { activeStudentEnrollment = null },
                onDeleteMessage = onDeleteMessage,
                onSend = { content ->
                    onSendAsTeacher(student.classId, student.studentId, officialName, content)
                }
            )
        }
    }
}

@Composable
private fun PrivateChatThread(
    headerTitle: String,
    notice: String,
    messages: List<PrivateMessage>,
    currentUserId: String,
    isGuestMode: Boolean = false,
    onGuestBlocked: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onDeleteMessage: ((PrivateMessage) -> Unit)? = null,
    onSend: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var messageToDelete by remember { mutableStateOf<PrivateMessage?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                } else {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(headerTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(notice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SvtHeroBannerCard(
                    title = stringResource(id = R.string.tab_messaging),
                    subtitle = headerTitle,
                    backgroundResId = R.drawable.img_tab_messaging,
                    heightDp = 112
                )
            }
            if (messages.isEmpty()) {
                item {
                    SvtEmptyStateCard(title = stringResource(id = R.string.messaging_empty))
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMine && onDeleteMessage != null && msg.id.isNotBlank()) {
                            IconButton(
                                onClick = { messageToDelete = msg },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_private_message_${msg.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(id = R.string.message_delete_action),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMine) 16.dp else 4.dp,
                                bottomEnd = if (isMine) 4.dp else 16.dp
                            ),
                            color = if (isMine) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 270.dp)
                        ) {
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMine) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                        if (!isMine && onDeleteMessage != null && msg.id.isNotBlank()) {
                            IconButton(
                                onClick = { messageToDelete = msg },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_private_message_${msg.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(id = R.string.message_delete_action),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (!isGuestMode) draft = it },
                    enabled = !isGuestMode,
                    placeholder = {
                        Text(
                            stringResource(
                                id = if (isGuestMode) R.string.guest_readonly_messaging_hint
                                else R.string.messaging_input_hint
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input"),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        if (isGuestMode) {
                            onGuestBlocked()
                        } else {
                            val trimmed = draft.trim()
                            if (trimmed.isNotEmpty()) {
                                onSend(trimmed)
                                draft = ""
                            }
                        }
                    },
                    enabled = isGuestMode || draft.isNotBlank(),
                    modifier = Modifier.testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(id = R.string.messaging_send),
                        tint = if (isGuestMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    messageToDelete?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text(stringResource(id = R.string.message_delete_confirm_title)) },
            text = { Text(stringResource(id = R.string.message_delete_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMessage?.invoke(msg)
                        messageToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_private_message_button")
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}
