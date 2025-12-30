package com.example.taskmanagerapp.data.repository

import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.data.local.TaskDao

// מחלקה שמתווכת בין הנתונים לבין התצוגה
// ככה הלוגיקה של התצוגה לא צריכה להכיר את המסד נתונים ישירות
class TaskRepository(private val dao: TaskDao) {

    // מחזיר רשימה שמתעדכנת אוטומטית כשיש שינוי בטבלה
    fun getAllTasks() = dao.getAllTasks()

    suspend fun insert(task: Task) = dao.insert(task)

    suspend fun update(task: Task) = dao.update(task)

    suspend fun delete(task: Task) = dao.delete(task)

    suspend fun deleteAllTasks() = dao.deleteAllTasks()

    suspend fun getTask(id: Int) = dao.getTaskById(id)
}
