package com.example.birthdaycountdown.ui

internal fun isValidSecondInput(text: String): Boolean = text.toIntOrNull()?.let { it in 0..59 } == true
