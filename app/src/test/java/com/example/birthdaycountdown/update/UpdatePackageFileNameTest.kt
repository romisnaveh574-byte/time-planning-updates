package com.example.birthdaycountdown.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePackageFileNameTest {
    @Test
    fun packageFileNameIncludesTheReleaseVersion() {
        assertEquals("time-planning-1.4.2.apk", UpdatePackageFileName.forVersion("v1.4.2"))
    }
}
