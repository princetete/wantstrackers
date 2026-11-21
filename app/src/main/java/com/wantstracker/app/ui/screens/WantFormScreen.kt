@file:OptIn(ExperimentalMaterial3Api::class)

package com.wantstracker.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wantstracker.app.data.CategoryEntity
import com.wantstracker.app.data.WantEntity
import com.wantstracker.app.domain.Priority
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.plainNumber
import com.wantstracker.app.ui.CategoryEditDialog
import com.wantstracker.app.ui.DateField
import com.wantstracker.app.ui.MainViewModel
import com.wantstracker.app.ui.QuarterPicker

@Composable
fun WantFormScreen(vm: MainViewModel, nav: NavController, id: Long) {
    val loaded by produceState<WantEntity?>(null, id) {
        value = if (id == 0L) null else vm.getWant(id)
    }
    if (id == 0L || loaded != null) {
        FormContent(vm, nav, loaded)
    } else {
        Scaffold { Spacer(Modifier.padding(it).fillMaxSize()) }
    }
}

@Composable
private fun FormContent(vm: MainViewModel, nav: NavController, initial: WantEntity?) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val isNew = initial == null

    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var priceText by rememberSaveable {
        mutableStateOf(initial?.price?.takeIf { it > 0 }?.let { plainNumber(it) } ?: "")
    }
    var link by rememberSaveable { mutableStateOf(initial?.link ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(initial?.categoryId) }
    var priority by rememberSaveable { mutableStateOf(initial?.priority ?: Priority.WANT) }
    var plannedYear by rememberSaveable { mutableStateOf<Int?>(initial?.plannedYear) }
    var plannedQuarter by rememberSaveable { mutableStateOf<Int?>(initial?.plannedQuarter) }
    var status by rememberSaveable { mutableStateOf(initial?.status ?: Status.NOT_STARTED) }
    var paidText by rememberSaveable {
        mutableStateOf(initial?.pricePaid?.let { plainNumber(it) } ?: "")
    }
    var doneAt by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var failReason by rememberSaveable { mutableStateOf("") }
    var showNewCategory by remember { mutableStateOf(false) }



    val canSave = name.isNotBlank() && !(isNew && status == Status.FAILED && failReason.isBlank())

    fun save() {
        val price = priceText.replace(",", "").toDoubleOrNull() ?: 0.0
        val now = System.currentTimeMillis()
        if (initial != null) {
            vm.updateWant(
                initial.copy(
                    name = name.trim(),
                    price = price,
                    link = link.trim(),
                    notes = notes.trim(),
                    priority = priority,
                    categoryId = categoryId,
                    plannedYear = plannedYear,
                    plannedQuarter = plannedQuarter,
                    pricePaid = if (initial.status == Status.BOUGHT) {
                        paidText.replace(",", "").toDoubleOrNull() ?: initial.pricePaid
                    } else initial.pricePaid
                )
            ) { nav.popBackStack() }
        } else {
            val py = plannedYear
            val pq = plannedQuarter
            val planned = if (py != null && pq != null) QuarterKey(py, pq) else null
            val baseAdded = planned?.startMillis()?.takeIf { it < now } ?: now
            val closed = status == Status.BOUGHT || status == Status.FAILED
            val addedAt = if (closed) minOf(baseAdded, doneAt) else baseAdded
            vm.addWant(
                WantEntity(
                    name = name.trim(),
                    price = price,
                    link = link.trim(),
                    notes = notes.trim(),
                    priority = priority,
                    categoryId = categoryId,
                    plannedYear = plannedYear,
                    plannedQuarter = plannedQuarter,
                    status = status,
                    failReason = if (status == Status.FAILED) failReason.trim() else "",
                    pricePaid = if (status == Status.BOUGHT) (paidText.replace(",", "").toDoubleOrNull() ?: price) else null,
                    addedAt = addedAt,
                    boughtAt = if (status == Status.BOUGHT) doneAt else null,
                    failedAt = if (status == Status.FAILED) doneAt else null,
                    lastStatusChangeAt = if (closed) doneAt else if (status == Status.IN_PROGRESS) now else addedAt
                )
            ) { nav.popBackStack() }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(if (isNew) "Add want" else "Edit want", fontWeight = FontWeight.Bold)
                        Text("Keep your wishlist organised", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { TextButton(enabled = canSave, onClick = { save() }) { Text("Save") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormSection("The want", "Start with a name and an estimated price.") {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("What do you want?") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = priceText, onValueChange = { priceText = it },
                    label = { Text("Price (₹)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            FormSection("Category", "Group similar wants together.") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = categoryId == null, onClick = { categoryId = null }, label = { Text("None") })
                    categories.forEach { c ->
                        FilterChip(
                            selected = categoryId == c.id,
                            onClick = { categoryId = c.id },
                            label = { Text("${c.emoji} ${c.name}") }
                        )
                    }
                    FilterChip(selected = false, onClick = { showNewCategory = true }, label = { Text("＋ New") })
                }
            }

            FormSection("Priority", "Choose how important this want is.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text("${p.emoji} ${p.label}") }
                        )
                    }
                }
            }

            FormSection("Plan", "Choose when you want to get it.") {
                QuarterPicker(plannedYear, plannedQuarter) { y, q ->
                    plannedYear = y
                    plannedQuarter = q
                }
            }

            if (isNew) {
                FormSection("Progress", "Set the current status of this want.") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Status.entries.forEach { statusOption ->
                            FilterChip(
                                selected = status == statusOption,
                                onClick = { status = statusOption },
                                label = { Text("${statusOption.emoji} ${statusOption.label}") }
                            )
                        }
                    }
                    if (status == Status.BOUGHT) {
                        OutlinedTextField(
                            value = paidText, onValueChange = { paidText = it },
                            label = { Text("Price paid (₹) — blank = listed price") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    if (status == Status.BOUGHT || status == Status.FAILED) {
                        DateField(
                            if (status == Status.BOUGHT) "Bought on" else "Failed on",
                            doneAt, { doneAt = it }
                        )
                    }
                    if (status == Status.FAILED) {
                        OutlinedTextField(
                            value = failReason, onValueChange = { failReason = it },
                            label = { Text("Why did it fail? (required)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            } else if (initial?.status == Status.BOUGHT) {
                FormSection("Purchase", "Record what you actually paid.") {
                    OutlinedTextField(
                        value = paidText, onValueChange = { paidText = it },
                        label = { Text("Price paid (₹)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            FormSection("Extra details", "Add a product link or anything you want to remember.") {
                OutlinedTextField(
                    value = link, onValueChange = { link = it },
                    label = { Text("Link (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showNewCategory) {
        CategoryEditDialog(
            initial = null,
            onDismiss = { showNewCategory = false },
            onSave = { n, e, c ->
                vm.addCategory(n, e, c) { newId -> categoryId = newId }
                showNewCategory = false
            }
        )
    }
}

@Composable
private fun FormSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}
