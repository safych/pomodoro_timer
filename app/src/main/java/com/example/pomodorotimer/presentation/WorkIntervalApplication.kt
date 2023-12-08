package com.example.pomodorotimer.presentation

import android.app.Application
import com.example.pomodorotimer.data.database.ApplicationDatabase

class WorkIntervalApplication : Application() {
    val database: ApplicationDatabase by lazy { ApplicationDatabase.getDatabase(this) }
}