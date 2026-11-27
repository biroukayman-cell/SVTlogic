package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.core.DocxTextExtractor
import com.example.core.TeacherAnalyticsLogic
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.RosterStudent
import com.example.data.model.VideoTracking
import com.example.data.offline.DownloadedResource
import com.example.data.offline.OfflineStorageManager
import com.example.data.offline.PickedFileTooLargeException
import com.example.data.repository.SvtRepository
import com.example.ui.components.SvtEmptyStateCard
import com.example.ui.components.SvtHeroBannerCard
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtOceanSecondary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.SvtWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private const val FILTER_OFFLINE_ONLY = "__offline__"

@Composable
fun ResourcesTab(
    isTeacher: Boolean,
    isOnline: Boolean,
    selectedClassId: String?,
    resources: List<CourseResource>,
    videoTrackings: List<VideoTracking>,
    classEnrollments: List<Enrollment>,
    teacherRoster: List<RosterStudent> = emptyList(),
    downloadedResources: Map<String, DownloadedResource>,
    downloadingProgress: Map<String, Float>,
    isUploadingResource: Boolean,
    onAddResource: (
        classId: String,
        title: String,
        desc: String,
        type: String,
        url: String,
        durationSec: Long,
        isDownloadable: Boolean
    ) -> Unit,
    onDeleteResource: (CourseResource) -> Unit,
    onPrepareResourceForOpen: (resource: CourseResource, currentUri: String, onReady: (String) -> Unit) -> Unit,
    onToggleResourceDownloadable: (CourseResource) -> Unit,
    onDownloadResourceOffline: (CourseResource) -> Unit,
    onRemoveOfflineResource: (String) -> Unit,
    onRecordVideoProgress: (
        resource: CourseResource,
        positionSec: Long,
        durationSec: Long,
        watchedPercentOverride: Double?
    ) -> Unit,
    onRecordFileOpened: (CourseResource) -> Unit = {},
    isGuestMode: Boolean = false,
    onGuestBlocked: () -> Unit = {}
) {
    val context = LocalContext.current

    var typeFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showDownloadsManagerDialog by rememberSaveable { mutableStateOf(false) }

    var pendingChoiceResourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeViewerResourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeViewerUri by rememberSaveable { mutableStateOf<String?>(null) }
    var activeViewerKind by rememberSaveable { mutableStateOf<String?>(null) }
    var savedVideoPositionMs by rememberSaveable { mutableLongStateOf(0L) }
    var savedVideoPlayWhenReady by rememberSaveable { mutableStateOf(true) }

    var trackingResourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var resourceToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    val classFiltered = remember(resources, selectedClassId, isTeacher) {
        if (isTeacher && selectedClassId != null) resources.filter { it.classId == selectedClassId }
        else resources
    }
    val visibleResources = remember(classFiltered, typeFilter, downloadedResources, searchQuery) {
        val typeFiltered = when (typeFilter) {
            null -> classFiltered
            FILTER_OFFLINE_ONLY -> classFiltered.filter { downloadedResources.containsKey(it.id) }
            else -> classFiltered.filter { it.type == typeFilter }
        }
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) typeFiltered
        else typeFiltered.filter {
            it.title.lowercase(Locale.getDefault()).contains(q) ||
                it.description.lowercase(Locale.getDefault()).contains(q) ||
                it.type.lowercase(Locale.getDefault()).contains(q)
        }
    }

    val pendingChoiceResource = remember(pendingChoiceResourceId, resources) {
        pendingChoiceResourceId?.let { id -> resources.firstOrNull { it.id == id } }
    }
    val activeViewerResource = remember(activeViewerResourceId, resources) {
        activeViewerResourceId?.let { id -> resources.firstOrNull { it.id == id } }
    }
    val trackingResource = remember(trackingResourceId, resources) {
        trackingResourceId?.let { id -> resources.firstOrNull { it.id == id } }
    }
    val resourceToDelete = remember(resourceToDeleteId, resources) {
        resourceToDeleteId?.let { id -> resources.firstOrNull { it.id == id } }
    }

    fun dismissActiveViewer() {
        activeViewerResourceId = null
        activeViewerUri = null
        activeViewerKind = null
        savedVideoPositionMs = 0L
        savedVideoPlayWhenReady = true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SvtHeroBannerCard(
                title = stringResource(id = R.string.tab_resources),
                subtitle = stringResource(id = R.string.app_subtitle),
                backgroundResId = R.drawable.img_tab_resources
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showAddDialog = true },
                            enabled = !isUploadingResource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_resource_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.resource_add))
                        }
                        if (isUploadingResource) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.resource_uploading_progress),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    if (isGuestMode) onGuestBlocked() else showDownloadsManagerDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("downloads_manager_button")
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(id = R.string.downloads_manager_button, downloadedResources.size))
            }
        }

        if (classFiltered.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(id = R.string.svt_search_resources_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resource_search_input")
                )
            }
        }

        item {
            val baseFilters = listOf(
                null to stringResource(id = R.string.resource_type_all),
                "video" to stringResource(id = R.string.resource_type_video),
                "pdf" to stringResource(id = R.string.resource_type_pdf),
                "word" to stringResource(id = R.string.resource_type_word),
                "image" to stringResource(id = R.string.resource_type_image)
            )
            val filters = baseFilters + (FILTER_OFFLINE_ONLY to stringResource(
                id = R.string.offline_filter_downloaded,
                downloadedResources.size
            ))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (key, labelText) ->
                    FilterChip(
                        selected = typeFilter == key,
                        onClick = { typeFilter = key },
                        label = { Text(labelText) },
                        modifier = Modifier.testTag("resource_filter_${key ?: "all"}")
                    )
                }
            }
        }

        if (visibleResources.isEmpty()) {
            item {
                SvtEmptyStateCard(title = stringResource(id = R.string.resources_empty))
            }
        } else {
            items(visibleResources, key = { it.id }) { resource ->
                val itemTracking = if (!isTeacher) {
                    videoTrackings.firstOrNull { it.resourceId == resource.id }
                } else null
                val downloaded = downloadedResources[resource.id]
                val downloadFraction = downloadingProgress[resource.id]
                val effectiveUri = downloaded?.localFileUri() ?: resource.fileUrl
                val isLocalUri = effectiveUri.startsWith("file://") || effectiveUri.startsWith("content://")
                val canOpenNow = isOnline || downloaded != null || isLocalUri

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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when (resource.type) {
                                            "video" -> SvtOceanSecondary.copy(alpha = 0.14f)
                                            "pdf" -> SvtCoralError.copy(alpha = 0.14f)
                                            "image" -> SvtEmeraldPrimary.copy(alpha = 0.14f)
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (resource.type) {
                                        "video" -> Icons.Filled.PlayCircle
                                        "pdf" -> Icons.Filled.PictureAsPdf
                                        "image" -> Icons.Filled.Image
                                        else -> Icons.Filled.Description
                                    },
                                    contentDescription = resource.type,
                                    tint = when (resource.type) {
                                        "video" -> SvtOceanSecondary
                                        "pdf" -> SvtCoralError
                                        "image" -> SvtEmeraldPrimary
                                        else -> MaterialTheme.colorScheme.secondary
                                    }
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(resource.title, style = MaterialTheme.typography.titleMedium)
                                if (resource.description.isNotBlank()) {
                                    Text(
                                        resource.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isTeacher) {
                                IconButton(
                                    onClick = { resourceToDeleteId = resource.id },
                                    modifier = Modifier.testTag("delete_resource_${resource.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(id = R.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        if (downloaded != null) {
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

                        if (downloadFraction != null) {
                            LinearProgressIndicator(
                                progress = { downloadFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        // Student progress / file open status
                        if (!isTeacher && resource.type == "video") {
                            val pct = (itemTracking?.watchedPercentage ?: 0.0).roundToInt().coerceIn(0, 100)
                            val statusText = when {
                                pct >= 90 -> stringResource(id = R.string.video_status_completed, pct)
                                pct > 0 -> stringResource(id = R.string.video_status_partial, pct)
                                else -> stringResource(id = R.string.video_status_not_started)
                            }
                            val statusColor = when {
                                pct >= 90 -> SvtSuccess
                                pct > 0 -> SvtWarning
                                else -> MaterialTheme.colorScheme.outline
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor
                                )
                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = statusColor
                                )
                            }
                        } else if (!isTeacher) {
                            val hasOpened = (itemTracking?.watchedPercentage ?: 0.0) >= 90.0
                            Surface(
                                color = if (hasOpened) SvtSuccess.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasOpened) Icons.Filled.CheckCircle else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = if (hasOpened) SvtSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            id = if (hasOpened) R.string.resource_file_opened_status
                                            else R.string.resource_file_not_opened_status
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasOpened) SvtSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (isTeacher && resource.type == "video") {
                            val approvedInClass = classEnrollments.filter {
                                it.classId == resource.classId && it.status == "approved"
                            }
                            val resTrackings = videoTrackings.filter { it.resourceId == resource.id }
                            val trackedByStudent = resTrackings.associateBy { it.studentId }
                            val totalStudents = maxOf(approvedInClass.size, trackedByStudent.size)
                            val watchedCount = resTrackings.count { it.watchedPercentage > 0.0 }
                            val avgPct = if (totalStudents > 0) {
                                val sumPct = if (approvedInClass.isNotEmpty()) {
                                    approvedInClass.sumOf { st ->
                                        (trackedByStudent[st.studentId]?.watchedPercentage ?: 0.0)
                                    }
                                } else {
                                    resTrackings.sumOf { it.watchedPercentage }
                                }
                                (sumPct / totalStudents.toDouble()).roundToInt().coerceIn(0, 100)
                            } else 0
                            val summaryColor = when {
                                avgPct >= 90 -> SvtSuccess
                                avgPct > 0 -> SvtOceanSecondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(
                                        id = R.string.resource_video_summary,
                                        watchedCount,
                                        totalStudents,
                                        avgPct
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = summaryColor
                                )
                                LinearProgressIndicator(
                                    progress = { (avgPct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = summaryColor
                                )
                            }
                        } else if (isTeacher) {
                            val approvedInClass = classEnrollments.filter {
                                it.classId == resource.classId && it.status == "approved"
                            }
                            val resTrackings = videoTrackings.filter { it.resourceId == resource.id }
                            val trackedByStudent = resTrackings.associateBy { it.studentId }
                            val totalStudents = maxOf(approvedInClass.size, trackedByStudent.size)
                            val openedCount = resTrackings.count { it.watchedPercentage >= 90.0 }
                            val summaryColor = if (openedCount > 0) SvtSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            Text(
                                text = stringResource(
                                    id = R.string.resource_file_summary,
                                    openedCount,
                                    totalStudents
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = summaryColor
                            )
                        }

                        if (!isTeacher && !canOpenNow) {
                            Text(
                                text = stringResource(id = R.string.offline_not_available_msg),
                                style = MaterialTheme.typography.labelSmall,
                                color = SvtWarning
                            )
                        }

                        if (isTeacher) {
                            FilterChip(
                                selected = resource.isDownloadable,
                                onClick = { onToggleResourceDownloadable(resource) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (resource.isDownloadable) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        stringResource(
                                            id = if (resource.isDownloadable) R.string.teacher_downloadable_on
                                            else R.string.teacher_downloadable_off
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("toggle_resource_downloadable_${resource.id}")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (isGuestMode) {
                                        onGuestBlocked()
                                    } else {
                                        pendingChoiceResourceId = resource.id
                                    }
                                },
                                enabled = isGuestMode || (canOpenNow && downloadFraction == null),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_resource_${resource.id}")
                            ) {
                                if (downloadFraction != null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    stringResource(
                                        id = if (resource.type == "video") R.string.resource_watch
                                        else R.string.resource_open
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (downloaded != null) {
                                IconButton(
                                    onClick = {
                                        if (isGuestMode) onGuestBlocked() else onRemoveOfflineResource(resource.id)
                                    },
                                    modifier = Modifier.testTag("remove_offline_resource_${resource.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = stringResource(id = R.string.offline_remove_download),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else if ((isTeacher || resource.isDownloadable) && downloadFraction == null) {
                                OutlinedButton(
                                    onClick = {
                                        if (isGuestMode) onGuestBlocked() else onDownloadResourceOffline(resource)
                                    },
                                    enabled = isGuestMode || isOnline || isLocalUri,
                                    modifier = Modifier.testTag("download_resource_${resource.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DownloadForOffline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        stringResource(id = R.string.offline_download_button),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (isTeacher) {
                            OutlinedButton(
                                onClick = { trackingResourceId = resource.id },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(
                                        if (resource.type == "video") "video_tracking_${resource.id}"
                                        else "file_tracking_${resource.id}"
                                    )
                            ) {
                                Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(
                                        id = if (resource.type == "video") R.string.resource_video_tracking
                                        else R.string.resource_file_tracking_button
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && selectedClassId != null) {
        AddResourceDialog(
            onSave = { title, desc, type, url, dur, isDownloadable ->
                onAddResource(selectedClassId, title, desc, type, url, dur, isDownloadable)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showDownloadsManagerDialog) {
        DownloadedFilesManagerDialog(
            downloadedResources = downloadedResources.values.sortedByDescending { it.downloadedAtMillis },
            onOpenFile = { item ->
                val targetUri = item.publicDownloadUri.ifBlank { item.localFileUri() }
                openExternalUrl(context, targetUri, item.type)
            },
            onShareFile = { item ->
                val mime = OfflineStorageManager.mimeTypeForResource(item.type, item.localFilePath)
                shareLocalFile(context, File(item.localFilePath), mime, item.title)
            },
            onDeleteFile = { item ->
                onRemoveOfflineResource(item.resourceId)
            },
            onDismiss = { showDownloadsManagerDialog = false }
        )
    }

    pendingChoiceResource?.let { res ->
        val downloaded = downloadedResources[res.id]
        val effectiveUri = downloaded?.localFileUri() ?: res.fileUrl
        ResourceOpenChoiceDialog(
            resource = res,
            onOpenInApp = {
                pendingChoiceResourceId = null
                onPrepareResourceForOpen(res, effectiveUri) { readyUri ->
                    if (res.type == "video") {
                        savedVideoPositionMs = 0L
                        savedVideoPlayWhenReady = true
                        activeViewerKind = "video"
                    } else {
                        onRecordFileOpened(res)
                        activeViewerKind = res.type
                    }
                    activeViewerResourceId = res.id
                    activeViewerUri = readyUri
                }
            },
            onOpenExternal = {
                pendingChoiceResourceId = null
                onPrepareResourceForOpen(res, effectiveUri) { readyUri ->
                    if (res.type == "video") {
                        val fallbackDur = res.durationSeconds.coerceAtLeast(1L)
                        onRecordVideoProgress(res, fallbackDur, fallbackDur, 100.0)
                    } else {
                        onRecordFileOpened(res)
                    }
                    openExternalUrl(context, readyUri, res.type)
                }
            },
            onDismiss = { pendingChoiceResourceId = null }
        )
    }

    val viewerRes = activeViewerResource
    val viewerUri = activeViewerUri
    val viewerKind = activeViewerKind
    if (viewerRes != null && !viewerUri.isNullOrBlank() && !viewerKind.isNullOrBlank()) {
        when (viewerKind) {
            "video" -> VideoPlayerTrackingDialog(
                resource = viewerRes,
                playbackUri = viewerUri,
                initialPositionMs = savedVideoPositionMs,
                initialPlayWhenReady = savedVideoPlayWhenReady,
                onSavePlaybackState = { posMs, playReady ->
                    savedVideoPositionMs = posMs
                    savedVideoPlayWhenReady = playReady
                },
                onProgressUpdate = { posSec, durSec, pctOverride ->
                    onRecordVideoProgress(viewerRes, posSec, durSec, pctOverride)
                },
                onOpenExternal = {
                    val externalTarget = if (viewerUri.startsWith("http", ignoreCase = true)) {
                        viewerUri
                    } else {
                        viewerRes.fileUrl.takeUnless { it.startsWith("firestore://") } ?: viewerUri
                    }
                    openExternalUrl(context, externalTarget, "video")
                },
                onDismiss = { dismissActiveViewer() }
            )

            "pdf" -> PdfViewerDialog(
                resource = viewerRes,
                fileUri = viewerUri,
                onOpenExternal = { openExternalUrl(context, viewerUri, viewerRes.type) },
                onDismiss = { dismissActiveViewer() }
            )

            "image" -> ImageViewerDialog(
                resource = viewerRes,
                imgUri = viewerUri,
                onOpenExternal = { openExternalUrl(context, viewerUri, viewerRes.type) },
                onDismiss = { dismissActiveViewer() }
            )

            else -> WordViewerDialog(
                resource = viewerRes,
                fileUri = viewerUri,
                onOpenExternal = { openExternalUrl(context, viewerUri, viewerRes.type) },
                onDismiss = { dismissActiveViewer() }
            )
        }
    }

    resourceToDelete?.let { res ->
        AlertDialog(
            onDismissRequest = { resourceToDeleteId = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_resource_msg, res.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteResource(res)
                        resourceToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { resourceToDeleteId = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    trackingResource?.let { res ->
        val studentsInClass = classEnrollments.filter { it.classId == res.classId && it.status == "approved" }
        val trackingsForRes = videoTrackings.filter { it.resourceId == res.id }
        if (res.type == "video") {
            TeacherVideoTrackingDialog(
                resource = res,
                students = studentsInClass,
                trackings = trackingsForRes,
                roster = teacherRoster,
                onDismiss = { trackingResourceId = null }
            )
        } else {
            TeacherFileTrackingDialog(
                resource = res,
                students = studentsInClass,
                trackings = trackingsForRes,
                roster = teacherRoster,
                onDismiss = { trackingResourceId = null }
            )
        }
    }
}

@Composable
private fun ResourceOpenChoiceDialog(
    resource: CourseResource,
    onOpenInApp: () -> Unit,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.resource_open_choice_title, resource.title),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenInApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_in_app_button")
                ) {
                    Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.resource_open_in_app))
                }
                OutlinedButton(
                    onClick = onOpenExternal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_external_app_button")
                ) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.resource_open_external_app))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun DownloadedFilesManagerDialog(
    downloadedResources: List<DownloadedResource>,
    onOpenFile: (DownloadedResource) -> Unit,
    onShareFile: (DownloadedResource) -> Unit,
    onDeleteFile: (DownloadedResource) -> Unit,
    onDismiss: () -> Unit
) {
    val totalBytes = remember(downloadedResources) {
        downloadedResources.sumOf { it.fileSizeBytes.coerceAtLeast(0L) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.downloads_manager_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(id = R.string.close))
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.downloads_manager_summary,
                            downloadedResources.size,
                            OfflineStorageManager.formatBytesReadable(totalBytes)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }

                if (downloadedResources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.downloads_manager_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(downloadedResources, key = { it.resourceId }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeLabel = OfflineStorageManager.formatBytesReadable(item.fileSizeBytes)
                                    val subtitle = buildString {
                                        append(item.type.uppercase())
                                        append(" • ")
                                        append(sizeLabel)
                                        if (item.publicDisplayPath.isNotBlank()) {
                                            append("\n")
                                            append(item.publicDisplayPath)
                                        }
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { onOpenFile(item) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(id = R.string.downloads_open_file), maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { onShareFile(item) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(id = R.string.downloads_share_file), maxLines = 1)
                                        }
                                        IconButton(onClick = { onDeleteFile(item) }) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = stringResource(id = R.string.delete),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.close))
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerTrackingDialog(
    resource: CourseResource,
    playbackUri: String,
    initialPositionMs: Long,
    initialPlayWhenReady: Boolean,
    onSavePlaybackState: (positionMs: Long, playWhenReady: Boolean) -> Unit,
    onProgressUpdate: (positionSec: Long, durationSec: Long, pctOverride: Double?) -> Unit,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var playbackError by remember(playbackUri) { mutableStateOf(false) }
    val player = remember(playbackUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(playbackUri)))
            prepare()
            if (initialPositionMs > 0L) {
                seekTo(initialPositionMs)
            }
            playWhenReady = initialPlayWhenReady
        }
    }

    var lastDispatchedPosSec by remember(playbackUri) { mutableStateOf(0L) }
    var lastDispatchedPct by remember(playbackUri) { mutableStateOf(0.0) }

    fun dispatchPlayerProgress(forceEnded: Boolean = false, forceCheckpoint: Boolean = false) {
        val rawDurMs = player.duration.takeIf { it > 0L }
        val durMs = (rawDurMs ?: (resource.durationSeconds.coerceAtLeast(1L) * 1000L)).coerceAtLeast(1000L)
        val durSec = ((durMs + 500L) / 1000L).coerceAtLeast(1L)
        val ended = forceEnded || player.playbackState == Player.STATE_ENDED
        val posMs = if (ended) durMs else player.currentPosition.coerceIn(0L, durMs)
        if (!ended && posMs <= 0L) return
        val posSec = if (ended) durSec else ((posMs + 500L) / 1000L).coerceIn(1L, durSec)
        val pctExact = if (ended) 100.0 else ((posMs.toDouble() / durMs.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        val posDelta = posSec - lastDispatchedPosSec
        val pctDelta = pctExact - lastDispatchedPct
        val shouldSync = if (ended) {
            lastDispatchedPct < 100.0 || posSec > lastDispatchedPosSec
        } else if (forceCheckpoint) {
            posSec > lastDispatchedPosSec || pctDelta >= 1.0
        } else {
            posDelta >= 15L || pctDelta >= 5.0 || (lastDispatchedPosSec == 0L && posSec >= 5L)
        }
        if (shouldSync) {
            lastDispatchedPosSec = maxOf(lastDispatchedPosSec, posSec)
            lastDispatchedPct = maxOf(lastDispatchedPct, pctExact)
            onProgressUpdate(posSec, durSec, pctExact)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    dispatchPlayerProgress(forceEnded = true, forceCheckpoint = true)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    dispatchPlayerProgress(
                        forceEnded = player.playbackState == Player.STATE_ENDED,
                        forceCheckpoint = true
                    )
                }
            }
        }
        player.addListener(listener)
        onDispose {
            onSavePlaybackState(
                player.currentPosition.coerceAtLeast(0L),
                player.playWhenReady
            )
            player.removeListener(listener)
            dispatchPlayerProgress(
                forceEnded = player.playbackState == Player.STATE_ENDED,
                forceCheckpoint = true
            )
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            delay(5000L)
            if (player.isPlaying) {
                dispatchPlayerProgress(forceEnded = false, forceCheckpoint = false)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            color = Color.Black
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (isLandscape) 12.dp else 12.dp, vertical = if (isLandscape) 4.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 10.dp)
                ) {
                    // Compact top bar in both orientations, ultra-compact in landscape
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = resource.title,
                            style = if (isLandscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val fallbackDur = (player.duration / 1000L).takeIf { it > 0L }
                                        ?: resource.durationSeconds.coerceAtLeast(1L)
                                    if (playbackError) {
                                        onProgressUpdate(fallbackDur, fallbackDur, 100.0)
                                    } else {
                                        val curPos = (player.currentPosition / 1000L).coerceIn(1L, fallbackDur)
                                        val curPct = maxOf((curPos.toDouble() / fallbackDur.toDouble()) * 100.0, 10.0)
                                            .coerceIn(0.0, 100.0)
                                        onProgressUpdate(curPos, fallbackDur, curPct)
                                    }
                                    onOpenExternal()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInNew,
                                    contentDescription = stringResource(id = R.string.pdf_open_external),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(id = R.string.close),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val playerModifier = if (isLandscape) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        }
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    this.player = player
                                    useController = true
                                }
                            },
                            update = { view ->
                                if (view.player !== player) {
                                    view.player = player
                                }
                            },
                            modifier = playerModifier.clip(RoundedCornerShape(8.dp))
                        )
                    }

                    if (playbackError) {
                        Text(
                            text = stringResource(id = R.string.video_playback_error_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (!isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val fallbackDur = (player.duration / 1000L).takeIf { it > 0L }
                                        ?: resource.durationSeconds.coerceAtLeast(1L)
                                    if (playbackError) {
                                        onProgressUpdate(fallbackDur, fallbackDur, 100.0)
                                    } else {
                                        val curPos = (player.currentPosition / 1000L).coerceIn(1L, fallbackDur)
                                        val curPct = maxOf((curPos.toDouble() / fallbackDur.toDouble()) * 100.0, 10.0)
                                            .coerceIn(0.0, 100.0)
                                        onProgressUpdate(curPos, fallbackDur, curPct)
                                    }
                                    onOpenExternal()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(id = R.string.pdf_open_external), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(onClick = onDismiss) {
                                Text(stringResource(id = R.string.close))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfViewerDialog(
    resource: CourseResource,
    fileUri: String,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by rememberSaveable(fileUri) { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderFailed by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(fileUri, currentPage) {
        scale = 1f
        offset = Offset.Zero
        renderFailed = false
        val rendered = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(fileUri)
                val pfd: ParcelFileDescriptor? = when (uri.scheme) {
                    "file" -> uri.path?.let { ParcelFileDescriptor.open(File(it), ParcelFileDescriptor.MODE_READ_ONLY) }
                    "content" -> context.contentResolver.openFileDescriptor(uri, "r")
                    else -> null
                }
                pfd?.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        val count = renderer.pageCount
                        if (count <= 0) return@runCatching null to 0
                        val safeIndex = currentPage.coerceIn(0, count - 1)
                        renderer.openPage(safeIndex).use { page ->
                            val targetW = (page.width * 2).coerceIn(400, 2200)
                            val aspect = page.height.toFloat() / page.width.coerceAtLeast(1).toFloat()
                            val targetH = (targetW * aspect).roundToInt().coerceIn(400, 3200)
                            val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(AndroidColor.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp to count
                        }
                    }
                }
            }.getOrNull()
        }
        if (rendered?.first != null) {
            pageBitmap = rendered.first
            pageCount = rendered.second
        } else {
            renderFailed = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ViewerTopBar(
                    title = resource.title,
                    scale = scale,
                    onZoomOut = {
                        val next = (scale - 0.5f).coerceIn(1f, 5f)
                        scale = next
                        if (next <= 1.01f) offset = Offset.Zero
                    },
                    onResetZoom = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    onZoomIn = {
                        scale = (scale + 0.5f).coerceIn(1f, 5f)
                    },
                    onOpenExternal = onOpenExternal,
                    onDismiss = onDismiss
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .clipToBounds()
                        .onSizeChanged { containerSize = it }
                        .pointerInput(currentPage, pageBitmap) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(currentPage, pageBitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                if (newScale <= 1.01f) {
                                    offset = Offset.Zero
                                } else {
                                    val maxX = (containerSize.width * (newScale - 1f)) / 2f
                                    val maxY = (containerSize.height * (newScale - 1f)) / 2f
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = pageBitmap
                    if (bmp != null && !renderFailed) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = resource.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        )
                    } else if (renderFailed) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.resource_no_viewer_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onOpenExternal) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(id = R.string.pdf_open_external))
                            }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }

                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pageCount > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (currentPage > 0) currentPage-- },
                                    enabled = currentPage > 0
                                ) {
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                                }
                                Text(
                                    text = stringResource(id = R.string.pdf_page_label, currentPage + 1, pageCount),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                IconButton(
                                    onClick = { if (currentPage < pageCount - 1) currentPage++ },
                                    enabled = currentPage < pageCount - 1
                                ) {
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(id = R.string.zoom_double_tap_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(id = R.string.close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageViewerDialog(
    resource: CourseResource,
    imgUri: String,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ViewerTopBar(
                    title = resource.title,
                    scale = scale,
                    darkSurface = true,
                    onZoomOut = {
                        val next = (scale - 0.5f).coerceIn(1f, 5f)
                        scale = next
                        if (next <= 1.01f) offset = Offset.Zero
                    },
                    onResetZoom = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    onZoomIn = {
                        scale = (scale + 0.5f).coerceIn(1f, 5f)
                    },
                    onOpenExternal = onOpenExternal,
                    onDismiss = onDismiss
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .onSizeChanged { containerSize = it }
                        .pointerInput(imgUri) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(imgUri) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                if (newScale <= 1.01f) {
                                    offset = Offset.Zero
                                } else {
                                    val maxX = (containerSize.width * (newScale - 1f)) / 2f
                                    val maxY = (containerSize.height * (newScale - 1f)) / 2f
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imgUri,
                        contentDescription = resource.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.zoom_double_tap_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.close), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordViewerDialog(
    resource: CourseResource,
    fileUri: String,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var paragraphs by remember { mutableStateOf<List<String>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var textScale by rememberSaveable(fileUri) { mutableFloatStateOf(1f) }

    LaunchedEffect(fileUri) {
        isLoading = true
        val extracted = withContext(Dispatchers.IO) {
            val uri = Uri.parse(fileUri)
            when (uri.scheme) {
                "file" -> uri.path?.let { DocxTextExtractor.extractFromFile(File(it)) }
                "content" -> runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        DocxTextExtractor.extractFromStream(stream)
                    }
                }.getOrNull()
                else -> null
            }
        }
        paragraphs = extracted?.paragraphs
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ViewerTopBar(
                    title = resource.title,
                    scale = textScale,
                    onZoomOut = { textScale = (textScale - 0.2f).coerceIn(0.8f, 2.5f) },
                    onResetZoom = { textScale = 1f },
                    onZoomIn = { textScale = (textScale + 0.2f).coerceIn(0.8f, 2.5f) },
                    onOpenExternal = onOpenExternal,
                    onDismiss = onDismiss
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(fileUri) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom != 1f) {
                                    textScale = (textScale * zoom).coerceIn(0.8f, 2.5f)
                                }
                            }
                        }
                ) {
                    val lines = paragraphs
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        lines.isNullOrEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.word_empty_or_unsupported),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = onOpenExternal) {
                                    Icon(Icons.Filled.OpenInNew, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(id = R.string.resource_open_external_app))
                                }
                            }
                        }
                        else -> {
                            SelectionContainer {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val scaledFont = (15f * textScale).sp
                                    val scaledLineHeight = (22f * textScale).sp
                                    lines.forEach { para ->
                                        Text(
                                            text = para,
                                            fontSize = scaledFont,
                                            lineHeight = scaledLineHeight,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onOpenExternal) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.pdf_open_external), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(id = R.string.close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerTopBar(
    title: String,
    scale: Float,
    darkSurface: Boolean = false,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onZoomIn: () -> Unit,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    val contentColor = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(
        color = if (darkSurface) Color.Black.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (darkSurface) 0.dp else 3.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                )
                IconButton(onClick = onZoomOut) {
                    Icon(
                        imageVector = Icons.Filled.ZoomOut,
                        contentDescription = stringResource(id = R.string.zoom_out),
                        tint = contentColor
                    )
                }
                TextButton(onClick = onResetZoom) {
                    Text(
                        text = "${(scale * 100).roundToInt()}%",
                        color = contentColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                IconButton(onClick = onZoomIn) {
                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = stringResource(id = R.string.zoom_in),
                        tint = contentColor
                    )
                }
                IconButton(onClick = onOpenExternal) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = stringResource(id = R.string.pdf_open_external),
                        tint = contentColor
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.close),
                        tint = contentColor
                    )
                }
            }
            if (!darkSurface) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TeacherVideoTrackingDialog(
    resource: CourseResource,
    students: List<Enrollment>,
    trackings: List<VideoTracking>,
    roster: List<RosterStudent>,
    onDismiss: () -> Unit
) {
    val trackingByStudentId = trackings.associateBy { it.studentId }
    val enrolledIds = students.map { it.studentId }.toSet()
    val extraRows = trackings
        .filterNot { enrolledIds.contains(it.studentId) }
        .map { tr ->
            val officialName = TeacherAnalyticsLogic.resolveOfficialStudentName(
                studentId = tr.studentId,
                classId = resource.classId,
                fallbackName = tr.studentName,
                roster = roster,
                enrollments = students
            )
            tr.studentId to officialName
        }
    val displayRows = students.map { enr ->
        val officialName = TeacherAnalyticsLogic.resolveOfficialStudentName(
            studentId = enr.studentId,
            massarNumber = enr.massarNumber,
            classId = resource.classId,
            fallbackName = enr.studentName,
            roster = roster,
            enrollments = students
        )
        enr.studentId to officialName
    } + extraRows

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResource(id = R.string.resource_video_tracking)} — ${resource.title}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayRows.forEach { (studentId, studentName) ->
                    val track = trackingByStudentId[studentId]
                    val pct = (track?.watchedPercentage ?: 0.0).roundToInt().coerceIn(0, 100)
                    val statusLabel = when {
                        pct >= 90 -> stringResource(id = R.string.video_status_completed, pct)
                        pct > 0 -> stringResource(id = R.string.video_status_partial, pct)
                        else -> stringResource(id = R.string.video_status_not_started)
                    }
                    val color = when {
                        pct >= 90 -> SvtSuccess
                        pct > 0 -> SvtWarning
                        else -> SvtCoralError
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = studentName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = color)
                            }
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = color
                            )
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
private fun TeacherFileTrackingDialog(
    resource: CourseResource,
    students: List<Enrollment>,
    trackings: List<VideoTracking>,
    roster: List<RosterStudent>,
    onDismiss: () -> Unit
) {
    val trackingByStudentId = trackings.associateBy { it.studentId }
    val enrolledIds = students.map { it.studentId }.toSet()
    val extraRows = trackings
        .filterNot { enrolledIds.contains(it.studentId) }
        .map { tr ->
            val officialName = TeacherAnalyticsLogic.resolveOfficialStudentName(
                studentId = tr.studentId,
                classId = resource.classId,
                fallbackName = tr.studentName,
                roster = roster,
                enrollments = students
            )
            tr.studentId to officialName
        }
    val displayRows = students.map { enr ->
        val officialName = TeacherAnalyticsLogic.resolveOfficialStudentName(
            studentId = enr.studentId,
            massarNumber = enr.massarNumber,
            classId = resource.classId,
            fallbackName = enr.studentName,
            roster = roster,
            enrollments = students
        )
        enr.studentId to officialName
    } + extraRows

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResource(id = R.string.resource_file_tracking_button)} — ${resource.title}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayRows.forEach { (studentId, studentName) ->
                    val track = trackingByStudentId[studentId]
                    val opened = (track?.watchedPercentage ?: 0.0) >= 90.0
                    val statusLabel = stringResource(
                        id = if (opened) R.string.resource_file_opened_status
                        else R.string.resource_file_not_opened_status
                    )
                    val color = if (opened) SvtSuccess else SvtCoralError
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = studentName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = color)
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
private fun AddResourceDialog(
    onSave: (
        title: String,
        desc: String,
        type: String,
        url: String,
        durationSec: Long,
        isDownloadable: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("pdf") }
    var fileUrl by remember { mutableStateOf("") }
    var pickedFileLabel by remember { mutableStateOf<String?>(null) }
    var fileSizeError by remember { mutableStateOf<String?>(null) }
    var durationText by remember { mutableStateOf("120") }
    var isDownloadable by remember { mutableStateOf(true) }
    var isStagingFile by remember { mutableStateOf(false) }

    val handlePickedUri: (Uri) -> Unit = { uri ->
        scope.launch {
            isStagingFile = true
            fileSizeError = null
            try {
                val staged = OfflineStorageManager.stagePickedUriToCache(
                    context = context,
                    uri = uri,
                    typeHint = selectedType,
                    maxBytes = SvtRepository.MAX_CHUNKED_UPLOAD_BYTES
                )
                fileUrl = staged.localFileUri
                val sizeText = OfflineStorageManager.formatBytesReadable(staged.sizeBytes)
                pickedFileLabel = "${staged.displayName} • $sizeText"
                if (title.isBlank() && staged.displayName.isNotBlank()) {
                    title = staged.displayName.substringBeforeLast('.').take(80)
                }
            } catch (_: PickedFileTooLargeException) {
                val maxMb = (SvtRepository.MAX_CHUNKED_UPLOAD_BYTES / (1024L * 1024L)).toInt()
                fileSizeError = context.getString(R.string.resource_too_large_error, maxMb)
                pickedFileLabel = null
                fileUrl = ""
            } catch (_: Exception) {
                fileSizeError = context.getString(R.string.resource_read_error)
                pickedFileLabel = null
                fileUrl = ""
            } finally {
                isStagingFile = false
            }
        }
    }

    val visualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(handlePickedUri)
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            handlePickedUri(it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.resource_add)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "pdf" to R.string.resource_type_pdf,
                        "word" to R.string.resource_type_word,
                        "image" to R.string.resource_type_image,
                        "video" to R.string.resource_type_video
                    ).forEach { (typeKey, resId) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = { selectedType = typeKey },
                            label = { Text(stringResource(id = resId)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.resource_title_label)) }
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(stringResource(id = R.string.resource_desc_label)) }
                )
                OutlinedTextField(
                    value = fileUrl,
                    onValueChange = {
                        fileUrl = it
                        pickedFileLabel = null
                        fileSizeError = null
                    },
                    label = { Text(stringResource(id = R.string.resource_url_label)) }
                )
                OutlinedButton(
                    onClick = {
                        when (selectedType) {
                            "image" -> visualMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                            "video" -> visualMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                            else -> documentPickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                )
                            )
                        }
                    },
                    enabled = !isStagingFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.resource_pick_file))
                }
                if (isStagingFile) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (!pickedFileLabel.isNullOrBlank()) {
                    Text(
                        text = pickedFileLabel!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!fileSizeError.isNullOrBlank()) {
                    Text(
                        text = fileSizeError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (selectedType == "video") {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(id = R.string.resource_duration_label)) },
                        singleLine = true
                    )
                }
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
                        onCheckedChange = { isDownloadable = it },
                        modifier = Modifier.testTag("add_resource_downloadable_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isStagingFile && title.trim().length >= 2 && fileUrl.isNotBlank() && fileSizeError == null) {
                        onSave(
                            title.trim(),
                            desc.trim(),
                            selectedType,
                            fileUrl.trim(),
                            durationText.toLongOrNull() ?: 0L,
                            isDownloadable
                        )
                    }
                },
                enabled = !isStagingFile && title.trim().length >= 2 && fileUrl.isNotBlank() && fileSizeError == null
            ) {
                Text(stringResource(id = R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

private fun openExternalUrl(context: Context, url: String, resourceType: String) {
    try {
        val normalized = OfflineStorageManager.normalizeDownloadUrl(url)
        val parsed = Uri.parse(normalized)
        val isLocalFile = parsed.scheme == "file" && parsed.path != null
        val finalUri = if (isLocalFile) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(parsed.path!!)
            )
        } else {
            parsed
        }
        val intent = if (isLocalFile || parsed.scheme == "content") {
            val mime = OfflineStorageManager.mimeTypeForResource(resourceType, normalized)
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(finalUri, mime)
                clipData = ClipData.newRawUri("", finalUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_VIEW, finalUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val chooser = Intent.createChooser(
            intent,
            context.getString(R.string.resource_open_external_app)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(R.string.resource_no_viewer_error),
            Toast.LENGTH_SHORT
        ).show()
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.resource_no_viewer_error),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun shareLocalFile(context: Context, file: File, mimeType: String, title: String) {
    try {
        if (!file.exists()) {
            Toast.makeText(context, context.getString(R.string.resource_read_error), Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri(title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.resource_no_viewer_error),
            Toast.LENGTH_SHORT
        ).show()
    }
}
