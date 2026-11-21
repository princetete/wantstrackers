package com.wantstracker.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wantstracker.app.domain.Priority
import com.wantstracker.app.domain.Status

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "🎁",
    val color: Long = 0xFF5648D8
)

@Entity(
    tableName = "wants",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class WantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double = 0.0,
    val link: String = "",
    val imagePath: String = "",
    val notes: String = "",
    val priority: Priority = Priority.WANT,
    val categoryId: Long? = null,
    val plannedYear: Int? = null,
    val plannedQuarter: Int? = null,
    val status: Status = Status.NOT_STARTED,
    val failReason: String = "",
    val pricePaid: Double? = null,
    val addedAt: Long,
    val boughtAt: Long? = null,
    val failedAt: Long? = null,
    val coolingUntil: Long? = null,
    val lastStatusChangeAt: Long
)

@Entity(
    tableName = "status_logs",
    foreignKeys = [
        ForeignKey(
            entity = WantEntity::class,
            parentColumns = ["id"],
            childColumns = ["wantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("wantId")]
)
data class StatusLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wantId: Long,
    val status: Status,
    val at: Long,
    val note: String = ""
)

object Defaults {
    val colors = listOf(
        0xFF5648D8, 0xFF0E8F8C, 0xFFE5484D, 0xFFF59E0B,
        0xFF22A06B, 0xFF3B82F6, 0xFFD946EF, 0xFF64748B
    )

    val categories = listOf(
        CategoryEntity(name = "Tech", emoji = "💻", color = colors[0]),
        CategoryEntity(name = "Gaming", emoji = "🎮", color = colors[6]),
        CategoryEntity(name = "Travel", emoji = "✈️", color = colors[5]),
        CategoryEntity(name = "Fashion", emoji = "👕", color = colors[2]),
        CategoryEntity(name = "Home", emoji = "🏠", color = colors[3]),
        CategoryEntity(name = "Fitness", emoji = "💪", color = colors[4]),
        CategoryEntity(name = "Gifts", emoji = "🎁", color = colors[1]),
        CategoryEntity(name = "Other", emoji = "✨", color = colors[7])
    )

    val emojiChoices = listOf(
        "💻", "📱", "🎮", "✈️", "👕", "👟", "🏠", "🛋️", "💪", "🎁", "📚", "🎧",
        "📷", "🚗", "🍔", "💄", "⌚", "🎸", "🧳", "✨"
    )
}
