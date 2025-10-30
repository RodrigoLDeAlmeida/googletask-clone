package com.rodrigo.googletask_clone

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rodrigo.googletask_clone.data.Category
import com.rodrigo.googletask_clone.data.Task
import com.rodrigo.googletask_clone.data.TaskWithSubtasks
import com.rodrigo.googletask_clone.ui.AddEditTaskActivity
import com.rodrigo.googletask_clone.ui.AddTaskBottomSheet
import com.rodrigo.googletask_clone.ui.TaskListAdapter
import com.rodrigo.googletask_clone.ui.TaskListItem
import com.rodrigo.googletask_clone.viewmodel.TaskViewModel

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val taskViewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskListAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private var isCompletedSectionExpanded = false
    private var pendingTasks: List<TaskWithSubtasks> = emptyList()
    private var completedTasks: List<TaskWithSubtasks> = emptyList()
    private val expandedTaskIds = mutableSetOf<Int>()

    private val addEditTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* Opcional: Tratar resultado */ }
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            writeCsvToUri(uri)
        }
    }

    private var pendingCsvData: String? = null
    private var pendingCsvSuggestedName: String = "completed_tasks.csv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDarkMode()
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        setupTaskRecyclerView()
        setupFab()
        observeViewModel()
        setupDarkModeSwitch()
    }

    private fun setupTaskRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.task_recycler_view)
        taskAdapter = TaskListAdapter(
            onTaskCheckedChanged = { task, isChecked -> taskViewModel.update(task.copy(isCompleted = isChecked)) },
            onTaskClicked = { task -> openTaskDetails(task) },
            onHeaderClicked = { 
                isCompletedSectionExpanded = !isCompletedSectionExpanded
                updateAdapterData()
            },
            onExpandClicked = { task ->
                if (expandedTaskIds.contains(task.id)) {
                    expandedTaskIds.remove(task.id)
                } else {
                    expandedTaskIds.add(task.id)
                }
                updateAdapterData()
            }
        )
        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        setupItemGestures(recyclerView)
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            AddTaskBottomSheet().show(supportFragmentManager, AddTaskBottomSheet.TAG)
        }
    }

    private fun observeViewModel() {
        taskViewModel.pendingTasks.observe(this) { pending ->
            pendingTasks = pending
            updateAdapterData()
        }
        taskViewModel.completedTasks.observe(this) { completed ->
            completedTasks = completed
            updateAdapterData()
        }
        taskViewModel.allCategories.observe(this) { categories ->
            updateDrawerMenu(categories)
        }
    }

    private fun updateAdapterData() {
        val items = mutableListOf<TaskListItem>()
        pendingTasks.forEach { taskWithSubtasks ->
            items.add(TaskListItem.TaskItem(taskWithSubtasks.task, taskWithSubtasks.subtasks.size))
            if (expandedTaskIds.contains(taskWithSubtasks.task.id)) {
                taskWithSubtasks.subtasks.forEach { subtask ->
                    items.add(TaskListItem.TaskItem(subtask))
                }
            }
        }

        if (completedTasks.isNotEmpty()) {
            items.add(TaskListItem.HeaderItem(completedTasks.size))
            if (isCompletedSectionExpanded) {
                completedTasks.forEach { items.add(TaskListItem.TaskItem(it.task)) }
            }
        }
        taskAdapter.expandedTaskIds = expandedTaskIds
        taskAdapter.isCompletedSectionExpanded = isCompletedSectionExpanded
        taskAdapter.submitList(items)
    }
    
    private fun updateDrawerMenu(categories: List<Category>) {
        val menu = navigationView.menu
        val currentCatId = taskViewModel.selectedCategoryId.value

        menu.removeGroup(R.id.category_group)

        val allTasksItem = menu.add(R.id.category_group, View.NO_ID, 0, "Todas as Tarefas").apply {
            setIcon(android.R.drawable.ic_menu_agenda)
            isCheckable = true
        }

        categories.forEachIndexed { index, category ->
            menu.add(R.id.category_group, category.id, index + 1, category.name).apply {
                setIcon(R.drawable.ic_launcher_foreground) // Ícone placeholder
                isCheckable = true
            }
        }
        menu.setGroupCheckable(R.id.category_group, true, true)
        
        val itemToSelect = menu.findItem(currentCatId ?: allTasksItem.itemId)
        itemToSelect?.isChecked = true
    }

    private fun setupDarkModeSwitch() {
        val switchItem = navigationView.menu.findItem(R.id.action_dark_mode)
        val switchView = switchItem.actionView as SwitchMaterial
        val sharedPrefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        switchView.isChecked = sharedPrefs.getBoolean("dark_mode", false)
        switchView.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun applyDarkMode() {
        val sharedPrefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val useDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (useDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_dark_mode) {
            return false
        }

        when (item.groupId) {
            R.id.category_group -> {
                val categoryId = if (item.itemId == View.NO_ID) null else item.itemId
                taskViewModel.selectCategory(categoryId)
                supportActionBar?.title = item.title
            }
            else -> when (item.itemId) {
                R.id.action_new_list -> showCreateListDialog()
                R.id.action_export_completed -> showExportOptions()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setupItemGestures(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION || position >= taskAdapter.currentList.size) {
                    return
                }
                val listItem = (taskAdapter.currentList[position] as? TaskListItem.TaskItem) ?: return

                if (direction == ItemTouchHelper.RIGHT) {
                    taskViewModel.update(listItem.task.copy(isCompleted = true))
                } else if (direction == ItemTouchHelper.LEFT) {
                    taskViewModel.delete(listItem.task)
                }
            }
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder is TaskListAdapter.HeaderViewHolder) return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun openTaskDetails(task: Task) {
        val intent = Intent(this, AddEditTaskActivity::class.java).apply {
            putExtra(AddEditTaskActivity.EXTRA_TASK, task)
        }
        addEditTaskLauncher.launch(intent)
    }

    private fun showExportOptions() {
        val completed = completedTasks.map { it.task }
        if (completed.isEmpty()) {
            Toast.makeText(this, "Não há tarefas concluídas", Toast.LENGTH_SHORT).show()
            return
        }
        pendingCsvData = buildCompletedCsv(completed)
        pendingCsvSuggestedName = "completed_tasks_${System.currentTimeMillis()}.csv"

        AlertDialog.Builder(this)
            .setTitle("Exportar concluídas")
            .setItems(arrayOf("Salvar arquivo...", "Compartilhar...")) { _, which ->
                when (which) {
                    0 -> createDocumentLauncher.launch(pendingCsvSuggestedName)
                    1 -> shareCompletedCsv()
                }
            }
            .show()
    }

    private fun buildCompletedCsv(tasks: List<Task>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val header = "title,description,dueDate,categoryId\n"
        val rows = tasks.joinToString("\n") { task ->
            val title = task.title.replace('"', '\'').replace(',', ' ')
            val desc = (task.description ?: "").replace('"', '\'').replace(',', ' ')
            val due = task.dueDate?.let { sdf.format(Date(it)) } ?: ""
            val category = task.categoryId?.toString() ?: ""
            "\"$title\",\"$desc\",\"$due\",\"$category\""
        }
        return header + rows
    }

    private fun writeCsvToUri(uri: Uri) {
        val data = pendingCsvData ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(data.toByteArray())
            }
            Toast.makeText(this, "Arquivo salvo", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Falha ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareCompletedCsv() {
        val data = pendingCsvData ?: return
        try {
            val file = File(cacheDir, pendingCsvSuggestedName)
            FileWriter(file).use { it.write(data) }
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartilhar CSV"))
        } catch (e: Exception) {
            Toast.makeText(this, "Falha ao compartilhar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showCreateListDialog() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Criar nova lista")
            .setView(editText)
            .setPositiveButton("Criar") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    taskViewModel.insert(Category(name = name))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        (menu.findItem(R.id.action_search).actionView as SearchView).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                taskViewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
        return true
    }
}
