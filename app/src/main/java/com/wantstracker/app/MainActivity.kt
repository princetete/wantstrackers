package com.wantstracker.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wantstracker.app.ui.AppNav
import com.wantstracker.app.ui.MainViewModel
import com.wantstracker.app.ui.screens.LockScreen
import com.wantstracker.app.ui.theme.WantsTheme

object LockState {
    var unlocked by mutableStateOf(false)
    var stoppedAt: Long = 0L
}

class MainActivity : FragmentActivity() {
    private val vm: MainViewModel by viewModels()

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33 && !vm.prefs.askedNotifications) {
            vm.prefs.askedNotifications = true
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val theme by vm.prefs.theme.collectAsStateWithLifecycle()
            val lockOn by vm.prefs.lockEnabled.collectAsStateWithLifecycle()
            val dark = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            WantsTheme(dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (lockOn && !LockState.unlocked) {
                        LockScreen(activity = this@MainActivity, onUnlock = { LockState.unlocked = true })
                    } else {
                        AppNav(vm)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) LockState.stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Re-lock only after 30s in the background so photo/file pickers don't trigger it.
        val t = LockState.stoppedAt
        if (t > 0 && SystemClock.elapsedRealtime() - t > 30_000L) LockState.unlocked = false
        LockState.stoppedAt = 0L
    }
}
