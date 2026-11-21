package com.wantstracker.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("wants_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(sp.getString("theme", "system") ?: "system")
    val theme: StateFlow<String> = _theme
    fun setTheme(v: String) {
        sp.edit().putString("theme", v).apply()
        _theme.value = v
    }

    private val _lock = MutableStateFlow(sp.getBoolean("lock", false))
    val lockEnabled: StateFlow<Boolean> = _lock
    fun setLockEnabled(v: Boolean) {
        sp.edit().putBoolean("lock", v).apply()
        _lock.value = v
    }

    private val _reminders = MutableStateFlow(sp.getBoolean("reminders", true))
    val remindersEnabled: StateFlow<Boolean> = _reminders
    fun setRemindersEnabled(v: Boolean) {
        sp.edit().putBoolean("reminders", v).apply()
        _reminders.value = v
    }

    var seeded: Boolean
        get() = sp.getBoolean("seeded", false)
        set(v) = sp.edit().putBoolean("seeded", v).apply()

    var askedNotifications: Boolean
        get() = sp.getBoolean("asked_notif", false)
        set(v) = sp.edit().putBoolean("asked_notif", v).apply()

    var lastQuarterNotified: String
        get() = sp.getString("last_quarter", "") ?: ""
        set(v) = sp.edit().putString("last_quarter", v).apply()

    var lastStaleNudge: Long
        get() = sp.getLong("last_stale", 0L)
        set(v) = sp.edit().putLong("last_stale", v).apply()
}
