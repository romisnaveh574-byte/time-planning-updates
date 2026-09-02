package com.example.birthdaycountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenOrderingTest {
    data class Item(val id: Long, val pinned: Boolean)

    @Test
    fun pinnedItemsSortAheadWithStableRelativeOrder() {
        val ordered = pinnedFirstStableOrder(
            listOf(
                Item(1, pinned = false),
                Item(2, pinned = true),
                Item(3, pinned = false),
                Item(4, pinned = true),
                Item(5, pinned = true)
            )
        ) { it.pinned }

        assertEquals(listOf(2L, 4L, 5L, 1L, 3L), ordered.map(Item::id))
    }
}
