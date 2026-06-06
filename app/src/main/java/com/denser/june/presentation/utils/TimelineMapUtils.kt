package com.denser.june.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Path
import android.graphics.Typeface
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalLocation
import org.maplibre.android.geometry.LatLng
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

data class LocationGroup(
    val location: JournalLocation,
    val journals: List<Journal>,
    val dateText: String
)

object TimelineMapUtils {

    fun buildLocationGroups(sortedPoints: List<Journal>): List<LocationGroup> {
        if (sortedPoints.isEmpty()) return emptyList()
        val groups = mutableListOf<LocationGroup>()
        var currentGroupJournals = mutableListOf<Journal>()
        var currentLoc = sortedPoints.first().location

        sortedPoints.forEach { journal ->
            val loc = journal.location ?: return@forEach
            if (currentLoc == null) {
                currentLoc = loc
                currentGroupJournals.add(journal)
            } else {
                val latDiff = abs(loc.latitude - currentLoc.latitude)
                val lngDiff = abs(loc.longitude - currentLoc.longitude)
                if (latDiff < 1e-5 && lngDiff < 1e-5) {
                    currentGroupJournals.add(journal)
                } else {
                    groups.add(
                        LocationGroup(
                            location = currentLoc,
                            journals = currentGroupJournals,
                            dateText = formatGroupDates(currentGroupJournals)
                        )
                    )
                    currentLoc = loc
                    currentGroupJournals = mutableListOf(journal)
                }
            }
        }
        if (currentGroupJournals.isNotEmpty() && currentLoc != null) {
            groups.add(
                LocationGroup(
                    location = currentLoc,
                    journals = currentGroupJournals,
                    dateText = formatGroupDates(currentGroupJournals)
                )
            )
        }
        return groups
    }

    fun formatGroupDates(journals: List<Journal>): String {
        if (journals.isEmpty()) return ""
        val dates = journals.map { journal ->
            Instant.ofEpochMilli(journal.dateTime).atZone(ZoneId.systemDefault()).toLocalDate()
        }.distinct().sorted()

        val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        if (dates.size == 1) return dates.first().format(shortFmt)

        val first = dates.first()
        val last = dates.last()
        val sameMonthAndYear = dates.all { it.month == first.month && it.year == first.year }

        return if (sameMonthAndYear) {
            val monthStr = first.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
            val isConsecutive = dates.size == (last.dayOfMonth - first.dayOfMonth + 1)
            if (isConsecutive) "${first.dayOfMonth}-${last.dayOfMonth} $monthStr"
            else dates.joinToString(", ") { "${it.dayOfMonth}" } + " $monthStr"
        } else {
            if (dates.size <= 2) dates.joinToString(" & ") { it.format(shortFmt) }
            else first.format(shortFmt) + " - " + last.format(shortFmt)
        }
    }

    fun createArchedPoints(start: LatLng, end: LatLng, segments: Int = 60): List<LatLng> {
        val latDiff = end.latitude - start.latitude
        val lngDiff = end.longitude - start.longitude
        val distance = sqrt(latDiff * latDiff + lngDiff * lngDiff).coerceAtLeast(1e-9)
        val archHeight = distance * 0.15
        
        val offsetLat = (-lngDiff / distance) * archHeight
        val offsetLng = (latDiff / distance) * archHeight

        val shouldInvert = if (abs(offsetLat) > 1e-9) {
            offsetLat < 0
        } else {
            offsetLng < 0
        }
        
        val factor = if (shouldInvert) -1.0 else 1.0
        val controlLat = (start.latitude + end.latitude) / 2.0 + offsetLat * factor
        val controlLng = (start.longitude + end.longitude) / 2.0 + offsetLng * factor

        return (0..segments).map { i ->
            val t = i.toDouble() / segments
            val u = 1.0 - t
            LatLng(
                u * u * start.latitude + 2 * u * t * controlLat + t * t * end.latitude,
                u * u * start.longitude + 2 * u * t * controlLng + t * t * end.longitude
            )
        }
    }

    fun createMarkerBitmap(
        context: Context,
        text: String,
        primaryColor: Int,
        onPrimaryColor: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val textSizePx = 11f * density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            color = onPrimaryColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val paddingH = (10f * density).toInt()
        val paddingV = (6f * density).toInt()
        val arrowH = (5f * density).toInt()
        val width = textBounds.width() + paddingH * 2
        val height = textBounds.height() + paddingV * 2 + arrowH
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            style = Paint.Style.FILL
        }
        val rect = RectF(0f, 0f, width.toFloat(), (height - arrowH).toFloat())
        canvas.drawRoundRect(rect, 6f * density, 6f * density, bgPaint)
        val path = Path().apply {
            moveTo(width / 2f - 6f * density, (height - arrowH).toFloat())
            lineTo(width / 2f + 6f * density, (height - arrowH).toFloat())
            lineTo(width / 2f, height.toFloat())
            close()
        }
        canvas.drawPath(path, bgPaint)
        val textY = (height - arrowH) / 2f + textBounds.height() / 2f - 1f
        canvas.drawText(text, width / 2f, textY, paint)
        return bitmap
    }
}
