package com.example.birthdaycountdown.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.birthdaycountdown.data.AppBackup
import com.example.birthdaycountdown.data.CountdownEntity
import com.example.birthdaycountdown.data.CountdownRepository
import com.example.birthdaycountdown.data.BackupCodec
import com.example.birthdaycountdown.data.WatchlistRepository
import com.example.birthdaycountdown.domain.DateFormatPreference
import com.example.birthdaycountdown.notifications.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

data class AppDisplaySettings(
    val showSolarDate: Boolean = true,
    val showLunarDate: Boolean = true,
    val showYears: Boolean = true,
    val showMonths: Boolean = true,
    val showDays: Boolean = true,
    val showHours: Boolean = true,
    val showMinutes: Boolean = true,
    val showSeconds: Boolean = true,
    val titleTextSize: Int = 20,
    val dateTextSize: Int = 14,
    val countdownTextSize: Int = 18,
    val titleBold: Boolean = true,
    val dateBold: Boolean = false,
    val countdownBold: Boolean = true,
    val cardLayoutStyle: CardLayoutStyle = CardLayoutStyle.STANDARD
)

enum class CardLayoutStyle(val label: String) { STANDARD("标准版"), COMPACT("紧凑版"), SIDEBAR("侧栏版") }

internal fun cardLayoutStyleLabels(): List<String> = CardLayoutStyle.entries.map { it.label }

internal fun parseCardLayoutStyle(value: String?): CardLayoutStyle =
    runCatching { CardLayoutStyle.valueOf(value.orEmpty()) }.getOrDefault(CardLayoutStyle.STANDARD)

enum class BottomNavIconId { CLOCK, CALENDAR_PLUS, USER, HEART, STAR, SETTINGS }

data class BottomNavItemSettings(
    val label: String,
    val icon: BottomNavIconId,
    val showIcon: Boolean = true,
    val showLabel: Boolean = true
) {
    fun withIconVisibility(visible: Boolean) = if (!visible && (!showLabel || label.isBlank())) this else copy(showIcon = visible)
    fun withLabelVisibility(visible: Boolean) = if (!visible && !showIcon) this else copy(showLabel = visible)
}

data class BottomNavSettings(
    val time: BottomNavItemSettings = BottomNavItemSettings("时间", BottomNavIconId.CLOCK),
    val add: BottomNavItemSettings = BottomNavItemSettings("添加时间", BottomNavIconId.CALENDAR_PLUS),
    val profile: BottomNavItemSettings = BottomNavItemSettings("我的", BottomNavIconId.USER),
    val ai: BottomNavItemSettings = BottomNavItemSettings("AI", BottomNavIconId.STAR)
)

val DEFAULT_BOTTOM_NAV_SETTINGS = BottomNavSettings()

class AppViewModel(
    private val repository: CountdownRepository,
    private val watchlistRepository: WatchlistRepository,
    private val scheduler: ReminderScheduler,
    private val preferences: AppPreferences
) : ViewModel() {
    val records: StateFlow<List<CountdownEntity>> = repository.records.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _now = MutableStateFlow(Instant.now())
    val now: StateFlow<Instant> = _now.asStateFlow()
    val format: StateFlow<DateFormatPreference> = preferences.format.stateIn(viewModelScope, SharingStarted.Eagerly, DateFormatPreference.CHINESE)
    private val _displaySettings = MutableStateFlow(preferences.readDisplaySettings())
    val displaySettings: StateFlow<AppDisplaySettings> = _displaySettings.asStateFlow()
    private val _bottomNavSettings = MutableStateFlow(preferences.readBottomNavSettings())
    val bottomNavSettings: StateFlow<BottomNavSettings> = _bottomNavSettings.asStateFlow()
    private var ticker: Job = viewModelScope.launch { while (true) { _now.value = Instant.now(); delay(1000) } }

    fun save(record: CountdownEntity) = viewModelScope.launch {
        val id = repository.save(record)
        val saved = record.copy(id = if (record.id == 0L) id else record.id)
        if (saved.reminderEnabled) scheduler.schedule(saved) else scheduler.cancel(saved.id)
    }

    fun delete(record: CountdownEntity) = viewModelScope.launch { repository.delete(record); scheduler.cancel(record.id) }
    fun restore(record: CountdownEntity) = viewModelScope.launch {
        val id = repository.save(record.copy(id = 0L))
        val restored = record.copy(id = id)
        if (restored.reminderEnabled) scheduler.schedule(restored)
    }
    fun setPinned(record: CountdownEntity, pinned: Boolean) = save(record.copy(isPinned = pinned))
    fun reorder(records: List<CountdownEntity>) = viewModelScope.launch { repository.reorder(records) }
    fun setFormat(value: DateFormatPreference) { preferences.setFormat(value) }
    fun setDisplaySettings(value: AppDisplaySettings) {
        _displaySettings.value = value
        preferences.setDisplaySettings(value)
    }
    fun setBottomNavSettings(value: BottomNavSettings) {
        _bottomNavSettings.value = value
        preferences.setBottomNavSettings(value)
    }
    fun resetBottomNavSettings() = setBottomNavSettings(DEFAULT_BOTTOM_NAV_SETTINGS)

    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        BackupCodec.encode(
            AppBackup(
                repository.allRecords(),
                watchlistRepository.allCategories(),
                watchlistRepository.allRecords(),
                watchlistRepository.allWatchStatuses()
            )
        )
    }

    suspend fun importBackup(content: String): Int = withContext(Dispatchers.IO) {
        val backup = BackupCodec.decode(content)
        watchlistRepository.ensureBuiltInStatuses()
        val imported = repository.import(backup.countdownRecords)
        imported.forEach { record ->
            if (record.reminderEnabled) scheduler.schedule(record) else scheduler.cancel(record.id)
        }
        watchlistRepository.import(backup.watchCategories, backup.watchRecords, backup.watchStatuses)
        imported.size + backup.watchRecords.size
    }
}

