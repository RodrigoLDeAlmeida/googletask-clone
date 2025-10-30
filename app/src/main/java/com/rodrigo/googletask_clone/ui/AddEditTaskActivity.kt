package com.rodrigo.googletask_clone.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.rodrigo.googletask_clone.R
import com.rodrigo.googletask_clone.data.Category
import com.rodrigo.googletask_clone.data.Task
import com.rodrigo.googletask_clone.viewmodel.CategoryViewModel
import com.rodrigo.googletask_clone.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

class AddEditTaskActivity : AppCompatActivity() {

    private val taskViewModel: TaskViewModel by viewModels()
    private val categoryViewModel: CategoryViewModel by viewModels()
    private var currentTask: Task? = null
    private var parentId: Int? = null
    private var dueDate: Long? = null

    private lateinit var titleEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var categorySpinner: Spinner
    private lateinit var categoryLabel: TextView
    private lateinit var dueDateText: TextView
    private lateinit var changeDateButton: Button
    private lateinit var markCompletedButton: Button
    private lateinit var subtaskSection: LinearLayout
    private lateinit var subtaskListContainer: LinearLayout
    private lateinit var toolbar: Toolbar
    private lateinit var addSubtaskButton: Button

    private var spinnerCategories: List<Category> = emptyList()

    private val subtaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* Opcional: Atualizar a UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_task)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bindViews()
        getIntentData()
        populateUI()
        configureUIForTaskType()
        setupCategorySpinner()
        setAddSubtaskButtonColor()
        setupDatePicker()
    }

    private fun bindViews() {
        titleEditText = findViewById(R.id.edit_task_title)
        descriptionEditText = findViewById(R.id.edit_task_description)
        categorySpinner = findViewById(R.id.spinner_category)
        categoryLabel = findViewById(R.id.text_view_category)
        dueDateText = findViewById(R.id.due_date_text)
        changeDateButton = findViewById(R.id.change_date_button)
        markCompletedButton = findViewById(R.id.button_mark_completed)
        subtaskSection = findViewById(R.id.subtask_section)
        subtaskListContainer = findViewById(R.id.subtask_list_container)
        addSubtaskButton = findViewById(R.id.add_subtask_button)
    }

    private fun getIntentData() {
        currentTask = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_TASK, Task::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Task>(EXTRA_TASK)
        }
        parentId = intent.getIntExtra(EXTRA_PARENT_ID, 0).takeIf { it > 0 }
        dueDate = currentTask?.dueDate
    }

    private fun populateUI() {
        currentTask?.let {
            titleEditText.setText(it.title)
            descriptionEditText.setText(it.description)
            updateCompletedButtonText(it.isCompleted)
            markCompletedButton.setOnClickListener { toggleCompletedStatus() }
            observeSubtasks(it.id)
        }
        updateDueDateText()
    }

    private fun setupCategorySpinner() {
        categoryViewModel.allCategories.observe(this) { categories ->
            spinnerCategories = categories
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories.map { it.name })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = adapter

            currentTask?.categoryId?.let { categoryId ->
                val position = categories.indexOfFirst { it.id == categoryId }
                if (position >= 0) categorySpinner.setSelection(position)
            }
        }
    }

    private fun setupDatePicker() {
        changeDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            dueDate?.let { calendar.timeInMillis = it }
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dueDate = calendar.timeInMillis
                    updateDueDateText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDueDateText() {
        if (dueDate != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dueDateText.text = "Vence em: ${sdf.format(Date(dueDate!!))}"
        } else {
            dueDateText.text = ""
        }
    }

    private fun observeSubtasks(taskId: Int) {
        taskViewModel.getTaskWithSubtasks(taskId).observe(this) { taskWithSubtasks ->
            taskWithSubtasks?.let { displaySubtasks(it.subtasks) }
        }
    }

    private fun displaySubtasks(subtasks: List<Task>) {
        subtaskListContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (subtask in subtasks) {
            val subtaskView = inflater.inflate(R.layout.subtask_item, subtaskListContainer, false)
            val title = subtaskView.findViewById<TextView>(R.id.task_title)
            val description = subtaskView.findViewById<TextView>(R.id.subtask_description)
            val checkbox = subtaskView.findViewById<CheckBox>(R.id.task_checkbox)

            title.text = subtask.title
            if (!subtask.description.isNullOrBlank()) {
                description.text = subtask.description
                description.visibility = View.VISIBLE
            } else {
                description.visibility = View.GONE
            }

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = subtask.isCompleted
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                taskViewModel.update(subtask.copy(isCompleted = isChecked))
            }

            subtaskView.setOnClickListener { openTaskDetails(subtask) }
            subtaskListContainer.addView(subtaskView)
        }
    }

    private fun openTaskDetails(task: Task?) {
        val intent = Intent(this, AddEditTaskActivity::class.java).apply {
            putExtra(EXTRA_TASK, task)
        }
        subtaskLauncher.launch(intent)
    }

    private fun configureUIForTaskType() {
        if (parentId != null || currentTask?.parentId != null) {
            subtaskSection.visibility = View.GONE
            categorySpinner.visibility = View.GONE
            categoryLabel.visibility = View.GONE
            supportActionBar?.title = if (currentTask == null) "Nova Subtarefa" else "Editar Subtarefa"
        } else {
            supportActionBar?.title = if (currentTask == null) "Nova Tarefa" else "Editar Tarefa"
            if (currentTask != null) {
                addSubtaskButton.setOnClickListener { openSubtaskCreation() }
            } else {
                subtaskSection.visibility = View.GONE
            }
        }
    }

    private fun setAddSubtaskButtonColor() {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val color = typedValue.data
        addSubtaskButton.compoundDrawables.firstOrNull()?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private fun openSubtaskCreation() {
        val intent = Intent(this, AddEditTaskActivity::class.java).apply {
            putExtra(EXTRA_PARENT_ID, currentTask!!.id)
        }
        subtaskLauncher.launch(intent)
    }

    private fun toggleCompletedStatus() {
        currentTask?.let {
            val newStatus = !it.isCompleted
            currentTask = it.copy(isCompleted = newStatus)
            updateCompletedButtonText(newStatus)
        }
    }

    private fun updateCompletedButtonText(isCompleted: Boolean) {
        markCompletedButton.text = if (isCompleted) "Marcar como pendente" else "Marcar como concluída"
    }

    private fun saveTask() {
        val title = titleEditText.text.toString()
        if (title.isBlank()) {
            Toast.makeText(this, "O título é obrigatório", Toast.LENGTH_SHORT).show()
            return
        }
        val description = descriptionEditText.text.toString()
        val categoryId = if (categorySpinner.visibility == View.VISIBLE) {
            val selectedPosition = categorySpinner.selectedItemPosition
            spinnerCategories.getOrNull(selectedPosition)?.id
        } else {
            currentTask?.categoryId
        }

        val taskToSave = currentTask?.copy(
            title = title,
            description = description,
            categoryId = categoryId,
            dueDate = dueDate
        ) ?: Task(
            title = title,
            description = description,
            parentId = parentId,
            categoryId = categoryId,
            dueDate = dueDate
        )

        if (taskToSave.id > 0) taskViewModel.update(taskToSave) else taskViewModel.insert(taskToSave)
        
        Toast.makeText(this, "Tarefa salva", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteTask() {
        currentTask?.let {
            AlertDialog.Builder(this)
                .setTitle("Excluir Tarefa")
                .setMessage("Tem certeza que deseja excluir esta tarefa?")
                .setPositiveButton("Excluir") { _, _ ->
                    taskViewModel.delete(it)
                    Toast.makeText(this, "Tarefa excluída", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.add_edit_task_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save_task -> {
                saveTask()
                true
            }
            R.id.action_delete_task -> {
                deleteTask()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_TASK = "com.rodrigo.googletask_clone.EXTRA_TASK"
        const val EXTRA_PARENT_ID = "com.rodrigo.googletask_clone.EXTRA_PARENT_ID"
    }
}
