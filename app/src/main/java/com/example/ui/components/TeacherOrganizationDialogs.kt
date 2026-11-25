package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Establishment
import com.example.data.model.SvtClass
import com.example.data.svt.SvtDomain

@Composable
fun CreateOrEditEstablishmentDialog(
    initialEstablishment: Establishment? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, city: String) -> Unit
) {
    var name by remember(initialEstablishment) {
        mutableStateOf(initialEstablishment?.name.orEmpty())
    }
    var city by remember(initialEstablishment) {
        mutableStateOf(initialEstablishment?.city.orEmpty())
    }
    val isEdit = initialEstablishment != null

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Apartment, contentDescription = null) },
        title = {
            Text(
                stringResource(
                    if (isEdit) R.string.establishment_edit_dialog_title
                    else R.string.establishment_create_dialog_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.establishment_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("establishment_name_input")
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.establishment_city_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("establishment_city_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), city.trim()) },
                enabled = name.trim().length >= 2,
                modifier = Modifier.testTag("confirm_establishment_button")
            ) {
                Text(stringResource(if (isEdit) R.string.profile_save else R.string.teacher_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun CreateOrEditClassDialog(
    establishments: List<Establishment>,
    initialClass: SvtClass? = null,
    preselectedEstablishmentId: String = "",
    onDismiss: () -> Unit,
    onRequestQuickCreateEstablishment: () -> Unit,
    onConfirm: (name: String, level: String, academicYear: String, establishmentId: String, establishmentName: String) -> Unit
) {
    val isEdit = initialClass != null
    var className by remember(initialClass) { mutableStateOf(initialClass?.name.orEmpty()) }
    var level by remember(initialClass) {
        mutableStateOf(initialClass?.level?.ifBlank { "2ème Bac SVT" } ?: "2ème Bac SVT")
    }
    var year by remember(initialClass) {
        mutableStateOf(initialClass?.academicYear?.ifBlank { "2026-2027" } ?: "2026-2027")
    }
    var selectedEstId by remember(initialClass, preselectedEstablishmentId, establishments) {
        mutableStateOf(
            initialClass?.establishmentId
                ?.takeIf { id -> id.isBlank() || establishments.any { it.id == id } }
                ?: preselectedEstablishmentId.takeIf { id -> establishments.any { it.id == id } }
                ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.School, contentDescription = null) },
        title = {
            Text(
                stringResource(
                    if (isEdit) R.string.class_edit_dialog_title
                    else R.string.teacher_new_class
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text(stringResource(R.string.teacher_class_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("class_name_input")
                )
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text(stringResource(R.string.teacher_class_level)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("class_level_input")
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text(stringResource(R.string.teacher_class_year)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("class_year_input")
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.class_establishment_picker_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = onRequestQuickCreateEstablishment,
                        modifier = Modifier.testTag("quick_add_establishment_in_class_dialog")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.establishment_new_button))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedEstId.isBlank(),
                        onClick = { selectedEstId = "" },
                        label = { Text(stringResource(R.string.establishment_none_chip)) },
                        modifier = Modifier.testTag("class_est_chip_none")
                    )
                    establishments.forEach { est ->
                        FilterChip(
                            selected = selectedEstId == est.id,
                            onClick = { selectedEstId = est.id },
                            label = { Text(est.name) },
                            modifier = Modifier.testTag("class_est_chip_${est.id}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val estName = establishments.firstOrNull { it.id == selectedEstId }?.name.orEmpty()
                    onConfirm(
                        className.trim(),
                        level.trim(),
                        year.trim(),
                        selectedEstId,
                        estName
                    )
                },
                enabled = className.trim().length >= 2 && level.trim().isNotBlank() && year.trim().isNotBlank(),
                modifier = Modifier.testTag("confirm_create_class_button")
            ) {
                Text(stringResource(if (isEdit) R.string.profile_save else R.string.teacher_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AddGlossaryNotionDialog(
    initialDomain: SvtDomain = SvtDomain.NERVOUS_MUSCULAR,
    onDismiss: () -> Unit,
    onConfirm: (domain: SvtDomain, term: String, definition: String, keyHint: String, keywordsRaw: String) -> Unit
) {
    var selectedDomain by remember(initialDomain) { mutableStateOf(initialDomain) }
    var term by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    var keyHint by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.AutoStories, contentDescription = null) },
        title = { Text(stringResource(R.string.glossary_add_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.glossary_notion_domain_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SvtDomain.entries.forEach { domain ->
                        FilterChip(
                            selected = selectedDomain == domain,
                            onClick = { selectedDomain = domain },
                            label = { Text(stringResource(domain.labelRes)) },
                            modifier = Modifier.testTag("glossary_add_domain_${domain.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text(stringResource(R.string.glossary_notion_term_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glossary_notion_term_input")
                )
                OutlinedTextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text(stringResource(R.string.glossary_notion_def_label)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glossary_notion_def_input")
                )
                OutlinedTextField(
                    value = keyHint,
                    onValueChange = { keyHint = it },
                    label = { Text(stringResource(R.string.glossary_notion_hint_label)) },
                    minLines = 1,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glossary_notion_hint_input")
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text(stringResource(R.string.glossary_notion_keywords_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glossary_notion_keywords_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedDomain,
                        term.trim(),
                        definition.trim(),
                        keyHint.trim(),
                        keywords.trim()
                    )
                },
                enabled = term.trim().length >= 2 && definition.trim().length >= 4,
                modifier = Modifier.testTag("confirm_add_glossary_notion_button")
            ) {
                Text(stringResource(R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
