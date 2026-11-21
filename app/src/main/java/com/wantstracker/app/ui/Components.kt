package com.wantstracker.app.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.wantstracker.app.data.CategoryEntity
import com.wantstracker.app.data.WantEntity
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.coolingDaysLeft
import com.wantstracker.app.domain.formatInr
import com.wantstracker.app.domain.isOverdue
import com.wantstracker.app.ui.theme.statusColor
import java.io.File

private data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainBottomBar(nav: NavController, selected: String) {
    val items = listOf(
        NavItem(Routes.HOME, "Wants", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
        NavItem(Routes.INSIGHTS, "Insights", Icons.Filled.BarChart, Icons.Outlined.BarChart),
        NavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        items.forEach { item ->
            val isSelected = selected == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        nav.navigate(item.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
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
    val shape = RoundedCornerShape(16.dp)
    val file = if (want.imagePath.isNotBlank()) File(want.imagePath) else null
    if (file != null && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(shape)
        )
    } else {
        val tint = category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
        Box(
            Modifier.size(size.dp).clip(shape).background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(want.emoji, fontSize = (size / 2).sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WantCard(
    want: WantEntity,
    category: CategoryEntity?,
    onClick: () -> Unit,
    onStatus: (Status) -> Unit,
    onEdit: () -> Unit,
    onMoveQuarter: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (compact || !want.status.isOpen) {
        WantCardBody(want, category, onClick, onStatus, onEdit, onMoveQuarter, compact, modifier)
        return
    }

    val latestOnStatus by rememberUpdatedState(onStatus)
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> latestOnStatus(Status.BOUGHT)
                SwipeToDismissBoxValue.EndToStart -> latestOnStatus(Status.FAILED)
                SwipeToDismissBoxValue.Settled -> {}
            }
            false
        }
    )
    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier,
        backgroundContent = { SwipeBackground(swipeState.dismissDirection, swipeState.targetValue) }
    ) {
        WantCardBody(want, category, onClick, onStatus, onEdit, onMoveQuarter, compact, Modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, target: SwipeToDismissBoxValue) {
    val bought = direction == SwipeToDismissBoxValue.StartToEnd
    val failed = direction == SwipeToDismissBoxValue.EndToStart
    val base = when {
        bought -> statusColor(Status.BOUGHT)
        failed -> statusColor(Status.FAILED)
        else -> Color.Transparent
    }
    val armed = target != SwipeToDismissBoxValue.Settled
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(base.copy(alpha = if (armed) 1f else 0.6f))
            .padding(horizontal = 20.dp),
        contentAlignment = if (failed) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (bought || failed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (bought) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (bought) "Bought" else "Failed",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WantCardBody(
    want: WantEntity,
    category: CategoryEntity?,
    onClick: () -> Unit,
    onStatus: (Status) -> Unit,
    onEdit: () -> Unit,
    onMoveQuarter: () -> Unit,
    compact: Boolean,
    modifier: Modifier
) {
    var menu by remember { mutableStateOf(false) }
    val overdue = want.isOverdue()
    val cooling = want.coolingDaysLeft()

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            priceText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(want.priority.emoji, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    Pill(
                        if (cooling > 0) "Cooling off · ${cooling}d" else "Ready to decide",
                        MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            if (!compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (want.status) {
                        Status.NOT_STARTED -> QuickAction(
                            Icons.Filled.PlayArrow,
                            "Start",
                            MaterialTheme.colorScheme.primary
                        ) { onStatus(Status.IN_PROGRESS) }
                        Status.IN_PROGRESS -> {
                            QuickAction(Icons.Filled.Check, "Bought", statusColor(Status.BOUGHT)) {
                                onStatus(Status.BOUGHT)
                            }
                            QuickAction(Icons.Filled.Close, "Failed", statusColor(Status.FAILED)) {
                                onStatus(Status.FAILED)
                            }
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
fun SummaryCard(
    quarterLabel: String,
    spent: Double,
    planned: Double,
    bought: Int,
    inProgress: Int,
    failed: Int
) {
    val fraction = if (planned > 0) (spent / planned).toFloat().coerceIn(0f, 1f) else 0f
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = onPrimary
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Bought in $quarterLabel",
                style = MaterialTheme.typography.labelLarge,
                color = onPrimary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatInr(spent),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "of ${formatInr(planned)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = onPrimary,
                trackColor = onPrimary.copy(alpha = 0.25f)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                SummaryStat(Icons.Filled.Check, "$bought bought", onPrimary)
                SummaryStat(Icons.Filled.PlayArrow, "$inProgress in progress", onPrimary)
                SummaryStat(Icons.Filled.Close, "$failed failed", onPrimary)
            }
        }
    }
}

@Composable
private fun SummaryStat(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.8f))
    }
}

@Composable
fun SectionHeader(text: String, count: Int, highlight: Boolean = false, total: Double? = null) {
    val sub = buildString {
        append(count)
        append(if (count == 1) " want" else " wants")
        if (total != null && total > 0) append(" · ${formatInr(total)}")
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp, start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
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
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
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
                    label = { Text("Q$q", maxLines = 1, softWrap = false) }
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
