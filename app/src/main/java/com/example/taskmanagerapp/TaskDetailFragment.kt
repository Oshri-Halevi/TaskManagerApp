package com.example.taskmanagerapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.taskmanagerapp.data.local.AppDatabase
import com.example.taskmanagerapp.data.repository.TaskRepository
import com.example.taskmanagerapp.databinding.FragmentTaskDetailBinding
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModelFactory
import kotlinx.coroutines.launch

class TaskDetailFragment : Fragment(R.layout.fragment_task_detail) {

    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!

    private val args: TaskDetailFragmentArgs by navArgs()

    private lateinit var repository: TaskRepository

    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentTaskDetailBinding.bind(view)

        repository = TaskRepository(
            AppDatabase.getDatabase(requireContext()).taskDao()
        )

        val id = args.taskId

        viewLifecycleOwner.lifecycleScope.launch {
            val task = viewModel.getTask(id)

            if (task != null) {
                binding.titleText.text = task.title
                binding.descriptionText.text = task.description

                if (!task.imageUri.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(task.imageUri)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(binding.detailImage)
                } else {
                    binding.detailImage.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                // If task is null (e.g. deleted), go back
                findNavController().navigateUp()
            }

            binding.btnEdit.setOnClickListener {
                val action =
                    TaskDetailFragmentDirections.actionTaskDetailFragmentToAddEditTaskFragment(id)
                findNavController().navigate(action)
            }
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(id)
        }
    }

    private fun showDeleteConfirmationDialog(taskId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val task = viewModel.getTask(taskId)
                    if (task != null) {
                        viewModel.delete(task)
                        findNavController().navigateUp()
                    }
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
