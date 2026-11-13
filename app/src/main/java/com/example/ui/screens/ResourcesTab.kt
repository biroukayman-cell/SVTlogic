package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.VideoTracking
import com.example.data.offline.DownloadedResource
import com.example.ui.components.SvtEmptyStateCard
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtOceanSecondary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.SvtWarning
import kotlinx.coroutines.delay
import java.io.File

private const val FILTER_OFFLINE_ONLY = "__offline__"

@Composable
fun ResourcesTab(
    isTeacher: Boolean,
    isOnline: Boolean,
    selectedClassId: String?,
    resources: List<CourseResource>,
    videoTrackings: List<VideoTracking>,
    classEnrollments: List<Enrollment>,
    downloadedResources: Map<String, DownloadedResource>,
    downloadingProgress: Map<String, Float>,
    onAddResource: (
        classId: String,
        title: String,
        desc: String,
        type: String,
        url: String,
        durationSec: Long,
        isDownloadable: Boolean
    ) -> Unit,
    onToggleResourceDownloadable: (CourseResource) -> Unit,
    onDownloadResourceOffline: (CourseResource) -> Unit,
    onRemoveOfflineResource: (String) -> Unit,
    onRecordVideoProgress: (resource: CourseResource, positionSec: Long, durationSec: Long) -> Unit
) {
    val context = LocalContext.current
    var typeFilter by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var videoToPlay by remember { mutableStateOf<Pair<CourseResource, String>?>(null) }
    var pdfToView by remember { mutableStateOf<Pair<CourseResource, String>?>(null) }
    var imageToView by remember { mutableStateOf<Pair<CourseResource, String>?>(null) }
    var trackingVideoResource by remember { mutableStateOf<CourseResource?>(null) }

    val classFiltered = remember(resources, selectedClassId, isTeacher) {
        if (isTeacher && selectedClassId != null) resources.filter { it.classId == selectedClassId }
        else resources
    }
    val visibleResources = remember(classFiltered, typeFilter, downloadedResources) {
        when (typeFilter) {
            null -> classFiltered
            FILTER_OFFLINE_ONLY -> classFiltered.filter { downloadedResources.containsKey(it.id) }
            else -> classFiltered.filter { it.type == typeFilter }
        }
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
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_resource_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.resource_add))
                    }
                }
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
            val filters = if (!isTeacher) {
                baseFilters + (FILTER_OFFLINE_ONLY to stringResource(
                    id = R.string.offline_filter_downloaded,
                    downloadedResources.size
                ))
            } else baseFilters

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
                val studentTracking = if (!isTeacher && resource.type == "video") {
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
                        }

                        if (!isTeacher && downloaded != null) {
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

                        if (!isTeacher && resource.type == "video") {
                            val pct = (studentTracking?.watchedPercentage ?: 0.0).toInt()
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
                                    when (resource.type) {
                                        "video" -> videoToPlay = resource to effectiveUri
                                        "pdf" -> pdfToView = resource to effectiveUri
                                        "image" -> imageToView = resource to effectiveUri
                                        else -> openExternalUrl(context, effectiveUri)
                                    }
                                },
                                enabled = canOpenNow,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    stringResource(
                                        id = if (resource.type == "video") R.string.resource_watch
                                        else R.string.resource_open
                                    )
                                )
                            }

                            if (!isTeacher) {
                                if (downloaded != null) {
                                    IconButton(
                                        onClick = { onRemoveOfflineResource(resource.id) },
                                        modifier = Modifier.testTag("remove_offline_resource_${resource.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = stringResource(id = R.string.offline_remove_download),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else if (resource.isDownloadable && downloadFraction == null) {
                                    OutlinedButton(
                                        onClick = { onDownloadResourceOffline(resource) },
                                        enabled = isOnline,
                                        modifier = Modifier.testTag("download_resource_${resource.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DownloadForOffline,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(id = R.string.offline_download_button))
                                    }
                                }
                            } else if (resource.type == "video") {
                                OutlinedButton(
                                    onClick = { trackingVideoResource = resource },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("video_tracking_${resource.id}")
                                ) {
                                    Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(id = R.string.resource_video_tracking))
                                }
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

    videoToPlay?.let { (res, playbackUri) ->
        VideoPlayerTrackingDialog(
            resource = res,
            playbackUri = playbackUri,
            onProgressUpdate = { posSec, durSec ->
                onRecordVideoProgress(res, posSec, durSec)
            },
            onDismiss = { videoToPlay = null }
        )
    }

    pdfToView?.let { (res, fileUri) ->
        PdfViewerDialog(
            resource = res,
            fileUri = fileUri,
            onOpenExternal = { openExternalUrl(context, fileUri) },
            onDismiss = { pdfToView = null }
        )
    }

    imageToView?.let { (res, imgUri) ->
        AlertDialog(
            onDismissRequest = { imageToView = null },
            title = { Text(res.title) },
            text = {
                AsyncImage(
                    model = imgUri,
                    contentDescription = res.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = {
                Button(onClick = { imageToView = null }) { Text(stringResource(id = R.string.close)) }
            }
        )
    }

    trackingVideoResource?.let { res ->
        val studentsInClass = classEnrollments.filter { it.classId == res.classId && it.status == "approved" }
        val trackingsForVideo = videoTrackings.filter { it.resourceId == res.id }
        TeacherVideoTrackingDialog(
            resource = res,
            students = studentsInClass,
            trackings = trackingsForVideo,
            onDismiss = { trackingVideoResource = null }
        )
    }
}

@Composable
private fun VideoPlayerTrackingDialog(
    resource: CourseResource,
    playbackUri: String,
    onProgressUpdate: (positionSec: Long, durationSec: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(playbackUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(playbackUri)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose {
            val pos = (player.currentPosition / 1000L).coerceAtLeast(0L)
            val dur = (player.duration / 1000L).takeIf { it > 0L } ?: resource.durationSeconds.coerceAtLeast(1L)
            onProgressUpdate(pos, dur)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            delay(3000L)
            if (player.isPlaying) {
                val pos = (player.currentPosition / 1000L).coerceAtLeast(0L)
                val dur = (player.duration / 1000L).takeIf { it > 0L } ?: resource.durationSeconds.coerceAtLeast(1L)
                onProgressUpdate(pos, dur)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resource.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(id = R.string.close)) }
        }
    )
}

@Composable
private fun PdfViewerDialog(
    resource: CourseResource,
    fileUri: String,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderFailed by remember { mutableStateOf(false) }

    LaunchedEffect(fileUri, currentPage) {
        try {
            val uri = Uri.parse(fileUri)
            if (uri.scheme == "content" || uri.scheme == "file") {
                val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
                pfd?.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        pageCount = renderer.pageCount
                        if (pageCount > 0) {
                            val safeIndex = currentPage.coerceIn(0, pageCount - 1)
                            renderer.openPage(safeIndex).use { page ->
                                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                pageBitmap = bmp
                            }
                        }
                    }
                }
            } else {
                renderFailed = true
            }
        } catch (_: Exception) {
            renderFailed = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resource.title) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (pageBitmap != null && !renderFailed) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = resource.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.White)
                    )
                    if (pageCount > 1) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0
                            ) { Text("<") }
                            Text(stringResource(id = R.string.pdf_page_label, currentPage + 1, pageCount))
                            TextButton(
                                onClick = { if (currentPage < pageCount - 1) currentPage++ },
                                enabled = currentPage < pageCount - 1
                            ) { Text(">") }
                        }
                    }
                } else {
                    Text(
                        text = fileUri,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onOpenExternal) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.pdf_open_external))
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
private fun TeacherVideoTrackingDialog(
    resource: CourseResource,
    students: List<Enrollment>,
    trackings: List<VideoTracking>,
    onDismiss: () -> Unit
) {
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
                students.forEach { student ->
                    val track = trackings.firstOrNull { it.studentId == student.studentId }
                    val pct = (track?.watchedPercentage ?: 0.0).toInt()
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(student.studentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = color)
                            }
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
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
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("pdf") }
    var fileUrl by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("120") }
    var isDownloadable by remember { mutableStateOf(true) }

    val visualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { fileUrl = it.toString() }
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
            fileUrl = it.toString()
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
                    onValueChange = { fileUrl = it },
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.resource_pick_file))
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
                    if (title.trim().length >= 2 && fileUrl.isNotBlank()) {
                        onSave(
                            title.trim(),
                            desc.trim(),
                            selectedType,
                            fileUrl.trim(),
                            durationText.toLongOrNull() ?: 0L,
                            isDownloadable
                        )
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

private fun openExternalUrl(context: Context, url: String) {
    try {
        val parsed = Uri.parse(url)
        val finalUri = if (parsed.scheme == "file" && parsed.path != null) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(parsed.path!!)
            )
        } else {
            parsed
        }
        val intent = Intent(Intent.ACTION_VIEW, finalUri).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
