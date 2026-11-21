@file:OptIn(ExperimentalMaterial3Api::class)

package com.wantstracker.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wantstracker.app.backup.CsvImport
import com.wantstracker.app.domain.formatInr
import com.wantstracker.app.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ImportScreen(vm: MainViewModel, nav: NavController) {
    var text by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val parsed = remember(text) { CsvImport.parse(text) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { text = vm.readText(uri) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import old wants", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Format — one want per line", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "name, price, quarter, status, category, note",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "iPhone 15, 79900, Q2 2025, bought, Tech\n" +
                            "Goa trip, 25000, Q4 2025, failed, Travel, Too expensive\n" +
                            "Standing desk, 18000, , not started, Home",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Only the name is required. Status can be: not started, in progress, bought or failed. " +
                            "Quarter can be left empty (unplanned). Missing categories are created for you. " +
                            "For failed wants, the last column is the reason.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    result = null
                },
                label = { Text("Paste your list here") },
                minLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Choose a CSV / text file") }

            if (text.isNotBlank()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${parsed.rows.size} want${if (parsed.rows.size == 1) "" else "s"} ready to import" +
                                if (parsed.rows.isNotEmpty()) " · ${formatInr(parsed.rows.sumOf { it.price })}" else "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        parsed.rows.take(5).forEach {
                            Text(
                                "• ${it.name} — ${it.status.label}" + (it.quarter?.let { q -> " — ${q.label}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (parsed.rows.size > 5) Text("…and ${parsed.rows.size - 5} more", style = MaterialTheme.typography.bodySmall)
                        parsed.warnings.take(6).forEach {
                            Text("⚠️ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            Button(
                enabled = parsed.rows.isNotEmpty(),
                onClick = {
                    vm.importRows(parsed.rows) { n ->
                        result = "Imported $n want${if (n == 1) "" else "s"} ✅"
                        text = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Import ${parsed.rows.size} wants") }

            result?.let {
                Row { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}
