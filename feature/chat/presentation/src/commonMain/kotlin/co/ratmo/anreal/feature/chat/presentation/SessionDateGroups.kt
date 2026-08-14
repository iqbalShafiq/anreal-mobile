package co.ratmo.anreal.feature.chat.presentation

import co.ratmo.anreal.core.presentation.AnrealCopy
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class SessionDateGroupUi(
    val label: String,
    val sessions: List<ChatSessionUi>,
)

internal fun groupSessionsByDate(
    sessions: List<ChatSessionUi>,
    todayIso: String = currentIsoDate(),
): List<SessionDateGroupUi> {
    val yesterdayIso = isoDateMinusDays(todayIso, 1)
    val groups = linkedMapOf<String, MutableList<ChatSessionUi>>()
    sessions.forEach { session ->
        val label = sessionDateLabel(session.updatedAt, todayIso, yesterdayIso)
        groups.getOrPut(label) { mutableListOf() }.add(session)
    }
    return groups.map { (label, items) -> SessionDateGroupUi(label = label, sessions = items) }
}

internal fun sessionDateLabel(
    updatedAt: String,
    todayIso: String,
    yesterdayIso: String = isoDateMinusDays(todayIso, 1),
): String {
    val date = updatedAt.take(10)
    if (date.length != 10 || date[4] != '-' || date[7] != '-') {
        return AnrealCopy.get(AnrealCopy.LABEL_EARLIER)
    }
    return when (date) {
        todayIso -> AnrealCopy.get(AnrealCopy.LABEL_TODAY)
        yesterdayIso -> AnrealCopy.get(AnrealCopy.LABEL_YESTERDAY)
        else -> formatCalendarLabel(date, todayIso)
    }
}

@OptIn(ExperimentalTime::class)
internal fun currentIsoDate(): String = Clock.System.now().toString().take(10)

internal fun isoDateMinusDays(isoDate: String, days: Int): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val year = parts[0].toIntOrNull() ?: return isoDate
    val month = parts[1].toIntOrNull() ?: return isoDate
    val day = parts[2].toIntOrNull() ?: return isoDate
    var y = year
    var m = month
    var d = day - days
    while (d <= 0) {
        m -= 1
        if (m <= 0) {
            m = 12
            y -= 1
        }
        d += daysInMonth(y, m)
    }
    return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

private fun formatCalendarLabel(isoDate: String, todayIso: String): String {
    val year = isoDate.take(4).toIntOrNull() ?: return isoDate
    val month = isoDate.substring(5, 7).toIntOrNull() ?: return isoDate
    val day = isoDate.substring(8, 10).toIntOrNull() ?: return isoDate
    val todayYear = todayIso.take(4).toIntOrNull() ?: year
    val daysAgo = isoDayNumber(todayIso) - isoDayNumber(isoDate)
    val monthName = MONTHS.getOrElse(month - 1) { isoDate }
    return if (year == todayYear && daysAgo in 2..6) {
        "$monthName $day, $year"
    } else {
        "$monthName $year"
    }
}

private fun isoDayNumber(isoDate: String): Int {
    val year = isoDate.take(4).toIntOrNull() ?: return 0
    val month = isoDate.substring(5, 7).toIntOrNull() ?: return 0
    val day = isoDate.substring(8, 10).toIntOrNull() ?: return 0
    var total = day
    repeat(month - 1) { index -> total += daysInMonth(year, index + 1) }
    total += year * 365 + year / 4 - year / 100 + year / 400
    return total
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }
}

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
