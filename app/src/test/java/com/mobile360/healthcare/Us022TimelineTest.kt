package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.data.model.TimelineType
import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.RecordsViewModel
import com.mobile360.healthcare.util.Times
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Tests: US-022 (REQ-019)
class Us022TimelineTest {
    @get:Rule val main = MainDispatcherRule()

    private val from = Times.at(-10, 0, 0)
    private val to = Times.at(-2, 0, 0)
    private val items = listOf(
        timelineItem("before", TimelineType.CONSULTATION, Times.at(-14, 9, 0)),
        timelineItem("onFrom", TimelineType.APPOINTMENT, from),
        timelineItem("inside", TimelineType.PRESCRIPTION, Times.at(-5, 9, 0)),
        timelineItem("onTo", TimelineType.LABORATORY, to),
        timelineItem("after", TimelineType.CONSULTATION, Times.at(-1, 9, 0))
    )

    @Test fun test_us_022_timeline_is_chronological_newest_first_for_all_time() = runTest {
        val vm = RecordsViewModel(StubRecords(items = items), SavedStateHandle())
        vm.setTab(1)
        vm.setPreset("all")
        val ui = vm.ui.first { !it.loading && it.timeline.isNotEmpty() }
        val dates = ui.timeline.map { it.dateMillis }
        assertEquals(dates.sortedDescending(), dates)
        assertEquals(items.size, ui.timeline.size)
    }

    @Test fun test_us_022_date_filter_keeps_only_items_in_range_inclusive() = runTest {
        val vm = RecordsViewModel(StubRecords(items = items), SavedStateHandle())
        vm.setTab(1)
        vm.setCustom(from, to)
        vm.setPreset("custom")
        val ui = vm.ui.first { !it.loading && it.timeline.isNotEmpty() }
        val ids = ui.timeline.map { it.id }.toSet()
        assertTrue("onFrom" in ids && "inside" in ids && "onTo" in ids)
        assertTrue("before" !in ids && "after" !in ids)
    }

    @Test fun test_us_022_range_without_items_gives_empty_timeline() = runTest {
        val vm = RecordsViewModel(StubRecords(items = items), SavedStateHandle())
        vm.setTab(1)
        vm.setCustom(Times.at(-100, 0, 0), Times.at(-90, 0, 0))
        vm.setPreset("custom")
        val ui = vm.ui.first { !it.loading }
        assertTrue(ui.timeline.isEmpty())
    }
}
