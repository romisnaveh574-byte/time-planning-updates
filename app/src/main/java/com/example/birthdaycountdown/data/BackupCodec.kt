package com.example.birthdaycountdown.data

import org.json.JSONArray
import org.json.JSONObject

data class AppBackup(
    val countdownRecords: List<CountdownEntity>,
    val watchCategories: List<WatchCategoryEntity>,
    val watchRecords: List<WatchRecordEntity>
)

fun backupScopeDescription(): String = "备份包含生日、纪念日和追剧记录；不包含 AI 对话、生图记录、图片与参考图。"

object BackupCodec {
    private const val FORMAT_VERSION = 1

    fun encode(backup: AppBackup): String = JSONObject()
        .put("formatVersion", FORMAT_VERSION)
        .put("records", JSONArray().apply { backup.countdownRecords.forEach { put(it.toJson()) } })
        .put("watchCategories", JSONArray().apply { backup.watchCategories.forEach { put(it.toJson()) } })
        .put("watchRecords", JSONArray().apply { backup.watchRecords.forEach { put(it.toJson()) } })
        .toString()

    fun decode(content: String): AppBackup {
        val root = JSONObject(content)
        require(root.getInt("formatVersion") == FORMAT_VERSION) { "不支持的备份版本" }
        val countdownRecords = root.getJSONArray("records").let { entries -> List(entries.length()) { entries.getJSONObject(it).toRecord() } }
        val watchCategories = root.optJSONArray("watchCategories")?.let { entries -> List(entries.length()) { entries.getJSONObject(it).toWatchCategory() } }.orEmpty()
        val watchRecords = root.optJSONArray("watchRecords")?.let { entries -> List(entries.length()) { entries.getJSONObject(it).toWatchRecord() } }.orEmpty()
        return AppBackup(countdownRecords, watchCategories, watchRecords)
    }

    private fun CountdownEntity.toJson() = JSONObject()
        .put("id", id).put("type", type.name).put("name", name).put("dateTimeIso", dateTimeIso)
        .put("calendarType", calendarType.name).put("lunarYear", lunarYear).put("lunarMonth", lunarMonth).put("lunarDay", lunarDay)
        .put("lunarLeapMonth", lunarLeapMonth).put("reminderEnabled", reminderEnabled).put("reminderMinutesBefore", reminderMinutesBefore)
        .put("showYears", showYears).put("showMonths", showMonths).put("showDays", showDays).put("showHours", showHours)
        .put("showMinutes", showMinutes).put("showSeconds", showSeconds).put("showSolarDate", showSolarDate).put("showLunarDate", showLunarDate)
        .put("cardBackgroundColor", cardBackgroundColor).put("cardTextColor", cardTextColor).put("solarDisplayMask", solarDisplayMask)
        .put("lunarDisplayMask", lunarDisplayMask).put("countdownDisplayMask", countdownDisplayMask).put("cardGradientId", cardGradientId)
        .put("titleGradientId", titleGradientId).put("solarGradientId", solarGradientId).put("lunarGradientId", lunarGradientId)
        .put("countdownGradientId", countdownGradientId).put("titleTextColor", titleTextColor).put("solarTextColor", solarTextColor)
        .put("lunarTextColor", lunarTextColor).put("countdownTextColor", countdownTextColor).put("sortOrder", sortOrder).put("isPinned", isPinned)

    private fun WatchCategoryEntity.toJson() = JSONObject()
        .put("id", id).put("name", name).put("sortOrder", sortOrder)

    private fun WatchRecordEntity.toJson() = JSONObject()
        .put("id", id).put("title", title).put("categoryId", categoryId).put("currentEpisode", currentEpisode).put("sortOrder", sortOrder)

    private fun JSONObject.toRecord() = CountdownEntity(
        id = getLong("id"), type = RecordType.valueOf(getString("type")),
        name = getString("name").trim().also { require(it.isNotEmpty()) { "记录名称不能为空" } },
        dateTimeIso = getString("dateTimeIso"), calendarType = CalendarType.valueOf(getString("calendarType")),
        lunarYear = nullableInt("lunarYear"), lunarMonth = nullableInt("lunarMonth"), lunarDay = nullableInt("lunarDay"),
        lunarLeapMonth = getBoolean("lunarLeapMonth"), reminderEnabled = getBoolean("reminderEnabled"), reminderMinutesBefore = getInt("reminderMinutesBefore"),
        showYears = getBoolean("showYears"), showMonths = getBoolean("showMonths"), showDays = getBoolean("showDays"),
        showHours = getBoolean("showHours"), showMinutes = getBoolean("showMinutes"), showSeconds = getBoolean("showSeconds"),
        showSolarDate = getBoolean("showSolarDate"), showLunarDate = getBoolean("showLunarDate"),
        cardBackgroundColor = getInt("cardBackgroundColor"), cardTextColor = getInt("cardTextColor"),
        solarDisplayMask = getInt("solarDisplayMask"), lunarDisplayMask = getInt("lunarDisplayMask"), countdownDisplayMask = getInt("countdownDisplayMask"),
        cardGradientId = getString("cardGradientId"), titleGradientId = getString("titleGradientId"), solarGradientId = getString("solarGradientId"),
        lunarGradientId = getString("lunarGradientId"), countdownGradientId = getString("countdownGradientId"),
        titleTextColor = getInt("titleTextColor"), solarTextColor = getInt("solarTextColor"), lunarTextColor = getInt("lunarTextColor"),
        countdownTextColor = getInt("countdownTextColor"), sortOrder = getInt("sortOrder"), isPinned = getBoolean("isPinned")
    )

    private fun JSONObject.toWatchCategory() = WatchCategoryEntity(
        id = getLong("id"), name = getString("name"), sortOrder = getInt("sortOrder")
    )

    private fun JSONObject.toWatchRecord() = WatchRecordEntity(
        id = getLong("id"), title = getString("title"), categoryId = getLong("categoryId"),
        currentEpisode = getInt("currentEpisode"), sortOrder = getInt("sortOrder")
    )

    private fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else getInt(key)
}
