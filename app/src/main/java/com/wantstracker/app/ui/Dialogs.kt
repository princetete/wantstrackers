@file:OptIn(ExperimentalMaterial3Api::class)

package com.wantstracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.wantstracker.app.data.CategoryEntity
import com.wantstracker.app.data.Defaults
import com.wantstracker.app.data.WantEntity
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.formatDate
import com.wantstracker.app.domain.localToPicker
import com.wantstracker.app.domain.pickerToLocal
import com.wantstracker.app.domain.plainNumber

/** Handles the extra questions some status changes need (price paid, failure reason). */
class StatusRequester(private val vm: MainViewModel) {
    var boughtFor by mutableStateOf<WantEntity?>(null)
    var failedFor by mutableStateOf<WantEntity?>(null)

    fun request(w: WantEntity, s: Status) {
        when (s) {
            Status.BOUGHT -> boughtFor = w
            Status.FAILED -> failedFor = w
            else -> vm.changeStatus(w.id, s)
        }
    }

    @Composable
    fun Dialogs() {
        boughtFor?.let { w ->
            BoughtDialog(w, onDismiss = { boughtFor = null }) { price, at ->
                vm.changeStatus(w.id, Status.BOUGHT, pricePaid = price, at = at)
                boughtFor = null
            }
        }
        failedFor?.let { w ->
            FailedDialog(w, onDismiss = { failedFor = null }) { reason ->
                vm.changeStatus(w.id, Status.FAILED, reason = reason)
                failedFor = null
            }
        }
    }
}

@Composable
fun rememberStatusRequester(vm: MainViewModel): StatusRequester = remember(vm) { StatusRequester(vm) }

@Composable
fun DateField(label: String, millis: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Filled.DateRange, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("$label: ${formatDate(millis)}")
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = localToPicker(millis))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(pickerToLocal(it)) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
fun BoughtDialog(want: WantEntity, onDismiss: () -> Unit, onConfirm: (Double, Long) -> Unit) {
    var price by remember { mutableStateOf(if (want.price > 0) plainNumber(want.price) else "") }
    var at by remember { mutableStateOf(System.currentTimeMillis()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🟢 Bought \"${want.name}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price paid (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                DateField("Bought on", at, { at = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(price.replace(",", "").toDoubleOrNull() ?: want.price, at)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FailedDialog(want: WantEntity, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔴 Let go of \"${want.name}\"?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Why? (required)") },
                    minLines = 2
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Too expensive", "Out of stock", "Changed my mind", "Found alternative").forEach { s ->
                        AssistChip(onClick = { reason = s }, label = { Text(s) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = reason.isNotBlank(), onClick = { onConfirm(reason.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CategoryEditDialog(
    initial: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, color: Long) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🎁") }
    var color by remember { mutableStateOf(initial?.color ?: Defaults.colors[0]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true
                )
                OutlinedTextField(
                    value = emoji, onValueChange = { emoji = it.take(4) },
                    label = { Text("Emoji") }, singleLine = true
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Defaults.emojiChoices.forEach { e ->
                        Text(e, fontSize = 24.sp, modifier = Modifier.clickable { emoji = e }.padding(4.dp))
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Defaults.colors.forEach { c ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(Color(c), CircleShape)
                                .then(
                                    if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(name.trim(), emoji.ifBlank { "🎁" }, color)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
