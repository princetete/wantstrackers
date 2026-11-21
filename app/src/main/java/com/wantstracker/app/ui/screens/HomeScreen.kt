@file:OptIn(ExperimentalMaterial3Api::class)

package com.wantstracker.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wantstracker.app.data.CategoryEntity
import com.wantstracker.app.data.WantEntity
import com.wantstracker.app.domain.Priority
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.effectiveKey
import com.wantstracker.app.domain.formatInr
import com.wantstracker.app.domain.plannedKey
import com.wantstracker.app.ui.EmptyState
import com.wantstracker.app.ui.MainBottomBar
import com.wantstracker.app.ui.MainViewModel
import com.wantstracker.app.ui.Routes
import com.wantstracker.app.ui.SectionHeader
import com.wantstracker.app.ui.StatusRequester
import com.wantstracker.app.ui.SummaryCard
import com.wantstracker.app.ui.WantCard
import com.wantstracker.app.ui.rememberStatusRequester
import com.wantstracker.app.ui.theme.statusColor

private enum class SortMode(val label: String) {
    QUARTER("Planned quarter"),
    NEWEST("Newest first"),
    PRICE_HIGH("Price: high to low"),
    PRICE_LOW("Price: low to high"),
    PRIORITY("Priority")
}

@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val wantsOrNull by vm.wants.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val requester = rememberStatusRequester(vm)

    var board by rememberSaveable { mutableStateOf(false) }
    var statusFilter by rememberSaveable { mutableStateOf<Status?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var yearFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    var priorityFilter by rememberSaveable { mutableStateOf<Priority?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortMode.QUARTER) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val wants = wantsOrNull ?: emptyList()
    val catById = categories.associateBy { it.id }
    val thisQuarter = QuarterKey.now()
    val years = (wants.map { it.effectiveKey.year } + thisQuarter.year).distinct().sortedDescending()

    val base = wants
        .filter { categoryFilter == null || it.categoryId == categoryFilter }
        .filter { yearFilter == null || it.effectiveKey.year == yearFilter }
        .filter { priorityFilter == null || it.priority == priorityFilter }
        .filter { query.isBlank() || it.name.contains(query, true) || it.notes.contains(query, true) }
    val filtered = base.filter { statusFilter == null || it.status == statusFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search wants") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text("Wants Tracker", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        searching = !searching
                        if (!searching) query = ""
                    }) {
                        Icon(
                            if (searching) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = { board = !board }) {
                        Icon(
                            if (board) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.ViewColumn,
                            contentDescription = if (board) "List view" else "Board view"
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Sort and filter")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            Text(
                                "Sort by",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                            SortMode.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text((if (sort == m) "✓ " else "    ") + m.label) },
                                    onClick = {
                                        sort = m
                                        menuOpen = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            Text(
                                "Priority",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                            DropdownMenuItem(
                                text = { Text((if (priorityFilter == null) "✓ " else "    ") + "All") },
                                onClick = {
                                    priorityFilter = null
                                    menuOpen = false
                                }
                            )
                            Priority.entries.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text((if (priorityFilter == p) "✓ " else "    ") + "${p.emoji} ${p.label}") },
                                    onClick = {
                                        priorityFilter = p
                                        menuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = { MainBottomBar(nav, Routes.HOME) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.form(0)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add want") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ChipRow {
                FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All") })
                Status.entries.forEach { s ->
                    FilterChip(
                        selected = statusFilter == s,
                        onClick = { statusFilter = if (statusFilter == s) null else s },
                        label = { Text("${s.emoji} ${s.label}") }
                    )
                }
            }
            ChipRow {
                FilterChip(selected = yearFilter == null, onClick = { yearFilter = null }, label = { Text("All years") })
                years.forEach { y ->
                    FilterChip(
                        selected = yearFilter == y,
                        onClick = { yearFilter = if (yearFilter == y) null else y },
                        label = { Text("$y") }
                    )
                }
            }
            if (categories.isNotEmpty()) {
                ChipRow {
                    categories.forEach { c ->
                        FilterChip(
                            selected = categoryFilter == c.id,
                            onClick = { categoryFilter = if (categoryFilter == c.id) null else c.id },
                            label = { Text("${c.emoji} ${c.name}") }
                        )
                    }
                }
            }

            when {
                wantsOrNull == null -> Spacer(Modifier.fillMaxSize())
                wants.isEmpty() -> EmptyState("🛍️", "Nothing here yet", "Tap “Add want” to start your wishlist.")
                filtered.isEmpty() && !board -> EmptyState("🔍", "No matches", "Try clearing some filters.")
                board -> BoardView(base, catById, nav, vm, requester)
                else -> ListView(wants, filtered, catById, nav, vm, requester, sort, thisQuarter)
            }
        }
    }
    requester.Dialogs()
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun ListView(
    all: List<WantEntity>,
    filtered: List<WantEntity>,
    catById: Map<Long, CategoryEntity>,
    nav: NavController,
    vm: MainViewModel,
    requester: StatusRequester,
    sort: SortMode,
    thisQuarter: QuarterKey
) {
    val pending = filtered.filter { it.status.isOpen }.sumOf { it.price }
    val quarterWants = all.filter { it.plannedKey == thisQuarter }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (quarterWants.isNotEmpty()) {
            item(key = "quarter_summary") {
                SummaryCard(
                    quarterLabel = thisQuarter.label,
                    spent = quarterWants.filter { it.status == Status.BOUGHT }.sumOf { it.pricePaid ?: it.price },
                    planned = quarterWants.sumOf { it.price },
                    bought = quarterWants.count { it.status == Status.BOUGHT },
                    inProgress = quarterWants.count { it.status == Status.IN_PROGRESS },
                    failed = quarterWants.count { it.status == Status.FAILED }
                )
            }
        }
        item(key = "summary") {
            Text(
                "${filtered.size} want${if (filtered.size == 1) "" else "s"}" +
                    (if (pending > 0) " · ${formatInr(pending)} still to buy" else ""),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        fun cardFor(w: WantEntity, m: Modifier): @Composable () -> Unit = {
            WantCard(
                want = w,
                category = catById[w.categoryId],
                onClick = { nav.navigate(Routes.detail(w.id)) },
                onStatus = { requester.request(w, it) },
                onEdit = { nav.navigate(Routes.form(w.id)) },
                onMoveQuarter = { vm.moveToQuarter(w.id, thisQuarter) },
                modifier = m
            )
        }

        if (sort == SortMode.QUARTER) {
            val keys: List<QuarterKey?> = filtered.mapNotNull { it.plannedKey }.distinct().sorted() +
                (if (filtered.any { it.plannedKey == null }) listOf<QuarterKey?>(null) else emptyList())
            keys.forEach { key ->
                val group = filtered.filter { it.plannedKey == key }
                    .sortedWith(compareBy({ it.priority.ordinal }, { it.name.lowercase() }))
                item(key = "h_${key?.label ?: "none"}") {
                    SectionHeader(
                        key?.label ?: "Unplanned",
                        group.size,
                        highlight = key == thisQuarter,
                        total = group.sumOf { it.price }
                    )
                }
                items(group, key = { it.id }) { w -> cardFor(w, Modifier.animateItem())() }
            }
        } else {
            val sorted = when (sort) {
                SortMode.NEWEST -> filtered.sortedByDescending { it.addedAt }
                SortMode.PRICE_HIGH -> filtered.sortedByDescending { it.price }
                SortMode.PRICE_LOW -> filtered.sortedBy { it.price }
                else -> filtered.sortedWith(compareBy({ it.priority.ordinal }, { it.name.lowercase() }))
            }
            items(sorted, key = { it.id }) { w -> cardFor(w, Modifier.animateItem())() }
        }
    }
}

@Composable
private fun BoardView(
    wants: List<WantEntity>,
    catById: Map<Long, CategoryEntity>,
    nav: NavController,
    vm: MainViewModel,
    requester: StatusRequester
) {
    val thisQuarter = QuarterKey.now()
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(Status.entries.toList()) { st ->
            val column = wants.filter { it.status == st }
                .sortedWith(compareBy({ it.priority.ordinal }, { it.name.lowercase() }))
            Column(Modifier.width(290.dp).fillMaxHeight()) {
                Row(
                    Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${st.emoji} ${st.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor(st)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${column.size}", style = MaterialTheme.typography.labelLarge)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(column, key = { it.id }) { w ->
                        WantCard(
                            want = w,
                            category = catById[w.categoryId],
                            onClick = { nav.navigate(Routes.detail(w.id)) },
                            onStatus = { requester.request(w, it) },
                            onEdit = { nav.navigate(Routes.form(w.id)) },
                            onMoveQuarter = { vm.moveToQuarter(w.id, thisQuarter) },
                            compact = true
                        )
                    }
                }
            }
        }
    }
}
