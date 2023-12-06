package com.example.flexibletodolistapp2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class TaskAdapter(private val viewModel: TaskViewModel) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNameTextView: TextView = itemView.findViewById(R.id.taskNameTextView)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.dueDateTextView)
        private val recurrenceTypeTextView: TextView = itemView.findViewById(R.id.recurrenceTypeTextView)
        private val taskCompletionCheckBox: CheckBox = itemView.findViewById(R.id.taskCompletionCheckBox)

        fun bind(currentTask: Task, viewModel: TaskViewModel) {
            val taskDefinition = currentTask.definition
            taskNameTextView.text = taskDefinition.taskName
            val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(
                Locale.US
            )
            dueDateTextView.text = taskDefinition.initialDueDate.format(DATE_FORMATTER)
            recurrenceTypeTextView.text = if (taskDefinition.frequency == 1) {
                taskDefinition.recurrenceType
            } else {
                "${taskDefinition.frequency} ${taskDefinition.recurrenceType}"
            }
            taskCompletionCheckBox.isChecked = taskDefinition.isCompleted

            taskCompletionCheckBox.setOnCheckedChangeListener { _, isChecked ->
                val updatedTask = taskDefinition.copy(isCompleted = isChecked)
                viewModel.update(updatedTask)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), viewModel)
    }
}
