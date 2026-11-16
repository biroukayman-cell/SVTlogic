package com.example.ui.screens

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.BiologicalScaleHint
import com.example.core.QrCodeUtils
import com.example.core.SvtLabLogic
import com.example.data.model.SvtClass
import com.example.data.svt.SvtDomain
import com.example.data.svt.SvtKnowledgeCatalog
import com.example.ui.components.SvtFactOfTheDayBanner
import com.example.ui.components.SvtHeroBannerCard
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtOceanSecondary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.SvtWarning
import java.util.Locale

private enum class SvtLabTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    GLOSSARY(R.string.svt_lab_tab_glossary, Icons.Filled.MenuBook, "svt_lab_tab_glossary"),
    FLASHCARDS(R.string.svt_lab_tab_flashcards, Icons.Filled.Style, "svt_lab_tab_flashcards"),
    DNA(R.string.svt_lab_tab_dna, Icons.Filled.Biotech, "svt_lab_tab_dna"),
    MICROSCOPE_GEOLOGY(R.string.svt_lab_tab_microscope, Icons.Filled.Public, "svt_lab_tab_microscope")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherClassLabThemesSelector(
    svtClass: SvtClass,
    onUpdateClassLabDomains: (classId: String, domains: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDomains = remember(svtClass.visibleLabDomains) {
        SvtLabLogic.resolveVisibleLabDomains(svtClass.visibleLabDomains)
    }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.teacher_lab_themes_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.teacher_lab_themes_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SvtDomain.entries.forEach { domain ->
                    val selected = domain in activeDomains
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val updatedSet = if (selected) activeDomains - domain else activeDomains + domain
                            val updatedNames = SvtDomain.entries
                                .filter { it in updatedSet }
                                .map { it.name }
                            onUpdateClassLabDomains(svtClass.id, updatedNames)
                        },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                text = stringResource(id = domain.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("lab_theme_chip_${svtClass.id}_${domain.name}")
                    )
                }
            }
        }
    }
}

@Composable
fun SvtLabTabScreen(
    isTeacher: Boolean,
    currentLanguage: AppLanguage,
    selectedTeacherClass: SvtClass?,
    studentAllowedDomains: Set<SvtDomain>,
    onUpdateClassLabDomains: (classId: String, domains: List<String>) -> Unit
) {
    val effectiveDomains = remember(isTeacher, selectedTeacherClass?.visibleLabDomains, studentAllowedDomains) {
        if (isTeacher) {
            selectedTeacherClass?.let { SvtLabLogic.resolveVisibleLabDomains(it.visibleLabDomains) }
                ?: SvtDomain.entries.toSet()
        } else {
            studentAllowedDomains.ifEmpty { SvtDomain.entries.toSet() }
        }
    }

    val availableTabs = remember(effectiveDomains) {
        SvtLabTab.entries.filter { tab ->
            when (tab) {
                SvtLabTab.DNA -> SvtDomain.CELL_GENETICS in effectiveDomains
                else -> true
            }
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(SvtLabTab.GLOSSARY) }
    var showTeacherThemeConfig by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(availableTabs) {
        if (selectedTab !in availableTabs) {
            selectedTab = availableTabs.first()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
        ) {
            if (isTeacher && selectedTeacherClass != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedTeacherClass.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { showTeacherThemeConfig = !showTeacherThemeConfig },
                            modifier = Modifier.testTag("toggle_lab_themes_config")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.teacher_lab_themes_title))
                        }
                    }
                    if (showTeacherThemeConfig) {
                        TeacherClassLabThemesSelector(
                            svtClass = selectedTeacherClass,
                            onUpdateClassLabDomains = onUpdateClassLabDomains
                        )
                    }
                }
            } else if (!isTeacher && effectiveDomains.size < SvtDomain.entries.size) {
                val activeDomainLabels = SvtDomain.entries
                    .filter { it in effectiveDomains }
                    .map { stringResource(id = it.labelRes) }
                    .joinToString(", ")
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.student_lab_themes_active_info, activeDomainLabels),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableTabs) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        leadingIcon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(id = tab.labelRes),
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }

            HorizontalDivider()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    SvtLabTab.GLOSSARY -> GlossaryLabSection(
                        currentLanguage = currentLanguage,
                        allowedDomains = effectiveDomains
                    )
                    SvtLabTab.FLASHCARDS -> FlashcardsLabSection(
                        currentLanguage = currentLanguage,
                        allowedDomains = effectiveDomains
                    )
                    SvtLabTab.DNA -> DnaSimulatorLabSection(currentLanguage = currentLanguage)
                    SvtLabTab.MICROSCOPE_GEOLOGY -> MicroscopeAndGeologyLabSection(
                        currentLanguage = currentLanguage,
                        showGeologySections = SvtDomain.GEOLOGY in effectiveDomains
                    )
                }
            }
        }
    }
}

