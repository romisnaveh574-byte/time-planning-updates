package com.example.birthdaycountdown.update

data class AppVersion(val code: Long, val name: String) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        val a = parseVersionName(name) ?: return code.compareTo(other.code)
        val b = parseVersionName(other.name) ?: return code.compareTo(other.code)
        return a.zip(b).firstOrNull { it.first != it.second }?.let { it.first.compareTo(it.second) } ?: 0
    }
}

fun parseVersionName(value: String): List<Int>? = value.removePrefix("v").split('.').takeIf { it.size == 3 }?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 }
