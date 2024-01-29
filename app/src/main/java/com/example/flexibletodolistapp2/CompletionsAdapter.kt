package com.example.flexibletodolistapp2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class CompletionsAdapter(private val viewModel: TaskViewModel) :
    ListAdapter<CompletionDate, CompletionsAdapter.CompletionsViewHolder>(CompletionDiffCallback()) {

    class CompletionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recurrenceDateTextView: TextView =
            itemView.findViewById(R.id.recurrenceDateTextView)
        private val recurrenceTimelinessTextView: TextView =
            itemView.findViewById(R.id.recurrenceTimelinessTextView)

        fun bind(currentCompletion: CompletionDate, viewModel: TaskViewModel) {
            val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(
                // TODO: use correct locale
                Locale.US
            )
            recurrenceDateTextView.text = currentCompletion.date.format(dateFormatter)

            // TODO: add "timeliness" of this completion
            // TODO: also consider logging the recurrence at the time of this completion
            recurrenceTimelinessTextView.text = currentCompletion.id.toString()

            itemView.setOnClickListener {
                // TODO: Add delete dialog
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletionsViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_completion, parent, false)
        return CompletionsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompletionsViewHolder, position: Int) {
        holder.bind(getItem(position), viewModel)
    }
}
