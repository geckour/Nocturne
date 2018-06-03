package com.geckour.nocturne

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.wearable.Wearable
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.coroutines.experimental.async
import java.util.*
import java.util.concurrent.TimeUnit

class SettingActivity : Activity() {

    companion object {
        private const val PREF_KEY_SWITCH_STATE_SYNC_ALARM = "pref_key_switch_state_sync_alarm"
        private var workerId: UUID? = null
    }

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_setting)

        setActionBar(toolbar)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        switch_sync_alarm.apply {
            setOnCheckedChangeListener { _, checked ->
                sharedPreferences.edit().putBoolean(PREF_KEY_SWITCH_STATE_SYNC_ALARM, checked).apply()
                summary_sync_alarm.text =
                        getString(if (checked) R.string.summary_on else R.string.summary_off)

                if (checked) setWork()
                else clearSync()
            }

            isChecked = getSwitchState(PREF_KEY_SWITCH_STATE_SYNC_ALARM)
            summary_sync_alarm.text =
                    getString(if (isChecked) R.string.summary_on else R.string.summary_off)
        }
    }

    private fun setWork() {
        val syncAlarmWork =
                PeriodicWorkRequest.Builder(
                        SyncAlarmWorker::class.java,
                        1L,
                        TimeUnit.MINUTES
                ).build().apply { workerId = id }

        WorkManager.getInstance().apply {
            cancelWork(this)
            enqueue(syncAlarmWork)
        }
    }

    private fun cancelWork(workManager: WorkManager = WorkManager.getInstance()) {
        workerId?.apply { workManager.cancelWorkById(this) }
    }

    private fun clearSync() {
        async {
            Wearable.getDataClient(applicationContext)
                    .deleteDataItems(Uri.parse("wear://${SyncAlarmWorker.WEAR_PATH_ALARM_TIME}"))
        }
        cancelWork()
    }

    private fun getSwitchState(key: String, default: Boolean = false): Boolean =
            if (sharedPreferences.contains(key))
                sharedPreferences.getBoolean(key, default)
            else default
}