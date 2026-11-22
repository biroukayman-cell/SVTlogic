package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.NotificationDrawerLogic
import com.example.core.NotificationPriority
import com.example.core.NotificationSortMode
import com.example.data.badges.SvtBadgeDefinition
import com.example.data.model.StudentNotification
import com.example.data.svt.SvtDomain
import com.example.data.svt.SvtKnowledgeCatalog
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtOceanSecondary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SvtWallpaperContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_bg_app),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (isDark) 0.16f else 0.94f,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    MaterialTheme.colorScheme.background.copy(
                        alpha = if (isDark) 0.52f else 0.14f
                    )
                )
        )
        content()
    }
}

@Composable
fun LanguageSwitcherRow(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLanguage.entries.forEach { lang ->
            val selected = lang == currentLanguage
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                    .testTag("lang_chip_${lang.code}")
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onLanguageSelected(lang) }
                    .minimumInteractiveComponentSize()
            ) {
                Text(
                    text = lang.nativeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun SvtEmptyStateCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_svt_classroom),
                contentDescription = title,
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BadgeCelebrationDialog(
    badges: List<SvtBadgeDefinition>,
    onDismiss: () -> Unit
) {
    if (badges.isEmpty()) return
    val first = badges.first()
    val badgeTitle = stringResource(id = first.titleRes)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(id = R.drawable.img_svt_badge_trophy),
                contentDescription = badgeTitle,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.badge_unlocked_banner_title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    badges.forEach { badge ->
                        val title = stringResource(id = badge.titleRes)
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = title,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "+${badge.xpReward} XP",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("badge_celebration_close_button")
            ) {
                Text(stringResource(id = R.string.badge_celebration_close))
            }
        }
    )
}

