package com.example.taskmanagerapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagerapp.data.local.AppDatabase
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.data.repository.TaskRepository
import com.example.taskmanagerapp.databinding.FragmentTaskListBinding
import com.example.taskmanagerapp.ui.TaskAdapter
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModelFactory
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class TodayTasksFragment : Fragment(R.layout.fragment_task_list) {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TaskAdapter
    private lateinit var repository: TaskRepository

    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = TaskRepository(
            AppDatabase.getDatabase(requireContext()).taskDao()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentTaskListBinding.bind(view)

        adapter = TaskAdapter(
            emptyList(),
            onItemClick = { task ->
                val action = TodayTasksFragmentDirections
                    .actionTodayTasksFragmentToTaskDetailFragment(task.id)
                findNavController().navigate(action)
            },
            onCheckChanged = { task, isChecked ->
                val updatedTask = task.copy(isDone = isChecked)
                viewModel.update(updatedTask)
            }
        )

        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val task = adapter.getTaskAt(position)
                
                viewModel.delete(task)
                
                Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {
                        viewModel.insert(task)
                    }
                    .show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewTasks)

        binding.fabAddTask.setOnClickListener {
            findNavController().navigate(R.id.action_todayTasksFragment_to_addEditTaskFragment)
        }

        viewModel.tasks.observe(viewLifecycleOwner) { list ->
            val todayStart = getStartOfDay()
            val todayEnd = getEndOfDay()
            
            val todayList = list.filter { task ->
                task.dueDate != null && 
                task.dueDate >= todayStart && 
                task.dueDate <= todayEnd
            }
            adapter.submitList(todayList)
            updateProgress(todayList)
        }
    }

    private fun updateProgress(todayList: List<Task>) {
        val total = todayList.size
        val done = todayList.count { it.isDone }

        if (total > 0) {
            val progress = (done * 100) / total
            binding.progressBarToday.progress = progress
        } else {
            binding.progressBarToday.progress = 0
        }
        binding.textProgressCount.text = getString(R.string.progress_text, done, total)
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
