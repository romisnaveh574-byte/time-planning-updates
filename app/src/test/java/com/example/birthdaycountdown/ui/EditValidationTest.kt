package com.example.birthdaycountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditValidationTest {
    @Test
    fun editorUsesBasicThenDisplayAndReminderSteps() {
        assertEquals(
            listOf(EditorStep.BASIC, EditorStep.DISPLAY_AND_REMINDER),
            EditorStep.entries
        )
    }

    @Test
    fun blankNameProducesAFieldLevelMessage() {
        assertEquals(
            "请输入名称",
            editorValidationMessage(
                name = "",
                secondText = "0",
                lunarValid = true,
                countdownMask = 4,
                showsDate = true
            )
        )
    }

    @Test
    fun secondsMustBeAnIntegerBetweenZeroAndFiftyNine() {
        assertTrue(isValidSecondInput("0"))
        assertTrue(isValidSecondInput("59"))
        assertFalse(isValidSecondInput(""))
        assertFalse(isValidSecondInput("60"))
    }
}
