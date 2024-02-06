package com.beyondnull.flexibletodos

import android.app.Application
import com.beyondnull.flexibletodos.manager.AlarmManager
import com.beyondnull.flexibletodos.manager.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the Timber logging lib
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Trigger notifications and alarms based on task state
        NotificationManager.watchTasksAndUpdateNotifications(
            this,
            applicationScope
        )

        AlarmManager(this, applicationScope).watchTasksAndSetAlarm()
    }

    companion object {
        // https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
        // https://developer.android.com/topic/architecture/data-layer#make_an_operation_live_longer_than_the_screen
        // This might be equivalent to `kotlinx.coroutines.MainScope()`?
        // No need to cancel this scope as it'll be torn down with the process
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}