package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.launch

enum class StudentListSortOption {
    NAME_ASC,
    SCORE_DESC,
    MASSAR_ASC
}

@Composable
fun <T> OrderedStudentListSection(
    items: List<T>,
    itemKey: (index: Int, item: T) -> Any,
    modifier: Modifier = Modifier,
    maxListHeight: Dp = 360.dp,
    sortOption: StudentListSortOption? = null,
    onSortChange: ((StudentListSortOption) -> Unit)? = null,
    availableSorts: List<StudentListSortOption> = listOf(
        StudentListSortOption.NAME_ASC,
        StudentListSortOption.SCORE_DESC,
        StudentListSortOption.MASSAR_ASC
    ),
    tagPrefix: String = "ordered_student_list",
    itemContent: @Composable (index: Int, item: T) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val visibleRangeText by remember(items.size, listState) {
        derivedStateOf {
            if (items.isEmpty()) {
                "0 / 0"
            } else {
                val visible = listState.layoutInfo.visibleItemsInfo
                val first = (listState.firstVisibleItemIndex + 1).coerceIn(1, items.size)
                val last = if (visible.isNotEmpty()) {
                    (visible.last().index + 1).coerceIn(first, items.size)
                } else {
                    first
                }
                "$first–$last / ${items.size}"
            }
        }
    }

    val canScrollUp by remember(listState) {
        derivedStateOf { listState.canScrollBackward }
    }
    val canScrollDown by remember(listState) {
        derivedStateOf { listState.canScrollForward }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sortOption != null && onSortChange != null && availableSorts.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    availableSorts.forEach { option ->
                        val labelRes = when (option) {
                            StudentListSortOption.NAME_ASC -> R.string.student_list_sort_name
                            StudentListSortOption.SCORE_DESC -> R.string.student_list_sort_score
                            StudentListSortOption.MASSAR_ASC -> R.string.student_list_sort_massar
                        }
                        FilterChip(
                            selected = sortOption == option,
                            onClick = { onSortChange(option) },
                            label = { Text(stringResource(labelRes), maxLines = 1) },
                            modifier = Modifier.testTag("${tagPrefix}_sort_${option.name.lowercase()}")
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (items.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = visibleRangeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = {
                                val target = (listState.firstVisibleItemIndex - 3).coerceAtLeast(0)
                                coroutineScope.launch { listState.animateScrollToItem(target) }
                            },
                            enabled = canScrollUp,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${tagPrefix}_arrow_up")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.student_list_scroll_up)
                            )
                        }
                        IconButton(
                            onClick = {
                                val target = (listState.firstVisibleItemIndex + 3)
                                    .coerceAtMost((items.size - 1).coerceAtLeast(0))
                                coroutineScope.launch { listState.animateScrollToItem(target) }
                            },
                            enabled = canScrollDown,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${tagPrefix}_arrow_down")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.student_list_scroll_down)
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxListHeight)
                .testTag("${tagPrefix}_lazy_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> itemKey(index, item) }
            ) { index, item ->
                itemContent(index, item)
            }
        }
    }
}
