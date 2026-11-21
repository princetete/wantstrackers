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
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
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
import com.wantstracker.app.domain.plannedKey
import com.wantstracker.app.ui.theme.statusColor

@Composable
fun MainBottomBar(nav: NavController, selected: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Triple(Routes.HOME, "Wants", Icons.Filled.Favorite),
                Triple(Routes.INSIGHTS, "Insights", Icons.Filled.BarChart),
                Triple(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
            ).forEach { (route, label, icon) ->
                val active = selected == route
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable {
                        if (!active) nav.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier.size(width = 64.dp, height = 32.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = label,
                            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(label, style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
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
        Text(want.emoji, fontSize = (size / 2).sp)
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
    val statusTint = statusColor(want.status)
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(5.dp).height(if (compact) 132.dp else 156.dp).background(statusTint))
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    WantThumb(want, category, size = 56)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(want.name, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val priceText = when {
                            want.status == Status.BOUGHT && want.pricePaid != null -> formatInr(want.pricePaid)
                            want.price > 0 -> formatInr(want.price)
                            else -> "No price"
                        }
                        Text(priceText, style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(buildString {
                            append(if (want.priority == com.wantstracker.app.domain.Priority.NEED) "Want" else want.priority.label)
                            if (category != null) append("  ·  ${category.name}")
                            want.plannedKey?.let { append("  ·  ${it.label}") }
                        }, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            Status.entries.filter { it != want.status }.forEach { st ->
                                DropdownMenuItem(text = { Text(st.label) }, onClick = {
                                    menu = false
                                    onStatus(st)
                                })
                            }
                            DropdownMenuItem(text = { Text("Edit") }, onClick = {
                                menu = false
                                onEdit()
                            })
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(want.status)
                    if (overdue) Pill("Overdue", statusColor(Status.FAILED))
                    if (cooling != null && want.status.isOpen) {
                        Pill(if (cooling > 0) "${cooling} days left" else "Ready to decide", MaterialTheme.colorScheme.tertiary)
                    }
                }
                if (!compact) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        when (want.status) {
                            Status.NOT_STARTED -> TextButton(onClick = { onStatus(Status.IN_PROGRESS) }) { Text("Start") }
                            Status.IN_PROGRESS -> {
                                TextButton(onClick = { onStatus(Status.BOUGHT) }) { Text("Bought") }
                                TextButton(onClick = { onStatus(Status.FAILED) }) { Text("Failed") }
                            }
                            else -> {}
                        }
                        if (overdue) TextButton(onClick = onMoveQuarter) { Text("Move to current quarter") }
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
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(104.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 46.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(16.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun QuarterPicker(year: Int?, quarter: Int?, onChange: (Int?, Int?) -> Unit) {
    val now = QuarterKey.now()
    val years = 2025..(now.year + 3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
