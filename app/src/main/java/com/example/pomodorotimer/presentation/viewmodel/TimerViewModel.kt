package com.example.pomodorotimer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import java.time.LocalDate

class TimerViewModel : ViewModel() {
    private var status: String = ""
    private var typeActivity: String = "Work"
    private val periodActivity: Array<Int> = arrayOf(0, 0, 0)
    private var activity: Int = 0

    fun getPeriod(): Int { return periodActivity[activity] }
    fun getStatus(): String { return status }
    fun getTypeActivity(): String { return typeActivity }

    fun start() {
        checkStatus()
    }

    fun skipToNext() {
        if(periodActivity[activity] != 7) periodActivity[activity] += 1 else periodActivity[activity] = 0
        checkStatus()
    }

    fun moveToNextActivity() {
        periodActivity[activity] = 0
        if(activity != 2) activity += 1 else activity = 0
        setTypeActivity()
        checkStatus()
    }

    private fun setTypeActivity() {
        typeActivity = when(activity) {
            0 -> "Work"
            1 -> "Hobby"
            else -> "Education"
        }
    }

    private fun checkStatus() {
        status = when(periodActivity[activity]) {
            in arrayOf(0, 2, 4, 6) -> typeActivity
            else -> "Relax"
        }
    }
}