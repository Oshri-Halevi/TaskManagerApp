package com.example.taskmanagerapp

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.taskmanagerapp.data.local.AppDatabase
import com.example.taskmanagerapp.data.repository.TaskRepository
import com.example.taskmanagerapp.databinding.FragmentSettingsBinding
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModelFactory
import java.util.*

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TaskRepository
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(repository)
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = TaskRepository(
            AppDatabase.getDatabase(requireContext()).taskDao()
        )
        prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.btnDeleteAll.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }

        // קובעים את מצב המתג לפי המצב האמיתי של האפליקציה
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        binding.switchDarkMode.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        // כשהמשתמש משנה את המתג, שומרים את הבחירה ומחילים את ערכת הנושא
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            applyTheme(isChecked)
        }

        val deviceLang = Locale.getDefault().language
        val savedLang = prefs.getString("language", null)
        val currentLang = savedLang ?: deviceLang
        if (currentLang == "iw" || currentLang == "he") {
            binding.radioHebrew.isChecked = true
        } else {
            binding.radioEnglish.isChecked = true
        }

        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            val selectedLangCode = when (checkedId) {
                R.id.radioHebrew -> "iw"
                else -> "en"
            }
            if (currentLang != selectedLangCode) {
                prefs.edit().putString("language", selectedLangCode).apply()
                requireActivity().recreate()
            }
        }
        
        val fontSize = prefs.getString("font_size", "normal")
        when (fontSize) {
            "small" -> binding.radioFontSmall.isChecked = true
            "large" -> binding.radioFontLarge.isChecked = true
            else -> binding.radioFontNormal.isChecked = true
        }

        binding.radioGroupFont.setOnCheckedChangeListener { _, checkedId ->
            val size = when (checkedId) {
                R.id.radioFontSmall -> "small"
                R.id.radioFontLarge -> "large"
                else -> "normal"
            }
            if(prefs.getString("font_size", "normal") != size) {
                prefs.edit().putString("font_size", size).apply()
                requireActivity().recreate()
            }
        }
    }

    private fun showDeleteAllConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_all_title)
            .setMessage(R.string.dialog_delete_all_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deleteAllTasks()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun applyTheme(isDark: Boolean) {
        val mode = if (isDark) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
