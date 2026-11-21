@file:OptIn(ExperimentalMaterial3Api::class)

package com.wantstracker.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
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
import com.wantstracker.app.domain.isOverdue
import com.wantstracker.app.ui.EmptyState
import com.wantstracker.app.ui.MainBottomBar
import com.wantstracker.app.ui.MainViewModel
import com.wantstracker.app.ui.Routes
import com.wantstracker.app.ui.SectionHeader
import com.wantstracker.app.ui.StatusRequester
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

    var viewMode by rememberSaveable { mutableStateOf("cards") }
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
    val years = (wants.map { it.effectiveKey.year } + thisQuarter.year).distinct().filter { it > 2024 }.sortedDescending()

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
                        Column {
                            Text("Wants", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                        }
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
                    IconButton(onClick = { viewMode = when (viewMode) { "cards" -> "table"; "table" -> "board"; else -> "cards" } }) {
                        Icon(
                            if (viewMode == "board") Icons.Filled.ViewColumn else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = when (viewMode) { "cards" -> "Switch to table view"; "table" -> "Switch to board view"; else -> "Switch to card view" }
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
                            Priority.entries.filter { it != Priority.NEED }.forEach { p ->
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = { MainBottomBar(nav, Routes.HOME) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.form(0)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add want", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(22.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            DashboardSummary(wants)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(selected = statusFilter == null, onClick = { statusFilter = null },
                    label = { Text("All · ${wants.size}") })
                Status.entries.forEach { st ->
                    val count = wants.count { it.status == st }
                    FilterChip(selected = statusFilter == st, onClick = { statusFilter = if (statusFilter == st) null else st },
                        label = { Text("${st.label} · $count") })
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterDropdown("Year", yearFilter?.toString() ?: "All years",
                    listOf("All years") + years.map { it.toString() },
                    { i -> yearFilter = if (i == 0) null else years[i - 1] })
                FilterDropdown("Category",
                    categories.firstOrNull { it.id == categoryFilter }?.let { "${it.emoji} ${it.name}" } ?: "All categories",
                    listOf("All categories") + categories.map { "${it.emoji} ${it.name}" },
                    { i -> categoryFilter = if (i == 0) null else categories[i - 1].id })
                if (statusFilter != null || yearFilter != null || categoryFilter != null || priorityFilter != null) {
                    FilterChip(selected = false, onClick = {
                        statusFilter = null; yearFilter = null; categoryFilter = null; priorityFilter = null
                    }, label = { Text("Clear filters") })
                }
            }

            when {
                wantsOrNull == null -> Spacer(Modifier.fillMaxSize())
                wants.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState("🛍️", "Your wishlist starts here", "Add the things you want to buy and organise them by year, quarter and priority.", "Add your first want") { nav.navigate(Routes.form(0)) }
                }
                filtered.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState("🔍", "No matches found", "Try changing your search or filters.", "Clear filters") { statusFilter = null; yearFilter = null; categoryFilter = null; priorityFilter = null; query = "" }
                }
                viewMode == "board" -> BoardView(filtered, catById, nav, vm, requester)
                viewMode == "table" -> TableView(filtered, nav)
                else -> ListView(filtered, catById, nav, vm, requester, sort, thisQuarter)
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
private fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != options.firstOrNull(),
            onClick = { expanded = true },
            label = { Text("$label: $selected", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ListView(
    filtered: List<WantEntity>,
    catById: Map<Long, CategoryEntity>,
    nav: NavController,
    vm: MainViewModel,
    requester: StatusRequester,
    sort: SortMode,
    thisQuarter: QuarterKey
) {
    val pending = filtered.filter { it.status.isOpen }.sumOf { it.price }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "summary") {
            Text(
                "${filtered.size} want${if (filtered.size == 1) "" else "s"}" +
                    (if (pending > 0) " · ${formatInr(pending)} still to buy" else ""),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        fun cardFor(w: WantEntity): @Composable () -> Unit = {
            WantCard(
                want = w,
                category = catById[w.categoryId],
                onClick = { nav.navigate(Routes.detail(w.id)) },
                onStatus = { requester.request(w, it) },
                onEdit = { nav.navigate(Routes.form(w.id)) },
                onMoveQuarter = { vm.moveToQuarter(w.id, thisQuarter) }
            )
        }

        if (sort == SortMode.QUARTER) {
            val planned = filtered.filter { it.plannedKey != null }
            val years = planned.mapNotNull { it.plannedKey?.year }.distinct().sorted()
            years.forEach { year ->
                val yearItems = planned.filter { it.plannedKey?.year == year }
                item(key = "year_$year") {
                    SectionHeader("Year $year", yearItems.size, highlight = year == thisQuarter.year)
                }
                val quarters = yearItems.mapNotNull { it.plannedKey }.distinct().sorted()
                quarters.forEach { key ->
                    val group = yearItems.filter { it.plannedKey == key }
                        .sortedWith(compareBy({ it.priority.ordinal }, { it.name.lowercase() }))
                    item(key = "quarter_${key.year}_${key.quarter}") {
                        SectionHeader("  Q${key.quarter}", group.size, highlight = key == thisQuarter)
                    }
                    items(group, key = { it.id }) { w -> cardFor(w)() }
                }
            }
            val unplanned = filtered.filter { it.plannedKey == null }
                .sortedWith(compareBy({ it.priority.ordinal }, { it.name.lowercase() }))
            if (unplanned.isNotEmpty()) {
                item(key = "year_unplanned") {
                    SectionHeader("Unplanned", unplanned.size)
                }
                items(unplanned, key = { it.id }) { w -> cardFor(w)() }
            }
        } else {
            val sorted = when (sort) {
                SortMode.NEWEST -> filtered.sortedByDescending { it.addedAt }
                SortMode.PRICE_HIGH -> filtered.sortedByDescending { it.price }
                SortMode.PRICE_LOW -> filtered.sortedBy { it.price }
                else -> filtered.sortedWith(compareBy({ it.priority.ordinal }, { it.name.lowercase() }))
            }
            items(sorted, key = { it.id }) { w -> cardFor(w)() }
        }
    }
}

@Composable
private fun TableView(wants: List<WantEntity>, nav: NavController) {
    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text("Want", Modifier.weight(2.2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Status", Modifier.weight(1.25f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Priority", Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Price", Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            HorizontalDivider()
        }
        items(wants.sortedBy { it.name.lowercase() }, key = { it.id }) { w ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 11.dp)
                    .then(Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(w.name, Modifier.weight(2.2f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                Text(w.status.label, Modifier.weight(1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = statusColor(w.status))
                Text(if (w.priority == Priority.NEED) "Want" else w.priority.label, Modifier.weight(1.1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                Text(if (w.status == Status.BOUGHT && w.pricePaid != null) formatInr(w.pricePaid) else formatInr(w.price), Modifier.weight(1.1f), maxLines = 1, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.End, color = MaterialTheme.colorScheme.primary)
            }.clickable { nav.navigate(Routes.detail(w.id)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
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

 
@Composable
private fun DashboardSummary(wants: List<WantEntity>) {
    val bought = wants.count { it.status == Status.BOUGHT }
    val open = wants.filter { it.status.isOpen }
    val totalValue = open.sumOf { it.price }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("STILL TO BUY", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
            Text(formatInr(totalValue), style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                SummaryCount(open.size, "open")
                SummaryCount(bought, "bought")
            }
        }
    }
}
@Composable
private fun SummaryCount(value: Int, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
    }
}

@Composable
private fun DashboardCount(label: String, count: Int, tint: Color, modifier: Modifier) {
    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
