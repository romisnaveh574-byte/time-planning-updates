package com.example.birthdaycountdown.update

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadProgressTest {
    @Test
    fun copyReportsTheFinalDownloadProgress() {
        val source = ByteArray(4097) { (it % 97).toByte() }
        val values = mutableListOf<DownloadProgress>()
        val output = ByteArrayOutputStream()

        DownloadProgress.copy(ByteArrayInputStream(source), output, source.size.toLong()) { values += it }

        assertEquals(source.toList(), output.toByteArray().toList())
        assertEquals(source.size.toLong(), values.last().receivedBytes)
        assertEquals(100, values.last().percentage)
        assertTrue(values.last().isComplete)
    }
}
