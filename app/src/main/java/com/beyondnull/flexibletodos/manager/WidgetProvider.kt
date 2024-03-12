package com.beyondnull.flexibletodos.manager

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
// TODO(P1): remove Log calls
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.Task
import com.beyondnull.flexibletodos.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class WidgetProvider : AppWidgetProvider() {
    @RequiresApi(31)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.e("myWidget", "at top of OnUpdate")
        GlobalScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).taskDao()
            val repository = TaskRepository(dao, GlobalScope)
            val tasks = repository.allTasks.first()
            // Perform this loop procedure for each widget that belongs to this
            // provider.
            Log.e("myWidget", "got Tasks")

            appWidgetIds.forEach { appWidgetId ->
                // Instantiate the RemoteViews object for the widget layout.
                val views = constructRemoteViews(context, tasks)
                // Tell the AppWidgetManager to perform an update on the current
                // widget.
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            super.onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        private fun constructRemoteViews(
            context: Context,
            tasks: List<Task>
        ): RemoteViews {
            // Instantiate the RemoteViews object for the widget layout.
            val views = RemoteViews(context.packageName, R.layout.widget_view).apply {
                // Use simplified RemoteViews collections (requires API 31)
                // https://developer.android.com/about/versions/12/features/widgets#leverage-simplified-remoteview-collections
                val builder = RemoteViews.RemoteCollectionItems.Builder()
                tasks.forEachIndexed { index, task ->
                    val listItemRV = RemoteViews(context.packageName, R.layout.widget_list_item)
                    listItemRV.setTextViewText(R.id.taskNameTextView, "foo")
                    builder.addItem(index.toLong(), listItemRV)
                }
                val collectionItems =
                    builder.setHasStableIds(true).setViewTypeCount(tasks.count()).build()
                setRemoteAdapter(R.id.list_view, collectionItems)

                // The empty view is displayed when the collection has no items.
                // It must be in the same layout used to instantiate the
                // RemoteViews object.
                setEmptyView(R.id.list_view, R.id.empty_view)
            }
            return views
        }

        fun watchTasksAndUpdateWidget(
            context: Context,
            externalScope: CoroutineScope
        ) {
            val dao = AppDatabase.getDatabase(context).taskDao()
            val repository = TaskRepository(dao, externalScope)
            externalScope.launch {
                Timber.d("Starting to watch for task changes to update widget")
                repository.allTasks.collect { tasks ->
                    Timber.d("Got updated tasks")

                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = constructRemoteViews(context, tasks)
                    appWidgetManager.updateAppWidget(
                        ComponentName(
                            context.packageName,
                            WidgetProvider::class.java.name
                        ), views
                    )
                }
            }
        }
    }
}
