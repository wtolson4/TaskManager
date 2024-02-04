package com.beyondnull.flexibletodos

import android.app.Application
import com.beyondnull.flexibletodos.BuildConfig
import timber.log.Timber

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the Timber logging lib
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}