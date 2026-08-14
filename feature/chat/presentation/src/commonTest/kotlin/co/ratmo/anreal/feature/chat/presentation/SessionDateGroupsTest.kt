package co.ratmo.anreal.feature.chat.presentation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.presentation.component.documentsSummary
import kotlin.test.Test

class SessionDateGroupsTest {

    @Test
    fun groups_today_yesterday_and_calendar() {
        val groups = groupSessionsByDate(
            sessions = listOf(
                ChatSessionUi(id = "1", title = "halo boy!", unread = false, updatedAt = "2026-08-14T10:00:00Z"),
                ChatSessionUi(id = "2", title = "panduan Anvia", unread = false, updatedAt = "2026-08-13T18:00:00Z"),
                ChatSessionUi(id = "3", title = "oi, siapa nama gw?", unread = false, updatedAt = "2026-08-12T09:00:00Z"),
            ),
            todayIso = "2026-08-14",
        )
        assertThat(groups.map { it.label }).containsExactly(
            AnrealCopy.get(AnrealCopy.LABEL_TODAY),
            AnrealCopy.get(AnrealCopy.LABEL_YESTERDAY),
            "Aug 12, 2026",
        )
    }

    @Test
    fun documents_summary_concatenates_counts() {
        assertThat(documentsSummary(1, 1)).isEqualTo("1 active · 1 cited")
        assertThat(documentsSummary(0, 0)).isEqualTo(AnrealCopy.get(AnrealCopy.LABEL_DOCUMENTS_NONE))
    }
}