@Composable
private fun SvtDomainFilterRow(
    availableDomains: List<SvtDomain>,
    selectedDomain: SvtDomain?,
    onSelectDomain: (SvtDomain?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        if (availableDomains.size > 1) {
            item {
                FilterChip(
                    selected = selectedDomain == null,
                    onClick = { onSelectDomain(null) },
                    label = { Text(stringResource(id = R.string.svt_domain_all)) }
                )
            }
        }
        items(availableDomains) { domain ->
            FilterChip(
                selected = selectedDomain == domain || (availableDomains.size == 1 && selectedDomain == null),
                onClick = { onSelectDomain(if (availableDomains.size == 1) null else domain) },
                label = { Text(stringResource(id = domain.labelRes)) }
            )
        }
    }
}

@Composable
private fun GlossaryLabSection(
    currentLanguage: AppLanguage,
    allowedDomains: Set<SvtDomain>
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedDomainName by rememberSaveable { mutableStateOf<String?>(null) }
    val availableDomains = remember(allowedDomains) {
        SvtDomain.entries.filter { it in allowedDomains }
    }
    val selectedDomain = selectedDomainName?.let { name ->
        availableDomains.firstOrNull { it.name == name }
    }

    LaunchedEffect(allowedDomains) {
        if (selectedDomainName != null && availableDomains.none { it.name == selectedDomainName }) {
            selectedDomainName = null
        }
    }

    val filtered = remember(query, selectedDomain, allowedDomains, currentLanguage) {
        val q = query.trim().lowercase(Locale.getDefault())
        SvtKnowledgeCatalog.glossaryEntries.filter { entry ->
            val allowedOk = entry.domain in allowedDomains
            val domainOk = selectedDomain == null || entry.domain == selectedDomain
            val textOk = q.isEmpty() ||
                entry.term.get(currentLanguage).lowercase(Locale.getDefault()).contains(q) ||
                entry.definition.get(currentLanguage).lowercase(Locale.getDefault()).contains(q) ||
                entry.keyHint.get(currentLanguage).lowercase(Locale.getDefault()).contains(q)
            allowedOk && domainOk && textOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SvtHeroBannerCard(
                title = stringResource(id = R.string.svt_lab_title),
                subtitle = stringResource(id = R.string.svt_lab_subtitle)
            )
        }

        item {
            SvtFactOfTheDayBanner(
                currentLanguage = currentLanguage,
                allowedDomains = allowedDomains,
                onOpenLab = null
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(id = R.string.svt_glossary_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(id = R.string.svt_dna_clear))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("svt_glossary_search_input")
            )
        }

        item {
            SvtDomainFilterRow(
                availableDomains = availableDomains,
                selectedDomain = selectedDomain,
                onSelectDomain = { selectedDomainName = it?.name }
            )
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.svt_glossary_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { entry ->
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.term.get(currentLanguage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(id = entry.domain.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = entry.definition.get(currentLanguage),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = entry.keyHint.get(currentLanguage),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
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
private fun FlashcardsLabSection(
    currentLanguage: AppLanguage,
    allowedDomains: Set<SvtDomain>
) {
    var selectedDomainName by rememberSaveable { mutableStateOf<String?>(null) }
    val availableDomains = remember(allowedDomains) {
        SvtDomain.entries.filter { it in allowedDomains }
    }
    val selectedDomain = selectedDomainName?.let { name ->
        availableDomains.firstOrNull { it.name == name }
    }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var isFlipped by rememberSaveable { mutableStateOf(false) }
    var masteredIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    LaunchedEffect(allowedDomains) {
        if (selectedDomainName != null && availableDomains.none { it.name == selectedDomainName }) {
            selectedDomainName = null
            currentIndex = 0
            isFlipped = false
        }
    }

    val cards = remember(selectedDomain, allowedDomains) {
        val domainFiltered = SvtKnowledgeCatalog.flashcards.filter { it.domain in allowedDomains }
        if (selectedDomain == null) domainFiltered
        else domainFiltered.filter { it.domain == selectedDomain }
    }
    val safeIndex = if (cards.isEmpty()) 0 else currentIndex.coerceIn(0, cards.lastIndex)
    val card = cards.getOrNull(safeIndex)
    val masteredInFilter = cards.count { masteredIds.contains(it.id) }
    val progress = if (cards.isEmpty()) 0f else masteredInFilter.toFloat() / cards.size.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SvtDomainFilterRow(
                availableDomains = availableDomains,
                selectedDomain = selectedDomain,
                onSelectDomain = {
                    selectedDomainName = it?.name
                    currentIndex = 0
                    isFlipped = false
                }
            )
        }

        if (cards.isNotEmpty() && card != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
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
                            Text(
                                text = stringResource(id = R.string.svt_flashcards_progress, safeIndex + 1, cards.size),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(id = R.string.svt_flashcards_mastered_count, masteredInFilter, cards.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = SvtSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
            }

            if (masteredInFilter == cards.size) {
                item {
                    Surface(
                        color = SvtSuccess.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SvtSuccess
                            )
                            Text(
                                text = stringResource(id = R.string.svt_flashcards_completed_banner),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                val isMastered = masteredIds.contains(card.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
                        .testTag("flashcard_surface")
                        .clickable { isFlipped = !isFlipped }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        id = if (isFlipped) R.string.svt_flashcards_answer_badge
                                        else R.string.svt_flashcards_question_badge
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isMastered) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = SvtSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Flip,
                                    contentDescription = stringResource(id = R.string.svt_flashcards_tap_flip),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedContent(targetState = isFlipped, label = "flashcard_flip") { showAnswer ->
                            if (!showAnswer) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = stringResource(id = card.domain.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = card.question.get(currentLanguage),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = card.answer.get(currentLanguage),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    HorizontalDivider()
                                    Text(
                                        text = card.explanation.get(currentLanguage),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.svt_flashcards_tap_flip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                val isMastered = masteredIds.contains(card.id)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isFlipped = false
                            currentIndex = if (safeIndex == 0) cards.lastIndex else safeIndex - 1
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(id = R.string.svt_flashcards_previous), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            masteredIds = if (isMastered) masteredIds - card.id else masteredIds + card.id
                            if (!isMastered && safeIndex < cards.lastIndex) {
                                isFlipped = false
                                currentIndex = safeIndex + 1
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("flashcard_master_button")
                    ) {
                        Text(
                            text = stringResource(
                                id = if (isMastered) R.string.svt_flashcards_review else R.string.svt_flashcards_mastered
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            isFlipped = false
                            currentIndex = if (safeIndex >= cards.lastIndex) 0 else safeIndex + 1
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(id = R.string.svt_flashcards_next), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (masteredIds.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = {
                            masteredIds = emptySet()
                            currentIndex = 0
                            isFlipped = false
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(id = R.string.svt_flashcards_reset))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DnaSimulatorLabSection(currentLanguage: AppLanguage) {
    val context = LocalContext.current
    var rawSequence by rememberSaveable { mutableStateOf(SvtKnowledgeCatalog.dnaPresets.first().templateDna) }
    val result = remember(rawSequence) { SvtLabLogic.transcribeAndTranslate(rawSequence) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.svt_dna_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.svt_dna_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(id = R.string.svt_dna_presets_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SvtKnowledgeCatalog.dnaPresets, key = { it.id }) { preset ->
                        FilterChip(
                            selected = result.templateDna == preset.templateDna,
                            onClick = { rawSequence = preset.templateDna },
                            label = { Text(preset.label.get(currentLanguage)) }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = rawSequence,
                onValueChange = { rawSequence = SvtLabLogic.cleanDnaSequence(it) },
                label = { Text(stringResource(id = R.string.svt_dna_input_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dna_sequence_input")
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf('A', 'T', 'C', 'G').forEach { base ->
                    FilledTonalButton(
                        onClick = { rawSequence = SvtLabLogic.cleanDnaSequence(rawSequence + base) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(base.toString(), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
                IconButton(
                    onClick = {
                        if (rawSequence.isNotEmpty()) rawSequence = rawSequence.dropLast(1)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(id = R.string.svt_dna_clear))
                }
                IconButton(
                    onClick = { rawSequence = "" },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(id = R.string.svt_dna_clear))
                }
            }
        }

        if (!result.hasValidBases || result.codons.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.svt_dna_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item {
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
                        StrandRow(
                            label = stringResource(id = R.string.svt_dna_template_label),
                            sequence = result.templateDna.chunked(3).joinToString(" "),
                            accentColor = SvtOceanSecondary
                        )
                        StrandRow(
                            label = stringResource(id = R.string.svt_dna_complementary_label),
                            sequence = result.complementaryDna.chunked(3).joinToString(" "),
                            accentColor = MaterialTheme.colorScheme.secondary
                        )
                        StrandRow(
                            label = stringResource(id = R.string.svt_dna_mrna_label),
                            sequence = result.mrnaStrand.chunked(3).joinToString(" "),
                            accentColor = SvtEmeraldPrimary
                        )

                        HorizontalDivider()

                        Text(
                            text = stringResource(id = R.string.svt_dna_codons_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            result.codons.forEachIndexed { idx, codon ->
                                val aa = result.aminoAcids.getOrNull(idx)
                                val bg = when {
                                    aa == "STOP" -> SvtCoralError.copy(alpha = 0.16f)
                                    aa == "Met" -> SvtSuccess.copy(alpha = 0.16f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                Surface(color = bg, shape = RoundedCornerShape(10.dp)) {
                                    Text(
                                        text = codon,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.svt_dna_protein_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            result.aminoAcids.forEach { aa ->
                                val chipColor = when (aa) {
                                    "STOP" -> SvtCoralError
                                    "Met" -> SvtSuccess
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Surface(
                                    color = chipColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = aa,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = chipColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        if (result.stoppedEarly) {
                            Text(
                                text = stringResource(id = R.string.svt_dna_stop_notice),
                                style = MaterialTheme.typography.labelMedium,
                                color = SvtCoralError
                            )
                        }
                        if (result.trailingBases.isNotEmpty()) {
                            Text(
                                text = stringResource(id = R.string.svt_dna_trailing_notice, result.trailingBases),
                                style = MaterialTheme.typography.labelMedium,
                                color = SvtWarning
                            )
                        }

                        val copiedMsg = stringResource(id = R.string.svt_dna_copied)
                        OutlinedButton(
                            onClick = {
                                val summary = buildString {
                                    appendLine("ADN 3'→5': ${result.templateDna}")
                                    appendLine("ARNm 5'→3': ${result.mrnaStrand}")
                                    append("Polypeptide: ${result.aminoAcids.joinToString("-")}")
                                }
                                if (QrCodeUtils.copyTextToClipboard(context, "SVT DNA Synthesis", summary)) {
                                    Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.svt_dna_copy_result))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrandRow(
    label: String,
    sequence: String,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            color = accentColor.copy(alpha = 0.08f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = sequence,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun MicroscopeAndGeologyLabSection(
    currentLanguage: AppLanguage,
    showGeologySections: Boolean
) {
    var eyepieceMag by rememberSaveable { mutableIntStateOf(10) }
    var objectiveMag by rememberSaveable { mutableIntStateOf(40) }
    var observedMmText by rememberSaveable { mutableStateOf("4.5") }

    val observedMm = observedMmText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val metrics = remember(eyepieceMag, objectiveMag, observedMm) {
        SvtLabLogic.computeMicroscopeMetrics(eyepieceMag, objectiveMag, observedMm)
    }

    val scaleLabelRes = when (metrics.scaleHint) {
        BiologicalScaleHint.NANOSCALE_VIRUS -> R.string.svt_scale_virus
        BiologicalScaleHint.BACTERIA -> R.string.svt_scale_bacteria
        BiologicalScaleHint.BLOOD_CELL -> R.string.svt_scale_blood_cell
        BiologicalScaleHint.PLANT_CELL -> R.string.svt_scale_plant_cell
        BiologicalScaleHint.MACRO_TISSUE -> R.string.svt_scale_tissue
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(id = R.string.svt_microscope_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.svt_microscope_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(id = R.string.svt_microscope_eyepiece),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15).forEach { mag ->
                            FilterChip(
                                selected = eyepieceMag == mag,
                                onClick = { eyepieceMag = mag },
                                label = { Text("×$mag") }
                            )
                        }
                    }

                    Text(
                        text = stringResource(id = R.string.svt_microscope_objective),
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(4, 10, 40, 100)) { mag ->
                            FilterChip(
                                selected = objectiveMag == mag,
                                onClick = { objectiveMag = mag },
                                label = { Text("×$mag") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = observedMmText,
                        onValueChange = { input ->
                            observedMmText = input.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                        },
                        label = { Text(stringResource(id = R.string.svt_microscope_observed_mm)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("microscope_observed_input")
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.svt_microscope_total_mag, metrics.totalMagnification),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(id = R.string.svt_microscope_real_size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.2f µm  •  %.4f mm  •  %.0f nm",
                                    metrics.realMicrometers,
                                    metrics.realMm,
                                    metrics.realNanometers
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = SvtEmeraldPrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = stringResource(id = scaleLabelRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SvtEmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Text(
                                text = stringResource(id = R.string.svt_microscope_formula),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showGeologySections) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(id = R.string.svt_geology_eras_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(SvtKnowledgeCatalog.geologicalEras, key = { it.id }) { era ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = era.name.get(currentLanguage),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = era.span.get(currentLanguage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = era.highlights.get(currentLanguage),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Filled.Terrain, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(id = R.string.svt_rocks_families_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(SvtKnowledgeCatalog.rockFamilies, key = { it.id }) { family ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = family.name.get(currentLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = family.origin.get(currentLanguage),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = family.examples.get(currentLanguage),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
