package net.abaresults.progresspath.util

import android.graphics.Paint
import java.util.Calendar
import java.util.Date
import java.util.Locale

inline fun <T1 : Any, T2 : Any, R : Any> safeLet(p1: T1?, p2: T2?, block: (T1, T2) -> R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}

inline fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, R : Any> safeLet(p1: T1?, p2: T2?, p3: T3?,p4: T4?, block: (T1, T2, T3, T4) -> R?): R? {
    return if (p1 != null && p2 != null && p3 != null && p4 != null) block(p1, p2, p3, p4) else null
}

fun String?.capitalizeFirst(): String {
    if (this.isNullOrEmpty()) {
        return this ?: ""
    }

    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

fun Date.isToday(): Boolean {
    val today = Calendar.getInstance()
    val dateCalendar = Calendar.getInstance().apply { time = this@isToday }

    return today.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)
}

/**
 * Determines if an item is available for a therapist to work on today.
 *
 * An item is available if it meets one of the following:
 *   - It was answered today by someone else (not the current therapist)
 *   - It has never been answered
 *   - It was not answered today
 */
fun isItemAvailableForTherapist(
    item: net.abaresults.progresspath.model.KidObjectiveItem,
    currentTherapistUserId: String
): Boolean {
    return ((item.lastResponseTime?.isToday() == true && item.lastModificationByUserId != currentTherapistUserId)
                    || item.lastResponseTime == null || item.lastResponseTime?.isToday() == false)
}

fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = ""

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        val testWidth = paint.measureText(testLine)

        if (testWidth <= maxWidth) {
            currentLine = testLine
        } else {
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                lines.add(word)
            }
        }
    }

    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }

    return lines
}