package com.example.pomodorotimer.data.entity

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "work_interval")
data class WorkInterval(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @NonNull val activity: String,
    @NonNull val time: LocalTime,
    @NonNull val date: LocalDate
)