package com.example.pomodorotimer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.pomodorotimer.data.entity.WorkInterval
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface WorkIntervalDao {
    @Query("SELECT date FROM work_interval GROUP BY date")
    fun getAllDate(): Flow<List<LocalDate>>

    @Query("SELECT time FROM work_interval WHERE date = :date AND activity = :activity")
    fun getAllTimeByActivity(date: LocalDate?, activity: String): Flow<List<LocalTime>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(workInterval: WorkInterval)

    @Query("SELECT * FROM work_interval WHERE date = :date")
    suspend fun getWorkIntervalsByDate(date: LocalDate): List<WorkInterval>

    @Delete
    suspend fun delete(workInterval: WorkInterval)

    @Transaction
    suspend fun deleteWorkIntervalsByDate(date: LocalDate) {
        val workIntervalsToDelete = getWorkIntervalsByDate(date)
        for (workInterval in workIntervalsToDelete) {
            delete(workInterval)
        }
    }
}