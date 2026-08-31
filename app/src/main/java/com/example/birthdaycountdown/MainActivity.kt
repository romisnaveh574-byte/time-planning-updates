package com.example.birthdaycountdown

import android.Manifest
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.birthdaycountdown.data.AppDatabase
import com.example.birthdaycountdown.data.CountdownRepository
import com.example.birthdaycountdown.data.WatchlistRepository
import com.example.birthdaycountdown.notifications.ReminderScheduler
import com.example.birthdaycountdown.ui.AppPreferences
import com.example.birthdaycountdown.ui.AppViewModel
import com.example.birthdaycountdown.ui.AppNav
import com.example.birthdaycountdown.ui.TimePlanningTheme
import com.example.birthdaycountdown.ui.WatchlistViewModel
import com.example.birthdaycountdown.data.AiHistoryRepository
import com.example.birthdaycountdown.update.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.create(applicationContext)
        val watchlistRepository = WatchlistRepository(db, db.watchlistDao())
        val viewModel = AppViewModel(CountdownRepository(db.countdownDao()), watchlistRepository, ReminderScheduler(this), AppPreferences(getSharedPreferences("settings", MODE_PRIVATE)))
        val watchlistViewModel = WatchlistViewModel(watchlistRepository)
        val aiHistoryRepository = AiHistoryRepository(db.aiHistoryDao(), applicationContext)
        setContent {
            TimePlanningTheme {
                AppNav(viewModel, watchlistViewModel, aiHistoryRepository, onRequestNotifications = {
                    if (android.os.Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
        }
        val updatePrefs = UpdatePreferences(getSharedPreferences("settings", MODE_PRIVATE))
        val day = LocalDate.now().toEpochDay()
        if (updatePrefs.shouldCheckToday(day)) {
            updatePrefs.markChecked(day)
            lifecycleScope.launch(Dispatchers.IO) {
                UpdateChecker(BuildConfig.UPDATE_REPOSITORY_OWNER, BuildConfig.UPDATE_REPOSITORY_NAME).check(AppVersion(BuildConfig.VERSION_CODE.toLong(), BuildConfig.VERSION_NAME))
            }
        }
    }
}
