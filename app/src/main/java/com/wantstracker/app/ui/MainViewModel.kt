package com.wantstracker.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wantstracker.app.WantsApp
import com.wantstracker.app.backup.Backup
import com.wantstracker.app.backup.ImportRow
import com.wantstracker.app.data.CategoryEntity
import com.wantstracker.app.data.Images
import com.wantstracker.app.data.StatusLogEntity
import com.wantstracker.app.data.WantEntity
import com.wantstracker.app.domain.DAY_MS
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WantsApp
    val repo = app.repository
    val prefs = app.prefs

    /** null while the first database read is in flight. */
    val wants: StateFlow<List<WantEntity>?> =
        repo.wants.stateIn<List<WantEntity>?>(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun want(id: Long): Flow<WantEntity?> = repo.observeWant(id)
    fun logs(id: Long): Flow<List<StatusLogEntity>> = repo.observeLogs(id)
    suspend fun getWant(id: Long): WantEntity? = repo.getWant(id)

    fun addWant(w: WantEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.addWant(w)
            onDone()
        }
    }

    fun updateWant(w: WantEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.updateWant(w)
            onDone()
        }
    }

    fun changeStatus(
        id: Long,
        status: Status,
        pricePaid: Double? = null,
        reason: String = "",
        at: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch { repo.changeStatus(id, status, pricePaid, reason, at) }
    }

    fun deleteWant(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.deleteWant(id)
            onDone()
        }
    }

    fun moveToQuarter(id: Long, key: QuarterKey) {
        viewModelScope.launch { repo.moveToQuarter(id, key) }
    }

    fun setCooling(id: Long, days: Int?) {
        viewModelScope.launch {
            repo.setCooling(id, days?.let { System.currentTimeMillis() + it * DAY_MS })
        }
    }

    fun addCategory(name: String, emoji: String, color: Long, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.addCategory(name, emoji, color)) }
    }

    fun updateCategory(c: CategoryEntity) {
        viewModelScope.launch { repo.updateCategory(c) }
    }

    fun deleteCategory(c: CategoryEntity) {
        viewModelScope.launch { repo.deleteCategory(c) }
    }

    suspend fun saveImage(uri: Uri): String? = Images.importFromUri(app, uri)

    fun importRows(rows: List<ImportRow>, onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(repo.importRows(rows)) }
    }

    fun exportTo(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val msg = try {
                val json = Backup.export(app)
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                }
                "Backup saved"
            } catch (e: Exception) {
                "Backup failed: ${e.message}"
            }
            onDone(msg)
        }
    }

    fun restoreFrom(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val msg = try {
                val json = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                }
                "Restored ${Backup.restore(app, json)} wants"
            } catch (e: Exception) {
                "Restore failed: ${e.message}"
            }
            onDone(msg)
        }
    }

    suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
    }
}
