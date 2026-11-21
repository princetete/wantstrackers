@file:OptIn(ExperimentalMaterial3Api::class)

package com.wantstracker.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wantstracker.app.data.WantEntity
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.coolingDaysLeft
import com.wantstracker.app.domain.formatDate
import com.wantstracker.app.domain.formatInr
import com.wantstracker.app.domain.isOverdue
import com.wantstracker.app.domain.plannedKey
import com.wantstracker.app.ui.ConfirmDialog
import com.wantstracker.app.ui.MainViewModel
import com.wantstracker.app.ui.Routes
import com.wantstracker.app.ui.StatusPill
import com.wantstracker.app.ui.WantThumb
import com.wantstracker.app.ui.rememberStatusRequester
import com.wantstracker.app.ui.theme.statusColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@Composable
fun DetailScreen(vm: MainViewModel, nav: NavController, id: Long) {
    val flow = remember(id) { vm.want(id) }
    val logsFlow = remember(id) { vm.logs(id) }
    val want by flow.collectAsStateWithLifecycle(initialValue = null)
    val logs by logsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by vm.categories.collectAsStateWithLifecycle()
    val requester = rememberStatusRequester(vm)
    val uriHandler = LocalUriHandler.current
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Want details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.form(id)) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        val w: WantEntity? = want
        if (w == null) {
            Spacer(Modifier.padding(padding).fillMaxSize())
            return@Scaffold
        }
        val category = categories.firstOrNull { it.id == w.categoryId }
        val cooling = w.coolingDaysLeft()

        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WantThumb(w, category, size = 76)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(w.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (w.price > 0) formatInr(w.price) else "No price set",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        StatusPill(w.status)
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("ABOUT THIS WANT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    InfoRow("Category", category?.let { "${it.emoji} ${it.name}" } ?: "—")
                    InfoRow("Priority", "${w.priority.emoji} ${w.priority.label}")
                    InfoRow("Planned", w.plannedKey?.label ?: "Unplanned")
                    InfoRow("Added", formatDate(w.addedAt))
                }
            }

            if (w.status == Status.BOUGHT || w.status == Status.FAILED) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (w.status == Status.BOUGHT) "Purchase details" else "Outcome details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (w.status == Status.BOUGHT) {
                            InfoRow("Bought", w.boughtAt?.let { formatDate(it) } ?: "—")
                            InfoRow("Listed", if (w.price > 0) formatInr(w.price) else "—")
                            InfoRow("Paid", w.pricePaid?.let { formatInr(it) } ?: "—")
                        } else {
                            InfoRow("Failed", w.failedAt?.let { formatDate(it) } ?: "—")
                            InfoRow("Reason", w.failReason.ifBlank { "—" })
                        }
                    }
                }
            }

            if (w.link.isNotBlank()) {
                val url = if (w.link.startsWith("http")) w.link else "https://${w.link}"
                Text(
                    w.link,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickableLink { runCatching { uriHandler.openUri(url) } }
                )
            }
            if (w.notes.isNotBlank()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(w.notes, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Text("Status", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Status.entries.forEach { s ->
                    FilterChip(
                        selected = w.status == s,
                        onClick = { if (w.status != s) requester.request(w, s) },
                        label = { Text("${s.emoji} ${s.label}") }
                    )
                }
            }

            if (w.isOverdue()) {
                val now = QuarterKey.now()
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Overdue — planned for ${w.plannedKey?.label}",
                            Modifier.weight(1f),
                            color = statusColor(Status.FAILED)
                        )
                        TextButton(onClick = { vm.moveToQuarter(w.id, now) }) { Text("Move to ${now.label}") }
                    }
                }
            }

            if (w.status.isOpen) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⏳ Cooling-off", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (w.coolingUntil == null) {
                            Text(
                                "Wait before you buy. You'll get a reminder when the time is up.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(7, 14, 30).forEach { d ->
                                    OutlinedButton(onClick = { vm.setCooling(w.id, d) }) { Text("$d days") }
                                }
                            }
                        } else {
                            val until = formatDate(w.coolingUntil)
                            Text(
                                if ((cooling ?: 0) > 0) "$cooling day${if (cooling == 1) "" else "s"} left (until $until)"
                                else "✅ Cooling-off is over ($until). Still want it?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { vm.setCooling(w.id, null) }) { Text("Clear timer") }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Activity timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    logs.forEachIndexed { index, l ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(l.status.emoji)
                            Spacer(Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (l.note == "Added") "Added to wishlist" else l.status.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    formatDate(l.at) + if (l.note.isNotBlank() && l.note != "Added") " · ${l.note}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    requester.Dialogs()

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this want?",
            text = "This also removes its history. It can't be undone.",
            confirmLabel = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                vm.deleteWant(id) { nav.popBackStack() }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            Modifier.width(88.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun Modifier.clickableLink(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
