package com.example.taskmanagerapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.data.repository.TaskRepository
import kotlinx.coroutines.launch

enum class FilterType {
    ALL, ACTIVE, DONE
}

enum class SortType {
    DATE, PRIORITY
}

// המחלקה הזאת אחראית על כל הלוגיקה של האפליקציה ושומרת על הנתונים כשהמסך מסתובב
class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    // משתנים לשמירת המצב של הסינון והמיון
    private val _filter = MutableLiveData(FilterType.ALL)
    val filter: LiveData<FilterType> = _filter

    private val _sort = MutableLiveData(SortType.DATE)

    // מקבל את כל המשימות מהמאגר נתונים
    private val _allTasks = repository.getAllTasks()

    // מאזין לשינויים בפילטר ובמיון ומעדכן את הרשימה שמוצגת
    val tasks: LiveData<List<Task>> = _filter.switchMap { currentFilter ->
        _sort.switchMap { currentSort ->
            _allTasks.map { tasks ->
                // קודם כל מסננים את הרשימה לפי מה שהמשתמש בחר
                val filtered = when (currentFilter) {
                    FilterType.ALL -> tasks
                    FilterType.ACTIVE -> tasks.filter { !it.isDone }
                    FilterType.DONE -> tasks.filter { it.isDone }
                    null -> tasks
                }

                // אחרי זה ממיינים אותה
                when (currentSort) {
                    // מיון לפי תאריך: קודם משימות פתוחות, אח"כ לפי תאריך
                    SortType.DATE -> filtered.sortedWith(compareBy<Task> { it.isDone }
                        .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    )
                    // מיון לפי חשיבות: קודם משימות פתוחות, אח"כ לפי עדיפות יורדת
                    SortType.PRIORITY -> filtered.sortedWith(compareBy<Task> { it.isDone }
                        .thenByDescending { it.priority }
                    )
                    null -> filtered.sortedBy { it.isDone }
                }
            }
        }
    }

    // פונקציות להוספה, עדכון ומחיקה שקוראות למאגר נתונים ברקע
    fun insert(task: Task) {
        viewModelScope.launch {
            repository.insert(task)
        }
    }

    fun update(task: Task) = viewModelScope.launch {
        repository.update(task)
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            repository.deleteAllTasks()
        }
    }

    suspend fun getTask(id: Int): Task? {
        return repository.getTask(id)
    }

    fun setFilter(type: FilterType) {
        _filter.value = type
    }

    fun setSort(type: SortType) {
        _sort.value = type
    }
}
