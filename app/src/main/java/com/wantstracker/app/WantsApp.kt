package com.wantstracker.app

import android.app.Application
import androidx.room.Room
import com.wantstracker.app.data.AppDatabase
import com.wantstracker.app.data.Repository
import com.wantstracker.app.notify.Notifier
import com.wantstracker.app.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WantsApp : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var repository: Repository
        private set
    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "wants.db").build()
        prefs = Prefs(this)
        repository = Repository(this, db)
        Notifier.createChannel(this)
        Reminders.scheduleDaily(this)
        CoroutineScope(Dispatchers.IO).launch {
            if (!prefs.seeded) {
                repository.seedDefaults()
                prefs.seeded = true
            }
        }
    }
}
