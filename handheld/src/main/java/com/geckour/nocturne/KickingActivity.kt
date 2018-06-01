package com.geckour.nocturne

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.*
import java.util.concurrent.TimeUnit

class KickingActivity : Activity() {

    companion object {
        private var workerId: UUID? = null

        fun getIntent(context: Context): Intent =
                Intent(context, KickingActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val syncAlarmWork =
                PeriodicWorkRequest.Builder(
                        SyncAlarmWorker::class.java,
                        1L,
                        TimeUnit.MINUTES).build().apply { workerId = id }

        WorkManager.getInstance().apply {
            workerId?.also { this.cancelWorkById(it) }
            enqueue(syncAlarmWork)
        }

        finish()
    }
}