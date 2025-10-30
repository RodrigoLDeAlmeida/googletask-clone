package com.rodrigo.googletask_clone.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rodrigo.googletask_clone.R
import com.rodrigo.googletask_clone.data.Task
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class TaskListAdapter(
    private val onTaskCheckedChanged: (Task, Boolean) -> Unit,
    private val onTaskClicked: (Task) -> Unit,
    private val onHeaderClicked: () -> Unit,
    private val onExpandClicked: (Task) -> Unit
) : ListAdapter<TaskListItem, RecyclerView.ViewHolder>(TaskListItemDiffCallback()) {

    var isCompletedSectionExpanded: Boolean = false
    var expandedTaskIds = setOf<Int>()

    override fun getItemViewType(position: Int): Int {
        return when(val item = getItem(position)) {
            is TaskListItem.HeaderItem -> VIEW_TYPE_COMPLETED_HEADER
            is TaskListItem.TaskItem -> {
                if (item.task.isCompleted) VIEW_TYPE_COMPLETED_TASK
                else if (item.task.parentId == null) VIEW_TYPE_PENDING_PARENT
                else VIEW_TYPE_PENDING_SUBTASK
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PENDING_PARENT -> ParentTaskViewHolder(inflater.inflate(R.layout.task_item, parent, false))
            VIEW_TYPE_PENDING_SUBTASK -> SubtaskViewHolder(inflater.inflate(R.layout.subtask_item, parent, false))
            VIEW_TYPE_COMPLETED_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.completed_header_item, parent, false))
            VIEW_TYPE_COMPLETED_TASK -> CompletedTaskViewHolder(inflater.inflate(R.layout.task_item, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TaskListItem.HeaderItem -> (holder as HeaderViewHolder).bind(item.completedCount, isCompletedSectionExpanded, onHeaderClicked)
            is TaskListItem.TaskItem -> when (holder) {
                is ParentTaskViewHolder -> holder.bind(item, expandedTaskIds.contains(item.task.id), onTaskCheckedChanged, onTaskClicked, onExpandClicked)
                is SubtaskViewHolder -> holder.bind(item.task, onTaskCheckedChanged, onTaskClicked)
                is CompletedTaskViewHolder -> holder.bind(item.task, onTaskCheckedChanged, onTaskClicked)
            }
        }
    }

    class ParentTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.task_title)
        private val checkbox: CheckBox = itemView.findViewById(R.id.task_checkbox)
        private val dueDate: TextView = itemView.findViewById(R.id.task_due_date)
        private val subtaskCount: TextView = itemView.findViewById(R.id.subtask_count)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expand_icon)

        fun bind(item: TaskListItem.TaskItem, isExpanded: Boolean, onCheck: (Task, Boolean) -> Unit, onClick: (Task) -> Unit, onExpand: (Task) -> Unit) {
            val task = item.task
            title.text = task.title

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = task.isCompleted
            checkbox.setOnCheckedChangeListener { _, isChecked -> onCheck(task, isChecked) }

            itemView.setOnClickListener { onClick(task) }

            dueDate.text = task.dueDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) }
            dueDate.visibility = if (task.dueDate != null) View.VISIBLE else View.GONE

            if (item.subtaskCount > 0) {
                subtaskCount.text = "${item.subtaskCount} subtarefas"
                subtaskCount.visibility = if (isExpanded) View.GONE else View.VISIBLE
                expandIcon.visibility = View.VISIBLE
                expandIcon.rotation = if (isExpanded) 180f else 0f
                expandIcon.setOnClickListener { onExpand(task) }
            } else {
                subtaskCount.visibility = View.GONE
                expandIcon.visibility = View.GONE
                expandIcon.setOnClickListener(null)
            }
        }
    }

    class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.task_title)
        private val checkbox: CheckBox = itemView.findViewById(R.id.task_checkbox)
        private val dueDate: TextView = itemView.findViewById(R.id.task_due_date)

        fun bind(task: Task, onCheck: (Task, Boolean) -> Unit, onClick: (Task) -> Unit) {
            title.text = task.title
            
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = task.isCompleted
            checkbox.setOnCheckedChangeListener { _, isChecked -> onCheck(task, isChecked) }

            itemView.setOnClickListener { onClick(task) }
            dueDate.text = task.dueDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) }
            dueDate.visibility = if (task.dueDate != null) View.VISIBLE else View.GONE
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.header_title)
        private val icon: ImageView = itemView.findViewById(R.id.header_expand_icon)

        fun bind(count: Int, isExpanded: Boolean, onHeaderClicked: () -> Unit) {
            title.text = "Concluídas ($count)"
            icon.rotation = if (isExpanded) 180f else 0f
            itemView.setOnClickListener { onHeaderClicked() }
        }
    }

    class CompletedTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.task_title)
        private val checkbox: CheckBox = itemView.findViewById(R.id.task_checkbox)
        private val dueDate: TextView = itemView.findViewById(R.id.task_due_date)

        fun bind(task: Task, onCheck: (Task, Boolean) -> Unit, onClick: (Task) -> Unit) {
            title.text = task.title

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = task.isCompleted
            checkbox.setOnCheckedChangeListener { _, isChecked -> onCheck(task, isChecked) }

            itemView.findViewById<View>(R.id.subtask_count).visibility = View.GONE
            itemView.findViewById<View>(R.id.expand_icon).visibility = View.GONE
            itemView.setOnClickListener { onClick(task) }

            dueDate.text = task.dueDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) }
            dueDate.visibility = if (task.dueDate != null) View.VISIBLE else View.GONE
        }
    }

    private class TaskListItemDiffCallback : DiffUtil.ItemCallback<TaskListItem>() {
        override fun areItemsTheSame(old: TaskListItem, new: TaskListItem) = old.id == new.id
        override fun areContentsTheSame(old: TaskListItem, new: TaskListItem) = old == new
    }

    companion object {
        private const val VIEW_TYPE_PENDING_PARENT = 1
        private const val VIEW_TYPE_PENDING_SUBTASK = 2
        private const val VIEW_TYPE_COMPLETED_HEADER = 3
        private const val VIEW_TYPE_COMPLETED_TASK = 4
    }
}