class AppPreferences(private val prefs: android.content.SharedPreferences) {
    val format: Flow<DateFormatPreference> = flow {
        emit(parseDateFormatPreference(prefs.getString("date_format", null)))
        while (true) { kotlinx.coroutines.delay(500); emit(parseDateFormatPreference(prefs.getString("date_format", null))) }
    }
    fun setFormat(value: DateFormatPreference) { prefs.edit().putString("date_format", value.name).apply() }

    fun setDisplaySettings(value: AppDisplaySettings) {
        prefs.edit()
            .putBoolean("show_solar_date", value.showSolarDate)
            .putBoolean("show_lunar_date", value.showLunarDate)
            .putBoolean("show_years", value.showYears)
            .putBoolean("show_months", value.showMonths)
            .putBoolean("show_days", value.showDays)
            .putBoolean("show_hours", value.showHours)
            .putBoolean("show_minutes", value.showMinutes)
            .putBoolean("show_seconds", value.showSeconds)
            .putInt("title_text_size", value.titleTextSize)
            .putInt("date_text_size", value.dateTextSize)
            .putInt("countdown_text_size", value.countdownTextSize)
            .putBoolean("title_bold", value.titleBold)
            .putBoolean("date_bold", value.dateBold)
            .putBoolean("countdown_bold", value.countdownBold)
            .putString("card_layout_style", value.cardLayoutStyle.name)
            .apply()
    }

    fun readDisplaySettings() = AppDisplaySettings(
        showSolarDate = prefs.getBoolean("show_solar_date", true),
        showLunarDate = prefs.getBoolean("show_lunar_date", true),
        showYears = prefs.getBoolean("show_years", true),
        showMonths = prefs.getBoolean("show_months", true),
        showDays = prefs.getBoolean("show_days", true),
        showHours = prefs.getBoolean("show_hours", true),
        showMinutes = prefs.getBoolean("show_minutes", true),
        showSeconds = prefs.getBoolean("show_seconds", true),
        titleTextSize = prefs.getInt("title_text_size", 20).coerceIn(14, 32),
        dateTextSize = prefs.getInt("date_text_size", 14).coerceIn(10, 24),
        countdownTextSize = prefs.getInt("countdown_text_size", 18).coerceIn(12, 30),
        titleBold = prefs.getBoolean("title_bold", true),
        dateBold = prefs.getBoolean("date_bold", false),
        countdownBold = prefs.getBoolean("countdown_bold", true),
        cardLayoutStyle = parseCardLayoutStyle(prefs.getString("card_layout_style", null))
    )

    fun setBottomNavSettings(value: BottomNavSettings) {
        val editor = prefs.edit()
        writeNavItem(editor, "time", value.time)
        writeNavItem(editor, "add", value.add)
        writeNavItem(editor, "profile", value.profile)
        writeNavItem(editor, "ai", value.ai)
        editor.apply()
    }

    fun readBottomNavSettings() = BottomNavSettings(
        time = readNavItem("time", DEFAULT_BOTTOM_NAV_SETTINGS.time),
        add = readNavItem("add", DEFAULT_BOTTOM_NAV_SETTINGS.add),
        profile = readNavItem("profile", DEFAULT_BOTTOM_NAV_SETTINGS.profile),
        ai = readNavItem("ai", DEFAULT_BOTTOM_NAV_SETTINGS.ai)
    )

    private fun writeNavItem(editor: android.content.SharedPreferences.Editor, key: String, value: BottomNavItemSettings) {
        editor.putString("nav_${key}_label", value.label.take(4))
            .putString("nav_${key}_icon", value.icon.name)
            .putBoolean("nav_${key}_show_icon", value.showIcon)
            .putBoolean("nav_${key}_show_label", value.showLabel)
    }

    private fun readNavItem(key: String, default: BottomNavItemSettings): BottomNavItemSettings {
        val icon = runCatching { BottomNavIconId.valueOf(prefs.getString("nav_${key}_icon", default.icon.name)!!) }.getOrDefault(default.icon)
        val item = BottomNavItemSettings(
            label = prefs.getString("nav_${key}_label", default.label).orEmpty().ifBlank { default.label }.take(4),
            icon = icon,
            showIcon = prefs.getBoolean("nav_${key}_show_icon", default.showIcon),
            showLabel = prefs.getBoolean("nav_${key}_show_label", default.showLabel)
        )
        return if (!item.showIcon && !item.showLabel) item.copy(showIcon = true) else item
    }
}

internal fun parseDateFormatPreference(value: String?): DateFormatPreference =
    runCatching { DateFormatPreference.valueOf(value.orEmpty()) }.getOrDefault(DateFormatPreference.CHINESE)
