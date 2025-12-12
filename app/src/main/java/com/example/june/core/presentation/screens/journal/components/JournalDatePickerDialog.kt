package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalDatePickerDialog(
    initialDateMillis: Long, onDateSelected: (Long) -> Unit, onDismiss: () -> Unit
) {
    val initialDate = remember(initialDateMillis) {
        Instant.ofEpochMilli(initialDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val headerFormatter =
        remember { DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault()) }
    val monthTitleFormatter = remember { DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()) }
    val yearFormatter = remember { DateTimeFormatter.ofPattern("yyyy", Locale.getDefault()) }

    val daysInMonth = remember(currentMonth) {
        val firstDayOfMonth = currentMonth.atDay(1)
        val daysInMonthCount = currentMonth.lengthOfMonth()
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

        val days = mutableListOf<LocalDate?>()
        repeat(firstDayOfWeek) { days.add(null) }
        for (i in 1..daysInMonthCount) {
            days.add(currentMonth.atDay(i))
        }
        days
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .clickable(enabled = false) {}) {
            Column(modifier = Modifier.padding(20.dp, 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = headerFormatter.format(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { currentMonth = currentMonth.minusMonths(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { currentMonth = currentMonth.plusMonths(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = yearFormatter.format(currentMonth),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    Text(
                        text = monthTitleFormatter.format(currentMonth),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = TextUnit(1.5F, TextUnitType.Sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    IconButton(
                        onClick = {
                            val today = LocalDate.now()
                            selectedDate = today
                            currentMonth = YearMonth.from(today)
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Jump to today",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    val daysOfWeek = listOf(
                        DayOfWeek.SUNDAY,
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY
                    )
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = MaterialTheme.typography.bodyLargeEmphasized,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(daysInMonth) { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.size(40.dp))
                        } else {
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        when {
                                            isToday -> BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            )

                                            else -> BorderStroke(0.dp, Color.Transparent)
                                        }, shape = RoundedCornerShape(36.dp)
                                    )
                                    .clickable { selectedDate = date }) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMediumEmphasized,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                        isToday -> MaterialTheme.colorScheme.onTertiaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            val millis =
                                selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                                    .toEpochMilli()
                            onDateSelected(millis)
                        }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}