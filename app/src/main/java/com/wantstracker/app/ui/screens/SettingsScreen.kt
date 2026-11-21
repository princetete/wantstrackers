@file:OptIn(ExperimentalMaterial3Api::class)

package com.wantstracker.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wantstracker.app.LockState
import com.wantstracker.app.ui.MainBottomBar
import com.wantstracker.app.ui.MainViewModel
import com.wantstracker.app.ui.Routes

object LockAuth {
    const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
}

@Composable
fun SettingsScreen(vm: MainViewModel, nav: NavController) {
    val ctx = LocalContext.current
    val theme by vm.prefs.theme.collectAsStateWithLifecycle()
    val reminders by vm.prefs.remindersEnabled.collectAsStateWithLifecycle()
    val lock by vm.prefs.lockEnabled.collectAsStateWithLifecycle()
    val wants by vm.wants.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri, wants.orEmpty(), categories) { toast(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) },
        bottomBar = { MainBottomBar(nav, Routes.SETTINGS) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Section("Appearance") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                        FilterChip(selected = theme == key, onClick = { vm.prefs.setTheme(key) }, label = { Text(label) })
                    }
                }
            }

            Section("Reminders") {
                SwitchRow(
                    "Reminders",
                    "New-quarter summary, cooling-off timers, and a nudge for wants stuck in progress",
                    reminders
                ) { on ->
                    vm.prefs.setRemindersEnabled(on)
                    if (on && Build.VERSION.SDK_INT >= 33 &&
                        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            Section("Security") {
                SwitchRow("App lock", "Ask for fingerprint, face or screen lock when opening the app", lock) { on ->
                    if (on) {
                        val ok = BiometricManager.from(ctx).canAuthenticate(LockAuth.AUTHENTICATORS) ==
                            BiometricManager.BIOMETRIC_SUCCESS
                        if (ok) {
                            LockState.unlocked = true
                            vm.prefs.setLockEnabled(true)
                        } else {
                            toast("Set up a screen lock or fingerprint in your phone settings first")
                        }
                    } else {
                        vm.prefs.setLockEnabled(false)
                    }
                }
            }

            Section("Manage") {
                NavRow("🗂️  Categories") { nav.navigate(Routes.CATEGORIES) }
                HorizontalDivider()
                NavRow("📥  Import old wants (CSV / paste)") { nav.navigate(Routes.IMPORT) }
            }

            Section("Import & Export") {
                Text(
                    "Export your wants as a CSV spreadsheet. It includes names, prices, links, notes, categories, priority, planned year and quarter, status and dates. No photos are included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(enabled = wants != null, onClick = {
                    exportLauncher.launch("wants-tracker-${System.currentTimeMillis()}.csv")
                }) { Text("Export CSV") }
                NavRow("📥  Import wants from CSV") { nav.navigate(Routes.IMPORT) }
            }

            Section("Widget") {
                Text(
                    "Long-press your home screen, choose Widgets, then add “Wants Tracker” to see your top want and progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "Wants Tracker 1.0 · All data stays on this device. No account, no ads, no tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }

}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp)
    )
}
