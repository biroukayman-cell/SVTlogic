package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.SvtLabLogic
import com.example.data.svt.SvtDomain
import com.example.data.svt.SvtKnowledgeCatalog
import com.example.data.svt.SvtMindMap
import com.example.ui.components.SvtEmptyStateCard
import com.example.ui.theme.SvtCoralError
import com.example.ui.theme.SvtEmeraldPrimary
import com.example.ui.theme.SvtOceanSecondary
import com.example.ui.theme.SvtSuccess
import com.example.ui.theme.SvtWarning
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SvtMindMapsLabSection(
    currentLanguage: AppLanguage,
    allowedDomains: Set<SvtDomain>
) {
    val availableDomains = remember(allowedDomains) {
        SvtDomain.entries.filter { it in allowedDomains }
    }
    var selectedDomainName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDomain = selectedDomainName?.let { name ->
        availableDomains.firstOrNull { it.name == name }
    }

    val filteredMaps = remember(allowedDomains, selectedDomain) {
        SvtKnowledgeCatalog.mindMaps.filter { map ->
            map.domain in allowedDomains && (selectedDomain == null || map.domain == selectedDomain)
        }
    }

    var selectedMapId by rememberSaveable { mutableStateOf(filteredMaps.firstOrNull()?.id.orEmpty()) }
    LaunchedEffect(filteredMaps) {
        if (filteredMaps.none { it.id == selectedMapId }) {
            selectedMapId = filteredMaps.firstOrNull()?.id.orEmpty()
        }
    }
    val activeMap = filteredMaps.firstOrNull { it.id == selectedMapId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("svt_mindmaps_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.svt_mindmap_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.svt_mindmap_section_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (availableDomains.size > 1) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDomain == null,
                            onClick = { selectedDomainName = null },
                            label = { Text(stringResource(id = R.string.svt_domain_all)) }
                        )
                    }
                    items(availableDomains) { domain ->
                        FilterChip(
                            selected = selectedDomain == domain,
                            onClick = { selectedDomainName = domain.name },
                            label = { Text(stringResource(id = domain.labelRes)) }
                        )
                    }
                }
            }
        }

        if (filteredMaps.isEmpty() || activeMap == null) {
            item {
                SvtEmptyStateCard(title = stringResource(id = R.string.svt_mindmap_empty))
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredMaps, key = { it.id }) { map ->
                        FilterChip(
                            selected = map.id == activeMap.id,
                            onClick = { selectedMapId = map.id },
                            label = {
                                Text(
                                    text = map.centralConcept.get(currentLanguage),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("mindmap_chip_${map.id}")
                        )
                    }
                }
            }

            item {
                MindMapCentralHubCard(
                    mindMap = activeMap,
                    currentLanguage = currentLanguage
                )
            }

            items(activeMap.branches.size) { index ->
                val branch = activeMap.branches[index]
                val branchAccent = mindMapBranchColor(index)
                var expanded by rememberSaveable(activeMap.id, index) { mutableStateOf(true) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clickable { expanded = !expanded }
                        .testTag("mindmap_branch_${activeMap.id}_$index")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(branchAccent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = branchAccent
                                )
                            }
                            Text(
                                text = branch.title.get(currentLanguage),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = branchAccent,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp)
                            ) {
                                branch.nodes.forEach { nodeText ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(branchAccent)
                                        )
                                        Text(
                                            text = nodeText.get(currentLanguage),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                branch.keyFormulaOrRule?.let { formula ->
                                    Surface(
                                        color = branchAccent.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = formula.get(currentLanguage),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = branchAccent,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
private fun MindMapCentralHubCard(
    mindMap: SvtMindMap,
    currentLanguage: AppLanguage
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(id = mindMap.domain.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.svt_mindmap_branches_count, mindMap.branches.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = mindMap.centralConcept.get(currentLanguage),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = mindMap.subtitle.get(currentLanguage),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = mindMap.summary.get(currentLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
            )
        }
    }
}

private fun mindMapBranchColor(index: Int): Color = when (index % 4) {
    0 -> SvtEmeraldPrimary
    1 -> SvtOceanSecondary
    2 -> SvtWarning
    else -> SvtCoralError
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SvtInteractiveToolsLabSection(
    allowedDomains: Set<SvtDomain>
) {
    var selectedToolIdx by rememberSaveable { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("svt_extra_tools_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedToolIdx == 0,
                        onClick = { selectedToolIdx = 0 },
                        leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(stringResource(id = R.string.svt_tool_atp_tab)) },
                        modifier = Modifier.testTag("svt_tool_chip_atp")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedToolIdx == 1,
                        onClick = { selectedToolIdx = 1 },
                        leadingIcon = { Icon(Icons.Filled.Biotech, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(stringResource(id = R.string.svt_tool_genetics_tab)) },
                        modifier = Modifier.testTag("svt_tool_chip_genetics")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedToolIdx == 2,
                        onClick = { selectedToolIdx = 2 },
                        leadingIcon = { Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(stringResource(id = R.string.svt_tool_earth_tab)) },
                        modifier = Modifier.testTag("svt_tool_chip_earth")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedToolIdx == 3,
                        onClick = { selectedToolIdx = 3 },
                        leadingIcon = { Icon(Icons.Filled.Timeline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(stringResource(id = R.string.svt_tool_chip_nervous_muscular)) },
                        modifier = Modifier.testTag("svt_tool_chip_nervous_muscular")
                    )
                }
            }
        }

        when (selectedToolIdx) {
            0 -> item { AtpMetabolismToolCard() }
            1 -> item { GeneticsAndBloodToolCard() }
            2 -> item { EcologyAndSeismicToolCard(showSeismic = SvtDomain.GEOLOGY in allowedDomains) }
            else -> item { NervousMuscularToolCard() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AtpMetabolismToolCard() {
    var glucoseMoles by rememberSaveable { mutableFloatStateOf(1f) }
    var selectedPathway by rememberSaveable { mutableStateOf(SvtLabLogic.MetabolicPathway.AEROBIC_RESPIRATION) }
    val result = remember(glucoseMoles, selectedPathway) {
        SvtLabLogic.computeMetabolicEnergy(glucoseMoles.toInt().coerceIn(1, 10), selectedPathway)
    }

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
            Text(
                text = stringResource(id = R.string.svt_tool_atp_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${stringResource(id = R.string.svt_tool_atp_glucose_label)} : ${result.glucoseMoles}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = glucoseMoles,
                onValueChange = { glucoseMoles = it },
                valueRange = 1f..10f,
                steps = 8
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedPathway == SvtLabLogic.MetabolicPathway.AEROBIC_RESPIRATION,
                    onClick = { selectedPathway = SvtLabLogic.MetabolicPathway.AEROBIC_RESPIRATION },
                    label = { Text(stringResource(id = R.string.svt_tool_atp_pathway_respiration)) }
                )
                FilterChip(
                    selected = selectedPathway == SvtLabLogic.MetabolicPathway.LACTIC_FERMENTATION,
                    onClick = { selectedPathway = SvtLabLogic.MetabolicPathway.LACTIC_FERMENTATION },
                    label = { Text(stringResource(id = R.string.svt_tool_atp_pathway_lactic)) }
                )
                FilterChip(
                    selected = selectedPathway == SvtLabLogic.MetabolicPathway.ALCOHOLIC_FERMENTATION,
                    onClick = { selectedPathway = SvtLabLogic.MetabolicPathway.ALCOHOLIC_FERMENTATION },
                    label = { Text(stringResource(id = R.string.svt_tool_atp_pathway_alcoholic)) }
                )
            }
            HorizontalDivider()
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.svt_tool_atp_total_result, result.totalAtpMoles),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(
                            id = R.string.svt_tool_atp_efficiency,
                            result.yieldPercentage.toInt(),
                            (result.totalAtpMoles * 30.5).toInt().toString()
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "O₂ : ${result.o2ConsumedMoles} mol  •  CO₂ : ${result.co2ProducedMoles} mol  •  Résidus : ${result.organicByproductMoles} mol",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneticsAndBloodToolCard() {
    var parent1 by rememberSaveable { mutableStateOf(SvtLabLogic.MonohybridGenotype.HETERO) }
    var parent2 by rememberSaveable { mutableStateOf(SvtLabLogic.MonohybridGenotype.HETERO) }
    val crossResult = remember(parent1, parent2) {
        SvtLabLogic.computeMonohybridCross(parent1, parent2)
    }

    var donorAbo by rememberSaveable { mutableStateOf(SvtLabLogic.BloodGroupAbo.O) }
    var donorRh by rememberSaveable { mutableStateOf(false) }
    var recipientAbo by rememberSaveable { mutableStateOf(SvtLabLogic.BloodGroupAbo.A) }
    var recipientRh by rememberSaveable { mutableStateOf(true) }
    val bloodOk = remember(donorAbo, donorRh, recipientAbo, recipientRh) {
        SvtLabLogic.isBloodCompatible(donorAbo, donorRh, recipientAbo, recipientRh)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Text(
                    text = stringResource(id = R.string.svt_tool_punnett_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(id = R.string.svt_tool_punnett_parent1), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SvtLabLogic.MonohybridGenotype.entries.forEach { g ->
                        FilterChip(
                            selected = parent1 == g,
                            onClick = { parent1 = g },
                            label = { Text(g.display) }
                        )
                    }
                }
                Text(stringResource(id = R.string.svt_tool_punnett_parent2), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SvtLabLogic.MonohybridGenotype.entries.forEach { g ->
                        FilterChip(
                            selected = parent2 == g,
                            onClick = { parent2 = g },
                            label = { Text(g.display) }
                        )
                    }
                }

                // 2x2 Punnett grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f))
                        listOf(parent2.gametes.first, parent2.gametes.second).forEach { gam ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = gam,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                    val p1Gametes = listOf(parent1.gametes.first, parent1.gametes.second)
                    crossResult.grid.forEachIndexed { rIdx, rowCells ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = p1Gametes[rIdx],
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            rowCells.forEach { cell ->
                                Surface(
                                    color = if (cell == "aa") SvtWarning.copy(alpha = 0.16f)
                                    else SvtEmeraldPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                ) {
                                    Text(
                                        text = cell,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "AA: ${crossResult.aaPercent}%  •  Aa: ${crossResult.hetPercent}%  •  aa: ${crossResult.recPercent}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${stringResource(id = R.string.svt_tool_punnett_dominant_pct, crossResult.dominantPhenotypePercent)}   |   ${stringResource(id = R.string.svt_tool_punnett_recessive_pct, crossResult.recessivePhenotypePercent)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

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
                Text(
                    text = stringResource(id = R.string.svt_tool_abo_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(id = R.string.svt_tool_abo_donor), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SvtLabLogic.BloodGroupAbo.entries.forEach { group ->
                        FilterChip(
                            selected = donorAbo == group,
                            onClick = { donorAbo = group },
                            label = { Text(group.label) }
                        )
                    }
                    FilterChip(
                        selected = donorRh,
                        onClick = { donorRh = !donorRh },
                        label = { Text(if (donorRh) "Rh+" else "Rh-") }
                    )
                }
                Text(stringResource(id = R.string.svt_tool_abo_recipient), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SvtLabLogic.BloodGroupAbo.entries.forEach { group ->
                        FilterChip(
                            selected = recipientAbo == group,
                            onClick = { recipientAbo = group },
                            label = { Text(group.label) }
                        )
                    }
                    FilterChip(
                        selected = recipientRh,
                        onClick = { recipientRh = !recipientRh },
                        label = { Text(if (recipientRh) "Rh+" else "Rh-") }
                    )
                }
                Surface(
                    color = (if (bloodOk) SvtSuccess else SvtCoralError).copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (bloodOk) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (bloodOk) SvtSuccess else SvtCoralError
                        )
                        Text(
                            text = if (bloodOk) {
                                stringResource(id = R.string.svt_tool_abo_compatible)
                            } else {
                                stringResource(id = R.string.svt_tool_abo_incompatible, "${donorAbo.label}${if (donorRh) "+" else "-"}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (bloodOk) SvtSuccess else SvtCoralError
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EcologyAndSeismicToolCard(showSeismic: Boolean) {
    var dbo5 by rememberSaveable { mutableFloatStateOf(4.0f) }
    var dco by rememberSaveable { mutableFloatStateOf(12.0f) }
    var o2 by rememberSaveable { mutableFloatStateOf(6.8f) }
    var deltaT by rememberSaveable { mutableFloatStateOf(15.0f) }

    val waterResult = remember(dbo5, dco, o2) {
        SvtLabLogic.evaluateWaterQuality(dbo5.toDouble(), dco.toDouble(), o2.toDouble())
    }
    val seismicResult = remember(deltaT) {
        SvtLabLogic.computeSeismicEpicenter(deltaT.toDouble())
    }

    val waterLabelRes = when (waterResult.qualityClass) {
        SvtLabLogic.WaterQualityClass.EXCELLENT -> R.string.svt_tool_water_quality_excellent
        SvtLabLogic.WaterQualityClass.GOOD -> R.string.svt_tool_water_quality_good
        SvtLabLogic.WaterQualityClass.MODERATE -> R.string.svt_tool_water_quality_moderate
        SvtLabLogic.WaterQualityClass.POLLUTED -> R.string.svt_tool_water_quality_polluted
    }
    val waterColor = when (waterResult.qualityClass) {
        SvtLabLogic.WaterQualityClass.EXCELLENT, SvtLabLogic.WaterQualityClass.GOOD -> SvtSuccess
        SvtLabLogic.WaterQualityClass.MODERATE -> SvtWarning
        SvtLabLogic.WaterQualityClass.POLLUTED -> SvtCoralError
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Text(
                    text = stringResource(id = R.string.svt_tool_water_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("${stringResource(id = R.string.svt_tool_water_dbo5)} : ${String.format(Locale.US, "%.1f", dbo5)}")
                Slider(value = dbo5, onValueChange = { dbo5 = it }, valueRange = 1f..25f)

                Text("${stringResource(id = R.string.svt_tool_water_dco)} : ${String.format(Locale.US, "%.1f", dco)}")
                Slider(value = dco, onValueChange = { dco = it }, valueRange = 2f..60f)

                Text("${stringResource(id = R.string.svt_tool_water_o2)} : ${String.format(Locale.US, "%.1f", o2)}")
                Slider(value = o2, onValueChange = { o2 = it }, valueRange = 0f..14f)

                Surface(
                    color = waterColor.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(id = waterLabelRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = waterColor
                        )
                        Text(
                            text = stringResource(
                                id = R.string.svt_tool_water_ratio,
                                String.format(Locale.US, "%.2f", waterResult.biodegradabilityRatio)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (showSeismic) {
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(id = R.string.svt_tool_seismic_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("${stringResource(id = R.string.svt_tool_seismic_deltat)} : ${deltaT.toInt()} s")
                    Slider(value = deltaT, onValueChange = { deltaT = it }, valueRange = 1f..120f)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.svt_tool_seismic_result,
                                String.format(Locale.US, "%.1f", seismicResult.distanceKm)
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NervousMuscularToolCard() {
    var stimulusMv by rememberSaveable { mutableFloatStateOf(-40f) }
    var atpPct by rememberSaveable { mutableFloatStateOf(80f) }
    var isMyelinated by rememberSaveable { mutableStateOf(true) }
    var calciumAvailable by rememberSaveable { mutableStateOf(true) }

    val result = remember(stimulusMv, atpPct, isMyelinated, calciumAvailable) {
        SvtLabLogic.computeNerveAndMuscleResponse(
            stimulusMv = stimulusMv.toInt(),
            isMyelinated = isMyelinated,
            atpLevelPct = atpPct.toInt(),
            calciumAvailable = calciumAvailable
        )
    }

    val stateColor = when (result.contractionState) {
        SvtLabLogic.MuscleContractionState.CONTRACTING -> SvtSuccess
        SvtLabLogic.MuscleContractionState.SUBTHRESHOLD -> SvtOceanSecondary
        SvtLabLogic.MuscleContractionState.NO_CALCIUM_BLOCKED -> SvtWarning
        SvtLabLogic.MuscleContractionState.FATIGUED_NO_ATP -> SvtCoralError
    }

    val stateLabelRes = when (result.contractionState) {
        SvtLabLogic.MuscleContractionState.CONTRACTING -> R.string.svt_tool_neuro_state_contracting
        SvtLabLogic.MuscleContractionState.SUBTHRESHOLD -> R.string.svt_tool_neuro_state_subthreshold
        SvtLabLogic.MuscleContractionState.NO_CALCIUM_BLOCKED -> R.string.svt_tool_neuro_state_no_ca
        SvtLabLogic.MuscleContractionState.FATIGUED_NO_ATP -> R.string.svt_tool_neuro_state_no_atp
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.testTag("nervous_muscular_tool_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.svt_tool_neuro_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.svt_tool_neuro_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(
                    id = R.string.svt_tool_neuro_stimulus,
                    result.stimulusMv,
                    result.thresholdMv
                ),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = stimulusMv,
                onValueChange = { stimulusMv = it },
                valueRange = -80f..50f,
                modifier = Modifier.testTag("neuro_stimulus_slider")
            )

            Text(
                text = stringResource(id = R.string.svt_tool_neuro_atp, atpPct.toInt()),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = atpPct,
                onValueChange = { atpPct = it },
                valueRange = 0f..100f,
                modifier = Modifier.testTag("neuro_atp_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.svt_tool_neuro_myelin),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isMyelinated,
                    onCheckedChange = { isMyelinated = it },
                    modifier = Modifier.testTag("neuro_myelin_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.svt_tool_neuro_calcium),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = calciumAvailable,
                    onCheckedChange = { calciumAvailable = it },
                    modifier = Modifier.testTag("neuro_calcium_switch")
                )
            }

            Surface(
                color = stateColor.copy(alpha = 0.14f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (result.actionPotentialTriggered) {
                            stringResource(
                                id = R.string.svt_tool_neuro_ap_yes,
                                String.format(Locale.US, "%.1f", result.conductionVelocityMs)
                            )
                        } else {
                            stringResource(
                                id = R.string.svt_tool_neuro_ap_no,
                                result.stimulusMv,
                                result.thresholdMv
                            )
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(id = stateLabelRes, result.sarcomereShorteningPct),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = stateColor
                    )

                    LinearProgressIndicator(
                        progress = { (result.sarcomereShorteningPct / 35f).coerceIn(0f, 1f) },
                        color = stateColor,
                        trackColor = stateColor.copy(alpha = 0.20f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
