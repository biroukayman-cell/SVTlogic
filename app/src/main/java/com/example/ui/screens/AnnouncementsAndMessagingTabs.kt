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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.ui.components.SvtEmptyStateCard
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AnnouncementsTab(
    isTeacher: Boolean,
    selectedClassId: String?,
    announcements: List<Announcement>,
    onPublishAnnouncement: (classId: String, title: String, content: String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    val visibleAnnouncements = remember(announcements, selectedClassId, isTeacher) {
        if (isTeacher && selectedClassId != null) announcements.filter { it.classId == selectedClassId }
        else announcements
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
}

@Composable
fun MessagingTab(
    isTeacher: Boolean,
    currentUserId: String,
    selectedClassId: String?,
    teacherEnrollments: List<Enrollment>,
    messages: List<PrivateMessage>,
    onSendAsStudent: (String) -> Unit,
    onSendAsTeacher: (classId: String, studentId: String, studentName: String, content: String) -> Unit,
    onMarkThreadRead: (List<PrivateMessage>) -> Unit
) {
    if (!isTeacher) {
        // Student strictly isolated private channel with their Teacher ONLY
        LaunchedEffect(messages) {
            val unreadFromTeacher = messages.filter { it.senderRole == "teacher" && !it.isRead }
            if (unreadFromTeacher.isNotEmpty()) onMarkThreadRead(unreadFromTeacher)
        }
        PrivateChatThread(
            headerTitle = stringResource(id = R.string.messaging_student_header),
            notice = stringResource(id = R.string.messaging_student_notice),
            messages = messages,
            currentUserId = currentUserId,
            onSend = onSendAsStudent
        )
    } else {
        // Teacher inbox grouped strictly per student
        var activeStudentEnrollment by remember { mutableStateOf<Enrollment?>(null) }
        val classStudents = remember(teacherEnrollments, selectedClassId) {
            val approved = teacherEnrollments.filter { it.status == "approved" }
            if (selectedClassId != null) approved.filter { it.classId == selectedClassId } else approved
        }

        if (activeStudentEnrollment == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val unreadTotal = messages.count { it.senderRole == "student" && !it.isRead }
                    Text(
                        text = stringResource(id = R.string.messaging_teacher_inbox, unreadTotal),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (classStudents.isEmpty()) {
                    item {
                        SvtEmptyStateCard(title = stringResource(id = R.string.messaging_empty))
                    }
                } else {
                    items(classStudents, key = { it.studentId }) { student ->
                        val studentMsgs = messages.filter { it.studentId == student.studentId }
                        val unreadCount = studentMsgs.count { it.senderRole == "student" && !it.isRead }
                        val lastMsg = studentMsgs.lastOrNull()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeStudentEnrollment = student },
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
                                    Text(student.studentName, style = MaterialTheme.typography.titleMedium)
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
            BackHandler { activeStudentEnrollment = null }
            val threadMessages = remember(messages, student.studentId) {
                messages.filter { it.studentId == student.studentId }
            }
            LaunchedEffect(threadMessages) {
                val unreadFromStudent = threadMessages.filter { it.senderRole == "student" && !it.isRead }
                if (unreadFromStudent.isNotEmpty()) onMarkThreadRead(unreadFromStudent)
            }
            PrivateChatThread(
                headerTitle = student.studentName,
                notice = student.className,
                messages = threadMessages,
                currentUserId = currentUserId,
                onBack = { activeStudentEnrollment = null },
                onSend = { content ->
                    onSendAsTeacher(student.classId, student.studentId, student.studentName, content)
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
    onBack: (() -> Unit)? = null,
    onSend: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

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
                    IconButton(onClick = onBack) {
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
            if (messages.isEmpty()) {
                item {
                    SvtEmptyStateCard(title = stringResource(id = R.string.messaging_empty))
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMine) 16.dp else 4.dp,
                                bottomEnd = if (isMine) 4.dp else 16.dp
                            ),
                            color = if (isMine) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMine) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
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
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(id = R.string.messaging_input_hint)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input"),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        val trimmed = draft.trim()
                        if (trimmed.isNotEmpty()) {
                            onSend(trimmed)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(id = R.string.messaging_send),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
