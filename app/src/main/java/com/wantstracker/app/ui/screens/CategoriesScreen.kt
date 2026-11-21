@file:OptIn(ExperimentalMaterial3Api::class)

package com.wantstracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wantstracker.app.data.CategoryEntity
import com.wantstracker.app.ui.CategoryEditDialog
import com.wantstracker.app.ui.ConfirmDialog
import com.wantstracker.app.ui.EmptyState
import com.wantstracker.app.ui.MainViewModel

@Composable
fun CategoriesScreen(vm: MainViewModel, nav: NavController) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val wants by vm.wants.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New category")
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState("🗂️", "No categories", "Tap ＋ to create one, e.g. “Gaming Setup”.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories, key = { it.id }) { c ->
                    val count = wants?.count { it.categoryId == c.id } ?: 0
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Color(c.color).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Text(c.emoji, fontSize = 22.sp) }
                            Spacer(Modifier.width(12.dp))
                            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "$count want${if (count == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editing = c }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { deleting = c }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        CategoryEditDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { n, e, col ->
                vm.addCategory(n, e, col)
                creating = false
            }
        )
    }
    editing?.let { c ->
        CategoryEditDialog(
            initial = c,
            onDismiss = { editing = null },
            onSave = { n, e, col ->
                vm.updateCategory(c.copy(name = n, emoji = e, color = col))
                editing = null
            }
        )
    }
    deleting?.let { c ->
        ConfirmDialog(
            title = "Delete “${c.name}”?",
            text = "Wants in this category stay, but become uncategorized.",
            confirmLabel = "Delete",
            onDismiss = { deleting = null },
            onConfirm = {
                vm.deleteCategory(c)
                deleting = null
            }
        )
    }
}
