package com.geckour.nocturne

import android.app.AlarmManager
import android.net.Uri
import androidx.work.Worker
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.experimental.async

class SyncAlarmWorker : Worker() {

    companion object {
        const val WEAR_PATH_ALARM_TIME = "/path_alarm_time"
        private const val WEAR_KEY_ALARM_TIME = "value"
    }

    override fun doWork(): WorkerResult {
        val alarmInfo = try {
            applicationContext.getSystemService(AlarmManager::class.java).nextAlarmClock
        } catch (t: Throwable) {
            null
        } ?: return WorkerResult.FAILURE

        pushAlarmTime(alarmInfo.triggerTime)

        return WorkerResult.SUCCESS
    }

    private fun pushAlarmTime(time: Long) {
        async {
            Wearable.getDataClient(applicationContext).putDataItem(
                    PutDataMapRequest.create(WEAR_PATH_ALARM_TIME).apply {
                        dataMap.apply {
                            putLong(WEAR_KEY_ALARM_TIME, time)
                        }
                    }.asPutDataRequest()
            )
        }
    }
}