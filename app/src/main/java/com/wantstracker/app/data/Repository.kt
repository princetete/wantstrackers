package com.wantstracker.app.data

import android.content.Context
import androidx.room.withTransaction
import com.wantstracker.app.backup.ImportRow
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.formatInr
import com.wantstracker.app.notify.Reminders
import com.wantstracker.app.widget.WantsWidgetProvider
import kotlinx.coroutines.flow.Flow
import java.io.File

class Repository(private val context: Context, val db: AppDatabase) {
    private val wantDao = db.wantDao()
    private val catDao = db.categoryDao()
    private val logDao = db.logDao()

    val wants: Flow<List<WantEntity>> = wantDao.observeAll()
    val categories: Flow<List<CategoryEntity>> = catDao.observeAll()

    fun observeWant(id: Long): Flow<WantEntity?> = wantDao.observe(id)
    fun observeLogs(id: Long): Flow<List<StatusLogEntity>> = logDao.observeFor(id)
    suspend fun getWant(id: Long): WantEntity? = wantDao.get(id)

    suspend fun seedDefaults() {
        if (catDao.count() == 0) catDao.insertAll(Defaults.categories)
    }

    // ---------- wants ----------

    private suspend fun insertWant(w: WantEntity): Long {
        val id = wantDao.insert(w.copy(id = 0))
        logDao.insert(StatusLogEntity(wantId = id, status = Status.NOT_STARTED, at = w.addedAt, note = "Added"))
        if (w.status != Status.NOT_STARTED) {
            val at = when (w.status) {
                Status.BOUGHT -> w.boughtAt
                Status.FAILED -> w.failedAt
                else -> w.lastStatusChangeAt
            } ?: w.lastStatusChangeAt
            val note = when (w.status) {
                Status.BOUGHT -> w.pricePaid?.let { "Paid ${formatInr(it)}" } ?: ""
                Status.FAILED -> w.failReason
                else -> ""
            }
            logDao.insert(StatusLogEntity(wantId = id, status = w.status, at = at, note = note))
        }
        return id
    }

    suspend fun addWant(w: WantEntity): Long {
        val id = insertWant(w)
        WantsWidgetProvider.refreshAll(context)
        return id
    }

    suspend fun updateWant(w: WantEntity) {
        wantDao.update(w)
        WantsWidgetProvider.refreshAll(context)
    }

    suspend fun changeStatus(id: Long, status: Status, pricePaid: Double?, reason: String, at: Long) {
        val w = wantDao.get(id) ?: return
        if (w.status == status) return
        val closed = status == Status.BOUGHT || status == Status.FAILED
        val paid = if (status == Status.BOUGHT) (pricePaid ?: w.price) else null
        wantDao.update(
            w.copy(
                status = status,
                lastStatusChangeAt = at,
                pricePaid = paid,
                boughtAt = if (status == Status.BOUGHT) at else null,
                failedAt = if (status == Status.FAILED) at else null,
                failReason = if (status == Status.FAILED) reason else "",
                coolingUntil = if (closed) null else w.coolingUntil
            )
        )
        val note = when (status) {
            Status.BOUGHT -> paid?.let { "Paid ${formatInr(it)}" } ?: ""
            Status.FAILED -> reason
            else -> ""
        }
        logDao.insert(StatusLogEntity(wantId = id, status = status, at = at, note = note))
        if (closed) Reminders.cancelCooling(context, id)
        WantsWidgetProvider.refreshAll(context)
    }

    suspend fun deleteWant(id: Long) {
        val w = wantDao.get(id)
        wantDao.deleteById(id)
        Reminders.cancelCooling(context, id)
        val path = w?.imagePath.orEmpty()
        if (path.isNotBlank()) runCatching { File(path).delete() }
        WantsWidgetProvider.refreshAll(context)
    }

    suspend fun moveToQuarter(id: Long, key: QuarterKey) {
        val w = wantDao.get(id) ?: return
        wantDao.update(w.copy(plannedYear = key.year, plannedQuarter = key.quarter))
        WantsWidgetProvider.refreshAll(context)
    }

    suspend fun setCooling(id: Long, until: Long?) {
        val w = wantDao.get(id) ?: return
        wantDao.update(w.copy(coolingUntil = until))
        if (until != null) Reminders.scheduleCooling(context, id, until) else Reminders.cancelCooling(context, id)
    }

    // ---------- categories ----------

    suspend fun addCategory(name: String, emoji: String, color: Long): Long =
        catDao.insert(CategoryEntity(name = name, emoji = emoji, color = color))

    suspend fun updateCategory(c: CategoryEntity) = catDao.update(c)
    suspend fun deleteCategory(c: CategoryEntity) = catDao.delete(c)

    // ---------- bulk import ----------

    suspend fun importRows(rows: List<ImportRow>): Int {
        val count = db.withTransaction {
            val existing = catDao.getAll().toMutableList()
            var colorIdx = existing.size
            var n = 0
            val now = System.currentTimeMillis()
            for (r in rows) {
                var catId: Long? = null
                if (r.category.isNotBlank()) {
                    val found = existing.firstOrNull { it.name.equals(r.category, ignoreCase = true) }
                    catId = if (found != null) {
                        found.id
                    } else {
                        val c = CategoryEntity(
                            name = r.category,
                            emoji = "🎁",
                            color = Defaults.colors[colorIdx++ % Defaults.colors.size]
                        )
                        val newId = catDao.insert(c)
                        existing.add(c.copy(id = newId))
                        newId
                    }
                }
                val q = r.quarter
                val closed = r.status == Status.BOUGHT || r.status == Status.FAILED
                val doneAt = if (q == null) now else q.midMillis().coerceAtMost(now)
                val start = q?.startMillis()?.takeIf { it < now } ?: now
                val addedAt = if (closed) minOf(start, doneAt) else start
                val w = WantEntity(
                    name = r.name,
                    price = r.price,
                    notes = if (r.status == Status.FAILED) "" else r.note,
                    categoryId = catId,
                    plannedYear = q?.year,
                    plannedQuarter = q?.quarter,
                    status = r.status,
                    failReason = if (r.status == Status.FAILED) r.note else "",
                    pricePaid = if (r.status == Status.BOUGHT) r.price else null,
                    addedAt = addedAt,
                    boughtAt = if (r.status == Status.BOUGHT) doneAt else null,
                    failedAt = if (r.status == Status.FAILED) doneAt else null,
                    lastStatusChangeAt = if (closed) doneAt else addedAt
                )
                insertWant(w)
                n++
            }
            n
        }
        WantsWidgetProvider.refreshAll(context)
        return count
    }
}