@Composable
fun NotificationsSheet(
    notifications: List<StudentNotification>,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit,
    newlyArrivedIds: Set<String> = emptySet(),
    onAcknowledgeNewNotifications: (Set<String>) -> Unit = {}
) {
    NotificationDrawer(
        notifications = notifications,
        onMarkAllRead = onMarkAllRead,
        onDismiss = onDismiss,
        newlyArrivedIds = newlyArrivedIds,
        onAcknowledgeNewNotifications = onAcknowledgeNewNotifications
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDrawer(
    notifications: List<StudentNotification>,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit,
    newlyArrivedIds: Set<String> = emptySet(),
    onAcknowledgeNewNotifications: (Set<String>) -> Unit = {}
) {
    var sortMode by rememberSaveable { mutableStateOf(NotificationSortMode.DATE) }
    var knownOpenIds by remember {
        mutableStateOf(notifications.map { it.id }.toSet())
    }
    var highlightedIds by remember { mutableStateOf(newlyArrivedIds) }

    LaunchedEffect(newlyArrivedIds) {
        if (newlyArrivedIds.isNotEmpty()) {
            highlightedIds = highlightedIds + newlyArrivedIds
        }
    }

    LaunchedEffect(notifications) {
        val arrivedNow = NotificationDrawerLogic.detectNewlyArrivedNotificationIds(
            knownIds = knownOpenIds,
            currentNotifications = notifications
        )
        knownOpenIds = notifications.map { it.id }.toSet()
        if (arrivedNow.isNotEmpty()) {
            highlightedIds = highlightedIds + arrivedNow
        }
    }

    LaunchedEffect(highlightedIds) {
        if (highlightedIds.isNotEmpty()) {
            val activeBatch = highlightedIds
            delay(3_600L)
            highlightedIds = highlightedIds - activeBatch
            onAcknowledgeNewNotifications(activeBatch)
        }
    }

    val sortedNotifications = remember(notifications, sortMode) {
        NotificationDrawerLogic.sortNotifications(notifications, sortMode)
    }

    val highlightTransition = rememberInfiniteTransition(label = "notif_highlight_pulse")
    val pulseProgress by highlightTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "notif_pulse_alpha"
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (highlightedIds.isNotEmpty()) {
                onAcknowledgeNewNotifications(highlightedIds)
            }
            onDismiss()
        },
        modifier = Modifier.testTag("notification_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.notifications_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (notifications.any { !it.isRead }) {
                    TextButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier.testTag("notifications_mark_all_read_button")
                    ) {
                        Text(stringResource(id = R.string.notifications_mark_all_read))
                    }
                }
            }

            if (notifications.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = sortMode == NotificationSortMode.DATE,
                        onClick = { sortMode = NotificationSortMode.DATE },
                        label = { Text(stringResource(id = R.string.notifications_sort_date)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("notification_sort_date_chip")
                    )
                    FilterChip(
                        selected = sortMode == NotificationSortMode.PRIORITY,
                        onClick = { sortMode = NotificationSortMode.PRIORITY },
                        label = { Text(stringResource(id = R.string.notifications_sort_priority)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("notification_sort_priority_chip")
                    )
                }
            }

            if (sortedNotifications.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.notifications_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    items(sortedNotifications, key = { it.id }) { notif ->
                        val priority = NotificationDrawerLogic.priorityForType(notif.type)
                        val isHighlighted = notif.id in highlightedIds
                        val formattedTime = NotificationDrawerLogic.formatTimestamp(notif.createdAt)

                        val baseContainer = when {
                            !notif.isRead && notif.type == "badge" ->
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                            !notif.isRead ->
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else ->
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                        val targetContainer = if (isHighlighted) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(
                                alpha = 0.55f + (0.35f * pulseProgress)
                            )
                        } else {
                            baseContainer
                        }
                        val animatedContainer by animateColorAsState(
                            targetValue = targetContainer,
                            animationSpec = tween(durationMillis = 350),
                            label = "notif_container_${notif.id}"
                        )
                        val animatedBorderColor by animateColorAsState(
                            targetValue = if (isHighlighted) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f + 0.55f * pulseProgress)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = tween(durationMillis = 300),
                            label = "notif_border_${notif.id}"
                        )

                        val (typeIcon, typeColor) = when (notif.type) {
                            "badge" -> Icons.Filled.EmojiEvents to MaterialTheme.colorScheme.tertiary
                            "quiz" -> Icons.Filled.Quiz to SvtCoralError
                            "message" -> Icons.AutoMirrored.Filled.Chat to SvtEmeraldPrimary
                            "enrollment" -> Icons.Filled.HowToReg to SvtOceanSecondary
                            "resource" -> Icons.Filled.MenuBook to MaterialTheme.colorScheme.secondary
                            "announcement" -> Icons.Filled.Campaign to MaterialTheme.colorScheme.primary
                            else -> Icons.Filled.Science to MaterialTheme.colorScheme.primary
                        }

                        val (priorityLabelRes, priorityBgColor, priorityTextColor) = when (priority) {
                            NotificationPriority.HIGH -> Triple(
                                R.string.notification_priority_high,
                                SvtCoralError.copy(alpha = 0.14f),
                                SvtCoralError
                            )
                            NotificationPriority.MEDIUM -> Triple(
                                R.string.notification_priority_medium,
                                SvtOceanSecondary.copy(alpha = 0.14f),
                                SvtOceanSecondary
                            )
                            NotificationPriority.NORMAL -> Triple(
                                R.string.notification_priority_normal,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .testTag("notification_card_${notif.id}"),
                            colors = CardDefaults.cardColors(containerColor = animatedContainer),
                            border = if (isHighlighted) BorderStroke(2.dp, animatedBorderColor) else null
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(typeColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        AnimatedVisibility(
                                            visible = isHighlighted,
                                            enter = fadeIn() + scaleIn()
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("notification_new_badge_${notif.id}")
                                            ) {
                                                Text(
                                                    text = stringResource(id = R.string.notification_new_badge),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = notif.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = priorityBgColor,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(id = priorityLabelRes),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = priorityTextColor,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (formattedTime.isNotBlank()) {
                                            Text(
                                                text = formattedTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                                if (!notif.isRead) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
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
}

@Composable
fun SvtHeroBannerCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    @DrawableRes backgroundResId: Int = R.drawable.img_tab_exercises,
    heightDp: Int = 148,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(MaterialTheme.shapes.large)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                shape = MaterialTheme.shapes.large
            )
    ) {
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            alpha = if (isDark) 0.38f else 1f,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.90f else 0.90f),
                            MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.56f),
                            MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.35f else 0.08f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(Modifier.width(10.dp))
                FilledTonalButton(
                    onClick = onActionClick,
                    modifier = Modifier.testTag("hero_svt_lab_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Biotech,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun SvtFactOfTheDayBanner(
    currentLanguage: AppLanguage,
    allowedDomains: Set<SvtDomain> = SvtDomain.entries.toSet(),
    onOpenLab: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val facts = SvtKnowledgeCatalog.factsOfTheDay.filter { it.domain in allowedDomains }
        .ifEmpty { SvtKnowledgeCatalog.factsOfTheDay }
    if (facts.isEmpty()) return
    var factIndex by rememberSaveable { mutableIntStateOf(0) }
    val fact = facts[factIndex % facts.size]

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        shape = MaterialTheme.shapes.medium
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.svt_fact_of_day_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(id = fact.domain.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = fact.title.get(currentLanguage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = fact.body.get(currentLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (onOpenLab != null) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenLab != null) {
                    TextButton(
                        onClick = onOpenLab,
                        modifier = Modifier.testTag("svt_fact_open_lab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Biotech,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(id = R.string.svt_lab_open_button))
                    }
                }
                TextButton(
                    onClick = { factIndex = (factIndex + 1) % facts.size },
                    modifier = Modifier.testTag("svt_fact_next_button")
                ) {
                    Text(stringResource(id = R.string.svt_fact_next))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
