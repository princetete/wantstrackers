package com.wantstracker.app.backup

import android.util.Base64
import androidx.room.withTransaction
import com.wantstracker.app.WantsApp
import com.wantstracker.app.data.CategoryEntity
import com.wantstracker.app.data.Images
import com.wantstracker.app.data.StatusLogEntity
import com.wantstracker.app.data.WantEntity
import com.wantstracker.app.domain.Priority
import com.wantstracker.app.domain.Status
import com.wantstracker.app.notify.Reminders
import com.wantstracker.app.widget.WantsWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private fun JSONObject.intOrNull(k: String): Int? = if (has(k) && !isNull(k)) getInt(k) else null
private fun JSONObject.longOrNull(k: String): Long? = if (has(k) && !isNull(k)) getLong(k) else null
private fun JSONObject.doubleOrNull(k: String): Double? = if (has(k) && !isNull(k)) getDouble(k) else null

object Backup {
    private const val APP_TAG = "WantsTracker"

    suspend fun export(app: WantsApp): String = withContext(Dispatchers.IO) {
        val db = app.db
        val root = JSONObject()
        root.put("app", APP_TAG)
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val cats = JSONArray()
        db.categoryDao().getAll().forEach {
            cats.put(JSONObject().put("id", it.id).put("name", it.name).put("emoji", it.emoji).put("color", it.color))
        }
        root.put("categories", cats)

        val wants = JSONArray()
        db.wantDao().getAll().forEach { w ->
            val o = JSONObject()
            o.put("id", w.id)
            o.put("name", w.name)
            o.put("price", w.price)
            o.put("link", w.link)
            o.put("notes", w.notes)
            o.put("priority", w.priority.name)
            o.put("categoryId", w.categoryId)
            o.put("plannedYear", w.plannedYear)
            o.put("plannedQuarter", w.plannedQuarter)
            o.put("status", w.status.name)
            o.put("failReason", w.failReason)
            o.put("pricePaid", w.pricePaid)
            o.put("addedAt", w.addedAt)
            o.put("boughtAt", w.boughtAt)
            o.put("failedAt", w.failedAt)
            o.put("coolingUntil", w.coolingUntil)
            o.put("lastStatusChangeAt", w.lastStatusChangeAt)
            if (w.imagePath.isNotBlank()) {
                val f = File(w.imagePath)
                if (f.exists() && f.length() < 3_000_000L) {
                    o.put("imageB64", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                }
            }
            wants.put(o)
        }
        root.put("wants", wants)

        val logs = JSONArray()
        db.logDao().getAll().forEach {
            logs.put(
                JSONObject().put("id", it.id).put("wantId", it.wantId)
                    .put("status", it.status.name).put("at", it.at).put("note", it.note)
            )
        }
        root.put("logs", logs)
        root.toString()
    }

    /** Replaces ALL current data with the backup. Returns the number of wants restored. */
    suspend fun restore(app: WantsApp, json: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        require(root.optString("app") == APP_TAG) { "This is not a Wants Tracker backup" }

        val cats = root.getJSONArray("categories").let { arr ->
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                CategoryEntity(o.getLong("id"), o.getString("name"), o.optString("emoji", "🎁"), o.getLong("color"))
            }
        }

        val imgDir = Images.dir(app)
        val keep = mutableSetOf<String>()
        val wants = root.getJSONArray("wants").let { arr ->
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                var path = ""
                if (o.has("imageB64")) {
                    val f = File(imgDir, "${UUID.randomUUID()}.jpg")
                    f.writeBytes(Base64.decode(o.getString("imageB64"), Base64.DEFAULT))
                    path = f.absolutePath
                    keep.add(path)
                }
                WantEntity(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    price = o.optDouble("price", 0.0),
                    link = o.optString("link", ""),
                    imagePath = path,
                    notes = o.optString("notes", ""),
                    priority = runCatching { Priority.valueOf(o.getString("priority")) }.getOrDefault(Priority.WANT),
                    categoryId = o.longOrNull("categoryId"),
                    plannedYear = o.intOrNull("plannedYear"),
                    plannedQuarter = o.intOrNull("plannedQuarter"),
                    status = runCatching { Status.valueOf(o.getString("status")) }.getOrDefault(Status.NOT_STARTED),
                    failReason = o.optString("failReason", ""),
                    pricePaid = o.doubleOrNull("pricePaid"),
                    addedAt = o.getLong("addedAt"),
                    boughtAt = o.longOrNull("boughtAt"),
                    failedAt = o.longOrNull("failedAt"),
                    coolingUntil = o.longOrNull("coolingUntil"),
                    lastStatusChangeAt = o.getLong("lastStatusChangeAt")
                )
            }
        }

        val logs = root.getJSONArray("logs").let { arr ->
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                StatusLogEntity(
                    id = o.getLong("id"),
                    wantId = o.getLong("wantId"),
                    status = runCatching { Status.valueOf(o.getString("status")) }.getOrDefault(Status.NOT_STARTED),
                    at = o.getLong("at"),
                    note = o.optString("note", "")
                )
            }
        }

        val db = app.db
        db.withTransaction {
            db.logDao().deleteAll()
            db.wantDao().deleteAll()
            db.categoryDao().deleteAll()
            db.categoryDao().insertAll(cats)
            db.wantDao().insertAll(wants)
            db.logDao().insertAll(logs)
        }

        // Remove images that no longer belong to any want, and re-arm cooling-off reminders.
        imgDir.listFiles()?.forEach { if (it.absolutePath !in keep) runCatching { it.delete() } }
        wants.forEach { w ->
            val until = w.coolingUntil
            if (until != null && w.status.isOpen) Reminders.scheduleCooling(app, w.id, until)
            else Reminders.cancelCooling(app, w.id)
        }
        WantsWidgetProvider.refreshAll(app)
        wants.size
    }
}
