package com.example.taskmanagerapp

import android.os.Bundle
import android.view.*
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
import com.example.taskmanagerapp.ui.viewmodel.FilterType
import com.example.taskmanagerapp.ui.viewmodel.SortType
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModelFactory
import com.google.android.material.snackbar.Snackbar

class TaskListFragment : Fragment(R.layout.fragment_task_list) {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TaskAdapter
    private lateinit var repository: TaskRepository

    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
                val action = TaskListFragmentDirections
                    .actionTaskListFragmentToTaskDetailFragment(task.id)
                findNavController().navigate(action)
            },
            onCheckChanged = { task, isChecked ->
                val updatedTask = task.copy(isDone = isChecked)
                viewModel.update(updatedTask)
            }
        )

        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter
        
        // הגדרת החלקה למחיקת משימה
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
                
                // מציגים הודעה עם אפשרות חרטה
                Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {
                        viewModel.insert(task)
                    }
                    .show()
            }
        }
        
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewTasks)

        binding.fabAddTask.setOnClickListener {
            findNavController().navigate(R.id.action_taskListFragment_to_addEditTaskFragment)
        }

        // מאזינים לשינויים ברשימת המשימות ומעדכנים את המסך
        viewModel.tasks.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateProgress(list)
        }
    }

    // פונקציה לחישוב ההתקדמות
    private fun updateProgress(tasks: List<Task>) {
        val total = tasks.size
        val done = tasks.count { it.isDone }

        if (total > 0) {
            val progress = (done * 100) / total
            binding.progressBarToday.progress = progress
        } else {
            binding.progressBarToday.progress = 0
        }
        
        binding.textProgressCount.text = getString(R.string.progress_text, done, total)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.task_filter_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // טיפול בלחיצות על התפריט
        return when (item.itemId) {
            R.id.sort_by_date -> {
                viewModel.setSort(SortType.DATE)
                true
            }
            R.id.sort_by_priority -> {
                viewModel.setSort(SortType.PRIORITY)
                true
            }
            R.id.filter_all -> {
                viewModel.setFilter(FilterType.ALL)
                true
            }
            R.id.filter_active -> {
                viewModel.setFilter(FilterType.ACTIVE)
                true
            }
            R.id.filter_done -> {
                viewModel.setFilter(FilterType.DONE)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
