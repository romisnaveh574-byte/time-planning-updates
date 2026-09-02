package com.example.birthdaycountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderVisibleItemsTest {
    data class Item(val id: Long)

    @Test
    fun reorderStateExplainsTheActiveGesture() {
        assertEquals("正在排序，松开后保存顺序", reorderStatusLabel(dragging = true))
        assertEquals(null, reorderStatusLabel(dragging = false))
    }

    @Test
    fun movesOnlyAmongVisibleItems() {
        val all = listOf(Item(1), Item(2), Item(3), Item(4), Item(5))

        val moved = moveVisibleItem(all, listOf(1L, 3L, 5L), 3L, 1) { it.id }

        assertEquals(listOf(1L, 2L, 5L, 4L, 3L), moved.map { it.id })
    }
}
