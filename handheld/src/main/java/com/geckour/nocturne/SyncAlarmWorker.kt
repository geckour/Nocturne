package com.geckour.nocturne

import android.app.AlarmManager
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class SyncAlarmWorker(context: Context, parameters: WorkerParameters) : Worker(context.applicationContext, parameters) {

    companion object {
        const val WEAR_PATH_ALARM_TIME = "/path_alarm_time"
        private const val WEAR_KEY_ALARM_TIME = "value"
    }

    override fun doWork(): Result {
        val alarmInfo = try {
            applicationContext.getSystemService(AlarmManager::class.java).nextAlarmClock
        } catch (t: Throwable) {
            null
        } ?: return Result.retry()

        pushAlarmTime(alarmInfo.triggerTime)

        return Result.success()
    }

    private fun pushAlarmTime(time: Long) {
        Wearable.getDataClient(applicationContext).putDataItem(
                PutDataMapRequest.create(WEAR_PATH_ALARM_TIME).apply {
                    dataMap.apply {
                        putLong(WEAR_KEY_ALARM_TIME, time)
                    }
                }.asPutDataRequest()
        )
    }
}