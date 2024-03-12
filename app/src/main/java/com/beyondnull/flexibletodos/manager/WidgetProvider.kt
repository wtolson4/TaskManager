package com.beyondnull.flexibletodos.manager

// TODO(P1): remove Log calls
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.beyondnull.flexibletodos.MainApplication
import com.beyondnull.flexibletodos.R
import com.beyondnull.flexibletodos.activity.MainActivity
import com.beyondnull.flexibletodos.calculation.UrgencyColorMapping
import com.beyondnull.flexibletodos.data.AppDatabase
import com.beyondnull.flexibletodos.data.Task
import com.beyondnull.flexibletodos.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        MainApplication.applicationScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).taskDao()
            val repository = TaskRepository(dao, MainApplication.applicationScope)
            val tasks = repository.allTasks.first()
            // Perform this loop procedure for each widget that belongs to this
            // provider.
            appWidgetIds.forEach { appWidgetId ->
                // Instantiate the RemoteViews object for the widget layout.
                val views = constructRemoteViews(context, tasks)
                // Tell the AppWidgetManager to perform an update on the current
                // widget.
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    companion object {

        private const val REQUEST_CODE_OPEN_ACTIVITY = 1

        fun constructRemoteViews(
            context: Context,
            tasks: List<Task>
        ): RemoteViews {
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val appOpenIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_ACTIVITY,
                activityIntent,
                // API level 31 requires specifying either of
                // PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_MUTABLE
                // See https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val remoteViews = RemoteViews(context.packageName, R.layout.widget_view).apply {
                // Use simplified RemoteViews collections (requires API 31)
                // https://developer.android.com/about/versions/12/features/widgets#leverage-simplified-remoteview-collections
                val builder = RemoteViews.RemoteCollectionItems.Builder()
                tasks.sortedBy { it.daysUntilDue }.forEachIndexed { index, task ->
                    val listItemRV = RemoteViews(context.packageName, R.layout.widget_list_item)
                    listItemRV.setTextViewText(R.id.taskNameTextView, task.name)
                    listItemRV.setTextViewText(R.id.dueDateTextView, task.getDueDaysString(context))
                    listItemRV.setTextColor(
                        R.id.dueDateTextView, UrgencyColorMapping.get(
                            context,
                            task.period,
                            task.daysUntilDue,
                            UrgencyColorMapping.ColorRange.STANDARD
                        )
                    )
                    builder.addItem(index.toLong(), listItemRV)
                }
                val collectionItems =
                    builder.setHasStableIds(true).setViewTypeCount(tasks.count()).build()
                setRemoteAdapter(R.id.list_view, collectionItems)

                // The empty view is displayed when the collection has no items.
                // It must be in the same layout used to instantiate the
                // RemoteViews object.
                setEmptyView(R.id.list_view, R.id.empty_view)

                // TODO: (P1) not sure why this doesn't work
                // setOnClickPendingIntent(R.id.list_view, appOpenIntent)
            }

            return remoteViews
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
