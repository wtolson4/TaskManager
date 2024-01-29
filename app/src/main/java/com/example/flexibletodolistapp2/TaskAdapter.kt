package com.example.flexibletodolistapp2

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class TaskAdapter(private val viewModel: TaskViewModel) :
    ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNameTextView: TextView = itemView.findViewById(R.id.taskNameTextView)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.dueDateTextView)
        private val recurrenceTypeTextView: TextView =
            itemView.findViewById(R.id.recurrenceTypeTextView)
        private val taskCompletionCheckBox: CheckBox =
            itemView.findViewById(R.id.taskCompletionCheckBox)

        fun bind(currentTask: Task, viewModel: TaskViewModel) {
            taskNameTextView.text = currentTask.definition.name
            val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(
                // TODO: use correct locale
                Locale.US
            )
            dueDateTextView.text = currentTask.nextDueDate.format(dateFormatter)
            recurrenceTypeTextView.text = "temp val 1"

            taskCompletionCheckBox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.insertCompletion(currentTask, LocalDate.now())
            }

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, EditTaskActivity::class.java)
                intent.putExtra("taskId", currentTask.definition.id)
                itemView.context.startActivity(intent)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), viewModel)
    }
}
