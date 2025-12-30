package com.example.taskmanagerapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val isDone: Boolean = false,
    val imageUri: String? = null,
    val priority: Int = 1, // 0 = Low, 1 = Normal, 2 = High
    val dueDate: Long? = null // Timestamp in milliseconds
)
