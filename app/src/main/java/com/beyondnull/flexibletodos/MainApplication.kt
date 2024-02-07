package com.beyondnull.flexibletodos

import android.app.Application
import android.util.Log
import com.beyondnull.flexibletodos.manager.AlarmManager
import com.beyondnull.flexibletodos.manager.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the Timber logging lib
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            val file = File(baseContext.filesDir, "logfile")
            Timber.plant(FileLoggingTree(file))
        }
        Timber.d("Starting main application")

        // Trigger notifications and alarms based on task state
        NotificationManager.watchTasksAndUpdateNotifications(
            this,
            applicationScope
        )

        AlarmManager(this, applicationScope).watchTasksAndSetAlarm()
    }

    companion object {
        // https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
        // https://developer.android.com/topic/architecture/data-layer#make_an_operation_live_longer_than_the_screen
        // This might be equivalent to `kotlinx.coroutines.MainScope()`?
        // No need to cancel this scope as it'll be torn down with the process
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

// From https://gist.github.com/cp-hardik-p/a3d27064ad3602ca2263007269f80145
class FileLoggingTree(private val logFile: File) : Timber.DebugTree() {

    private val maxLogSize = 5 * 1024 * 1024 // 5 MB

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS", Locale.US)

    private fun getPriorityString(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> ""
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.DEBUG) {
            val log = generateLog(priority, tag, message)
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            writeLog(logFile, log)
            ensureLogSize(logFile)
        }
    }

    private fun generateLog(priority: Int, tag: String?, message: String): String {
        val logTimeStamp = dateFormat.format(Date())

        return StringBuilder().append(logTimeStamp).append(" ")
            .append(getPriorityString(priority)).append(": ")
            .append(tag).append(" - ")
            .append(message).append('\n').toString()
    }

    private fun writeLog(logFile: File, log: String) {
        val writer = FileWriter(logFile, true)
        writer.append(log)
        writer.flush()
        writer.close()
    }

    @Throws(IOException::class)
    private fun ensureLogSize(logFile: File) {
        if (logFile.length() < maxLogSize) return

        // We remove first 25% part of logs
        val startIndex = logFile.length() / 4

        val randomAccessFile = RandomAccessFile(logFile, "r")
        randomAccessFile.seek(startIndex)

        val into = ByteArrayOutputStream()

        val buf = ByteArray(4096)
        var n: Int
        while (true) {
            n = randomAccessFile.read(buf)
            if (n < 0) break
            into.write(buf, 0, n)
        }

        randomAccessFile.close()

        val outputStream = FileOutputStream(logFile)
        into.writeTo(outputStream)

        outputStream.close()
        into.close()
    }
}