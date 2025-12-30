package com.example.taskmanagerapp

import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.taskmanagerapp.data.local.AppDatabase
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.data.repository.TaskRepository
import com.example.taskmanagerapp.databinding.FragmentAddEditTaskBinding
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModelFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.Date

class AddEditTaskFragment : Fragment(R.layout.fragment_add_edit_task) {

    private var _binding: FragmentAddEditTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TaskRepository
    private val args: AddEditTaskFragmentArgs by navArgs()

    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(repository)
    }

    private var selectedImageUri: String? = null
    private var selectedDueDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            selectedImageUri = savedInstanceState.getString("image_uri")
            val date = savedInstanceState.getLong("due_date", -1)
            if (date != -1L) selectedDueDate = date
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddEditTaskBinding.bind(view)

        repository = TaskRepository(
            AppDatabase.getDatabase(requireContext()).taskDao()
        )

        if (selectedImageUri != null) {
            binding.imagePreview.setImageURI(android.net.Uri.parse(selectedImageUri))
        }
        updateDateText()
        setupPriorityChips()

        if (args.taskId != -1) {
            viewLifecycleOwner.lifecycleScope.launch {
                val task = viewModel.getTask(args.taskId)
                if (task != null) {
                    binding.editTitle.setText(task.title)
                    binding.editDescription.setText(task.description)
                    
                    when(task.priority) {
                        0 -> binding.chipGroupPriority.check(R.id.chipLow)
                        2 -> binding.chipGroupPriority.check(R.id.chipHigh)
                        else -> binding.chipGroupPriority.check(R.id.chipNormal)
                    }

                    if (selectedDueDate == null) {
                        selectedDueDate = task.dueDate
                        updateDateText()
                    }

                    if (selectedImageUri == null) {
                        selectedImageUri = task.imageUri
                        if (!task.imageUri.isNullOrEmpty()) {
                            Glide.with(requireContext())
                                .load(task.imageUri)
                                .into(binding.imagePreview)
                        }
                    }
                }
            }
        } else {
             binding.chipGroupPriority.check(R.id.chipNormal)
        }

        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveTask()
        }
    }
    private fun setupPriorityChips() {
        binding.chipGroupPriority.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as Chip
                if (chip == selectedChip) {
                    chip.setChipBackgroundColorResource(R.color.selectedPriority)
                    chip.setTextColor(Color.WHITE)
                } else {
                    val colorRes = when (chip.id) {
                        R.id.chipLow -> R.color.priority_low
                        R.id.chipNormal -> R.color.priority_normal
                        R.id.chipHigh -> R.color.priority_high
                        else -> R.color.priority_normal
                    }
                    chip.setChipBackgroundColorResource(colorRes)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorPrimary))
                }
            }
        }
    }
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (selectedDueDate != null) {
            calendar.timeInMillis = selectedDueDate!!
        }
        
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDueDate = calendar.timeInMillis
                updateDateText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun updateDateText() {
        if (selectedDueDate != null) {
            val dateString = DateFormat.getDateFormat(requireContext()).format(Date(selectedDueDate!!))
            binding.textDueDate.text = getString(R.string.label_due_date, dateString)
        } else {
            binding.textDueDate.text = getString(R.string.no_date_selected)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("image_uri", selectedImageUri)
        if (selectedDueDate != null) {
            outState.putLong("due_date", selectedDueDate!!)
        }
    }

    private fun saveTask() {
        val title = binding.editTitle.text.toString()
        val description = binding.editDescription.text.toString()

        if (title.isBlank()) {
            val errorMsg = getString(R.string.toast_title_required)
            binding.inputLayoutTitle.error = getString(R.string.error_title_required)
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            return
        } else {
            binding.inputLayoutTitle.error = null
        }

        val priority = when(binding.chipGroupPriority.checkedChipId) {
            R.id.chipLow -> 0
            R.id.chipHigh -> 2
            else -> 1
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (args.taskId == -1) {
                val task = Task(
                    title = title,
                    description = description,
                    isDone = false,
                    imageUri = selectedImageUri,
                    priority = priority,
                    dueDate = selectedDueDate
                )
                viewModel.insert(task)
                findNavController().navigateUp()
            } else {
                val old = viewModel.getTask(args.taskId)
                if (old != null) {
                    val updated = old.copy(
                        title = title,
                        description = description,
                        imageUri = selectedImageUri,
                        priority = priority,
                        dueDate = selectedDueDate
                    )
                    viewModel.update(updated)
                }
                val action = AddEditTaskFragmentDirections.actionAddEditTaskFragmentToTaskListFragment()
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it.toString()
            binding.imagePreview.setImageURI(it)
            binding.imagePreview.requestFocus()
        }
    }
}
