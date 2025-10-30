package com.rodrigo.googletask_clone.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.rodrigo.googletask_clone.R
import com.rodrigo.googletask_clone.data.Task
import com.rodrigo.googletask_clone.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTaskBottomSheet : BottomSheetDialogFragment() {

    private val taskViewModel: TaskViewModel by activityViewModels()
    private var dueDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleEditText = view.findViewById<TextInputEditText>(R.id.new_task_title)
        val detailsEditText = view.findViewById<TextInputEditText>(R.id.new_task_details)
        val dateTimeTextView = view.findViewById<TextView>(R.id.new_task_date_time)
        val saveButton = view.findViewById<Button>(R.id.button_save_new)
        val cancelButton = view.findViewById<Button>(R.id.button_cancel)

        dateTimeTextView.setOnClickListener { showDateTimePicker(dateTimeTextView) }
        cancelButton.setOnClickListener { dismiss() }

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            if (title.isBlank()) {
                Toast.makeText(requireContext(), "O título é obrigatório", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val details = detailsEditText.text.toString()
            val categoryId = taskViewModel.selectedCategoryId.value
            val newTask = Task(
                title = title,
                description = details,
                dueDate = dueDate?.timeInMillis,
                categoryId = categoryId,
                orderPosition = System.currentTimeMillis() // Garante que a nova tarefa fique no topo
            )
            taskViewModel.insert(newTask)
            dismiss() // Fecha o bottom sheet
        }
    }

    private fun showDateTimePicker(textView: TextView) {
        val currentCalendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                dueDate = selectedDate
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                textView.text = sdf.format(selectedDate.time)

            }, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), true).show()

        }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    companion object {
        const val TAG = "AddTaskBottomSheet"
    }
}
