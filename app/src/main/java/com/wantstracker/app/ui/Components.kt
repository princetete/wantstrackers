package com.wantstracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wantstracker.app.data.CategoryEntity
import com.wantstracker.app.data.WantEntity
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.coolingDaysLeft
import com.wantstracker.app.domain.formatInr
import com.wantstracker.app.domain.isOverdue
import com.wantstracker.app.ui.theme.statusColor

@Composable
fun MainBottomBar(nav: NavController, selected: String) {
    NavigationBar {
        listOf(
            Triple(Routes.HOME, "Wants", Icons.Filled.Favorite),
            Triple(Routes.INSIGHTS, "Insights", Icons.Filled.BarChart),
            Triple(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
        ).forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = selected == route,
                onClick = {
                    if (selected != route) {
                        nav.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
fun StatusPill(status: Status, modifier: Modifier = Modifier) {
    val c = statusColor(status)
    Surface(color = c.copy(alpha = 0.14f), shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(c, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(status.label, style = MaterialTheme.typography.labelMedium, color = c)
        }
    }
}

@Composable
fun WantThumb(want: WantEntity, category: CategoryEntity?, size: Int = 56) {
    val tint = category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(category?.emoji ?: "🛍️", fontSize = (size / 2).sp)
    }
}

@Composable
fun WantCard(
    want: WantEntity,
    category: CategoryEntity?,
    onClick: () -> Unit,
    onStatus: (Status) -> Unit,
    onEdit: () -> Unit,
    onMoveQuarter: () -> Unit,
    compact: Boolean = false
) {
    var menu by remember { mutableStateOf(false) }
    val overdue = want.isOverdue()
    val cooling = want.coolingDaysLeft()

    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                WantThumb(want, category, size = if (compact) 44 else 56)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        want.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val priceText = when {
                        want.status == Status.BOUGHT && want.pricePaid != null -> "Paid ${formatInr(want.pricePaid)}"
                        want.price > 0 -> formatInr(want.price)
                        else -> "No price"
                    }
                    Text(
                        "$priceText  ${want.priority.emoji}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        Status.entries.filter { it != want.status }.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("Move to ${s.emoji} ${s.label}") },
                                onClick = {
                                    menu = false
                                    onStatus(s)
                                }
                            )
                        }
                        DropdownMenuItem(text = { Text("Edit") }, onClick = {
                            menu = false
                            onEdit()
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(want.status)
                if (category != null) Pill("${category.emoji} ${category.name}", Color(category.color))
                if (overdue) Pill("Overdue", statusColor(Status.FAILED))
                if (cooling != null && want.status.isOpen) {
                    Pill(if (cooling > 0) "⏳ ${cooling}d" else "⏳ ready to decide", MaterialTheme.colorScheme.tertiary)
                }
            }
            if (!compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (want.status) {
                        Status.NOT_STARTED -> TextButton(onClick = { onStatus(Status.IN_PROGRESS) }) { Text("▶ Start") }
                        Status.IN_PROGRESS -> {
                            TextButton(onClick = { onStatus(Status.BOUGHT) }) { Text("🟢 Bought") }
                            TextButton(onClick = { onStatus(Status.FAILED) }) { Text("🔴 Failed") }
                        }
                        else -> {}
                    }
                    if (overdue) {
                        val next = QuarterKey.now()
                        TextButton(onClick = onMoveQuarter) { Text("Move to ${next.label}") }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String, count: Int, highlight: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuarterPicker(year: Int?, quarter: Int?, onChange: (Int?, Int?) -> Unit) {
    val now = QuarterKey.now()
    val years = (now.year - 6)..(now.year + 3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = year == null,
                onClick = { onChange(null, null) },
                label = { Text("Unplanned") }
            )
            (1..4).forEach { q ->
                FilterChip(
                    selected = quarter == q && year != null,
                    onClick = { onChange(year ?: now.year, q) },
                    label = { Text("Q$q") }
                )
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            years.forEach { y ->
                FilterChip(
                    selected = year == y,
                    onClick = { onChange(y, quarter ?: if (y == now.year) now.quarter else 1) },
                    label = { Text("$y") }
                )
            }
        }
    }
}
