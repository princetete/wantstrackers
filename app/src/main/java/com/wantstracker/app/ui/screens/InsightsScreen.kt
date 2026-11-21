@file:OptIn(ExperimentalMaterial3Api::class)

package com.wantstracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wantstracker.app.data.WantEntity
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.effectiveKey
import com.wantstracker.app.domain.formatInr
import com.wantstracker.app.ui.EmptyState
import com.wantstracker.app.ui.MainBottomBar
import com.wantstracker.app.ui.MainViewModel
import com.wantstracker.app.ui.Routes
import com.wantstracker.app.ui.theme.statusColor

@Composable
fun InsightsScreen(vm: MainViewModel, nav: NavController) {
    val wantsOrNull by vm.wants.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    var year by rememberSaveable { mutableStateOf<Int?>(null) }

    val all = wantsOrNull ?: emptyList()
    val thisYear = QuarterKey.now().year
    val years = (all.map { it.effectiveKey.year } + thisYear).distinct().sortedDescending()
    val scoped = all.filter { year == null || it.effectiveKey.year == year }
    val bought = scoped.filter { it.status == Status.BOUGHT }
    val failed = scoped.filter { it.status == Status.FAILED }
    val open = scoped.filter { it.status.isOpen }
    val decided = bought.size + failed.size
    val rate = if (decided > 0) bought.size * 100 / decided else null
    val spent = bought.sumOf { it.pricePaid ?: it.price }
    val openValue = open.sumOf { it.price }

    val perQuarter: List<Pair<QuarterKey, List<WantEntity>>> =
        if (year != null) {
            (1..4).map { q -> QuarterKey(year!!, q) to scoped.filter { it.effectiveKey.quarter == q } }
        } else {
            scoped.groupBy { it.effectiveKey }.toList().sortedBy { it.first }.takeLast(8)
        }

    Scaffold(
        topBar = { TopAppBar(title = { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("Your insights", fontWeight = FontWeight.Bold); Text("Understand your wishlist progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) } }) },
        bottomBar = { MainBottomBar(nav, Routes.INSIGHTS) }
    ) { padding ->
        if (all.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                EmptyState("📊", "No data yet", "Add a few wants and your stats will show up here.")
            }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 12.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = year == null, onClick = { year = null }, label = { Text("All time") })
                years.forEach { y ->
                    FilterChip(selected = year == y, onClick = { year = y }, label = { Text("$y") })
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (year == null) "YOUR WISHLIST AT A GLANCE" else "$year AT A GLANCE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
                            Text(formatInr(spent), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Still to buy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                            Text(formatInr(openValue), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Total wants", "${scoped.size}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatTile("Bought", "${bought.size}", statusColor(Status.BOUGHT), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Failed", "${failed.size}", statusColor(Status.FAILED), Modifier.weight(1f))
                StatTile("Still open", "${open.size}", statusColor(Status.IN_PROGRESS), Modifier.weight(1f))
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Success rate", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (rate == null) {
                        Text("Nothing bought or failed yet.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("$rate%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { rate / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = statusColor(Status.BOUGHT),
                            trackColor = statusColor(Status.FAILED).copy(alpha = 0.35f)
                        )
                        Text(
                            "${bought.size} bought · ${failed.size} let go",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Spent on bought wants: ${formatInr(spent)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Still to buy: ${formatInr(openValue)}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("By quarter", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (perQuarter.isEmpty()) Text("No data.", style = MaterialTheme.typography.bodyMedium)
                    perQuarter.forEach { (key, list) ->
                        val b = list.count { it.status == Status.BOUGHT }
                        val f = list.count { it.status == Status.FAILED }
                        val o = list.size - b - f
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (year != null) "Q${key.quarter}" else key.label,
                                Modifier.width(76.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            StackedBar(b, f, o, Modifier.weight(1f))
                            Text(
                                "  $b/${list.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "🟢 bought  🔴 failed  ⚪ open  ·  x/y = bought out of planned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val byCat = bought.groupBy { it.categoryId }
                .map { (id, list) -> id to list.sumOf { it.pricePaid ?: it.price } }
                .sortedByDescending { it.second }
            if (byCat.isNotEmpty()) {
                val max = byCat.maxOf { it.second }.coerceAtLeast(1.0)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Spent by category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        byCat.forEach { (id, value) ->
                            val c = categories.firstOrNull { it.id == id }
                            Column {
                                Row {
                                    Text(
                                        c?.let { "${it.emoji} ${it.name}" } ?: "Uncategorized",
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(formatInr(value), style = MaterialTheme.typography.bodyMedium)
                                }
                                LinearProgressIndicator(
                                    progress = { (value / max).toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }

            if (failed.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🕊️ Let go", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        val reasons = failed.groupBy { it.failReason.trim().ifBlank { "No reason" } }
                            .map { it.key to it.value.size }.sortedByDescending { it.second }
                        Text(
                            reasons.joinToString("  ·  ") { "${it.first} ×${it.second}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        failed.forEach { w ->
                            Text("• ${w.name} — ${w.failReason.ifBlank { "no reason" }}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            val y = year
            if (y != null) {
                val bestQuarter = perQuarter.maxByOrNull { it.second.count { w -> w.status == Status.BOUGHT } }
                val bestCount = bestQuarter?.second?.count { it.status == Status.BOUGHT } ?: 0
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🎉 $y in review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${scoped.size} wants: ${bought.size} bought, ${failed.size} let go, ${open.size} still open.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (bestCount > 0 && bestQuarter != null) {
                            Text(
                                "Best quarter: Q${bestQuarter.first.quarter} with $bestCount bought.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (spent > 0) Text("You spent ${formatInr(spent)}.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StackedBar(bought: Int, failed: Int, open: Int, modifier: Modifier) {
    val total = bought + failed + open
    Row(modifier.height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (total > 0) {
            if (bought > 0) Box(Modifier.weight(bought.toFloat()).fillMaxSize().background(statusColor(Status.BOUGHT)))
            if (failed > 0) Box(Modifier.weight(failed.toFloat()).fillMaxSize().background(statusColor(Status.FAILED)))
            if (open > 0) Box(Modifier.weight(open.toFloat()).fillMaxSize().background(statusColor(Status.NOT_STARTED).copy(alpha = 0.5f)))
        }
    }
}
