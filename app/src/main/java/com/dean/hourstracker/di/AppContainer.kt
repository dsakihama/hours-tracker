package com.dean.hourstracker.di

import android.content.Context
import com.dean.hourstracker.data.AppDatabase
import com.dean.hourstracker.data.HoursRepository

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    val repository = HoursRepository(database.projectDao(), database.entryDao())
}
