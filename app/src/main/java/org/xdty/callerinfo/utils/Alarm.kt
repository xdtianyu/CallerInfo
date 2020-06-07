package org.xdty.callerinfo.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import androidx.work.PeriodicWorkRequest.Builder
import org.xdty.callerinfo.application.Application
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.service.ScheduleService
import org.xdty.callerinfo.worker.UpgradeWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class Alarm {

    @Inject
    internal lateinit var setting: Setting
    @Inject
    internal lateinit var application: Application

    init {
        Application.appComponent.inject(this)
    }

    fun alarm() {
        Log.v(TAG, "alarm")
        if (!setting.isAutoReportEnabled && !setting.isMarkingEnabled) {
            Log.v(TAG, "alarm is not installed")
            return
        }
        val intent = Intent(application, ScheduleService::class.java)
        val pIntent = PendingIntent.getService(application, 0, intent, 0)
        val alarm = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pIntent)
        val now = System.currentTimeMillis()
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, now + 5 * 1000, 60 * 60 * 1000.toLong(), pIntent)
    }

    fun enqueueUpgradeWork() {
        if (!setting.isOfflineDataAutoUpgrade) {
            Log.d(TAG, "Offline data auto upgrade is not enabled.")
            return
        }
        val builder = Builder(UpgradeWorker::class.java, 6, TimeUnit.HOURS)
        val constraints: Constraints = Constraints.Builder()
//                .setRequiredNetworkType(NetworkType.CONNECTED)
//                .setRequiresBatteryNotLow(true)
//                .setRequiresStorageNotLow(true)
                .build()
        builder.setConstraints(constraints)
        val request = builder.build()
        WorkManager.getInstance().enqueueUniquePeriodicWork(UpgradeWorker::class.java.simpleName,
                ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelUpgradeWork() {
        WorkManager.getInstance().cancelUniqueWork(UpgradeWorker::class.java.simpleName)
    }

    fun runUpgradeWorkOnce(): LiveData<WorkInfo> {
        val request: OneTimeWorkRequest = OneTimeWorkRequest.Builder(UpgradeWorker::class.java).build()
        val workManager = WorkManager.getInstance()
        workManager.enqueue(request)
        return workManager.getWorkInfoByIdLiveData(request.id)
    }

    companion object {
        private val TAG = Alarm::class.java.simpleName
    }

}