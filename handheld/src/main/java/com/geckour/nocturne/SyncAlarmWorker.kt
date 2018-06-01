package com.geckour.nocturne

import android.app.AlarmManager
import android.net.Uri
import androidx.work.Worker
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.experimental.async
import timber.log.Timber

class SyncAlarmWorker : Worker() {

    companion object {
        private const val WEAR_PATH_ALARM_TIME = "/path_alarm_time"
        private const val WEAR_KEY_ALARM_TIME = "value"
    }

    override fun doWork(): WorkerResult {
        val alarmInfo = try {
            applicationContext.getSystemService(AlarmManager::class.java).nextAlarmClock
        } catch (t: Throwable) {
            Timber.e(t)
            null
        } ?: return WorkerResult.FAILURE

        async {
            val onComplete = {
                Wearable.getDataClient(applicationContext).putDataItem(
                        PutDataMapRequest.create(WEAR_PATH_ALARM_TIME).apply {
                            dataMap.apply {
                                putLong(WEAR_KEY_ALARM_TIME, alarmInfo.triggerTime)
                            }
                        }.asPutDataRequest()
                )
            }

            Wearable.getDataClient(applicationContext).deleteDataItems(Uri.parse("wear://$WEAR_PATH_ALARM_TIME"))
                    .addOnCompleteListener { onComplete() }
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener { onComplete() }
        }

        return WorkerResult.SUCCESS
    }
}