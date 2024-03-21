package com.example.pomodorotimer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pomodorotimer.data.dao.WorkIntervalDao
import com.example.pomodorotimer.data.entity.WorkInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class HistoryViewModel(private val workIntervalDao: WorkIntervalDao): ViewModel() {
    var workIntervalDates: Flow<List<LocalDate>> = workIntervalDao.getAllDate()

    var getTimeForWork: Flow<List<LocalTime>>? = null
    var getTimeForHobby: Flow<List<LocalTime>>? = null
    var getTimeForEducation: Flow<List<LocalTime>>? = null

    fun setTime(date: LocalDate) {
        getTimeForWork = workIntervalDao.getAllTimeByActivity(date = date, activity = "Work")
        getTimeForHobby = workIntervalDao.getAllTimeByActivity(date = date, activity = "Hobby")
        getTimeForEducation = workIntervalDao.getAllTimeByActivity(date = date, activity = "Education")
    }

    fun deleteByDate(date: LocalDate) {
        viewModelScope.launch {
            workIntervalDao.deleteWorkIntervalsByDate(date = date)
        }
    }

    fun addNewWorkInterval(activity: String, time: Int) {
        val newWorkInterval = getNewWorkIntervalEntry(activity, time)
        insertWorkInterval(newWorkInterval)
    }

    private fun insertWorkInterval(workInterval: WorkInterval) {
        viewModelScope.launch {
            workIntervalDao.insert(workInterval)
        }
    }

    private fun getNewWorkIntervalEntry(activity: String, time: Int): WorkInterval {
        val minute: LocalTime = LocalTime.of(0, time)
        return WorkInterval(
            activity = activity,
            time = minute,
            date = LocalDate.now()
        )
    }
}

class WorkIntervalViewModelFactory(
    private val workIntervalDao: WorkIntervalDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(workIntervalDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}