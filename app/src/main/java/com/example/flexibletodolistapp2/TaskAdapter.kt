package com.example.flexibletodolistapp2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(private val viewModel: TaskViewModel) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNameTextView: TextView = itemView.findViewById(R.id.taskNameTextView)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.dueDateTextView)
        private val recurrenceTypeTextView: TextView = itemView.findViewById(R.id.recurrenceTypeTextView)
        private val taskCompletionCheckBox: CheckBox = itemView.findViewById(R.id.taskCompletionCheckBox)

        fun bind(currentTask: Task, viewModel: TaskViewModel) {
            taskNameTextView.text = currentTask.taskName
            dueDateTextView.text = currentTask.dueDate
            recurrenceTypeTextView.text = if (currentTask.frequency == 1) {
                currentTask.recurrenceType
            } else {
                "${currentTask.frequency} ${currentTask.recurrenceType}"
            }
            taskCompletionCheckBox.isChecked = currentTask.isCompleted

            taskCompletionCheckBox.setOnCheckedChangeListener { _, isChecked ->
                val updatedTask = currentTask.copy(isCompleted = isChecked)
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
