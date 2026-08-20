package com.dean.hourstracker

import android.app.Application
import com.dean.hourstracker.di.AppContainer

class HoursTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
