package com.denser.june.presentation.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.screens.home.timeline.components.journal.TimelineJournalCard

@Composable
fun ItemInfoSheetContent(
    dateDisplay: String,
    parentJournal: Journal?,
    modifier: Modifier = Modifier,
    onOpenJournal: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                text = dateDisplay,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (parentJournal != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TimelineJournalCard(
                        journal = parentJournal,
                        onJournalClick = onOpenJournal
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemInfoSheet(
    onDismissRequest: () -> Unit,
    dateDisplay: String,
    parentJournal: Journal? = null,
    modifier: Modifier = Modifier,
    onOpenJournal: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
    ) {
        ItemInfoSheetContent(
            dateDisplay = dateDisplay,
            parentJournal = parentJournal,
            onOpenJournal = onOpenJournal,
            content = content
        )
    }
}

@Composable
fun ItemSpecsRow(
    specs: List<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            specs.forEachIndexed { index, spec ->
                Text(
                    text = spec,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (index < specs.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .height(12.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}
