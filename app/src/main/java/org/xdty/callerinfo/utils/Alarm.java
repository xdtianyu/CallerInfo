package org.xdty.callerinfo.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.service.ScheduleService;
import org.xdty.callerinfo.worker.UpgradeWorker;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public final class Alarm {

    private static final String TAG = Alarm.class.getSimpleName();

    @Inject
    Setting mSetting;

    @Inject
    Application mApplication;

    public Alarm() {
        Application.getApplication().getAppComponent().inject(this);
    }

    public void alarm() {
        Log.v(TAG, "alarm");
        if (!mSetting.isAutoReportEnabled() && !mSetting.isMarkingEnabled()) {
            Log.v(TAG, "alarm is not installed");
            return;
        }

        Intent intent = new Intent(mApplication, ScheduleService.class);
        PendingIntent pIntent = PendingIntent.getService(mApplication, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) mApplication.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
        long now = System.currentTimeMillis();
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, now + 5 * 1000, 60 * 60 * 1000, pIntent);
    }

    public void enqueueWorkers() {
        PeriodicWorkRequest.Builder builder =
                new PeriodicWorkRequest.Builder(UpgradeWorker.class, 6, TimeUnit.HOURS);
        Constraints constraints = new Constraints.Builder()
//                .setRequiredNetworkType(NetworkType.CONNECTED)
//                .setRequiresBatteryNotLow(true)
//                .setRequiresStorageNotLow(true)
                .build();
        builder.setConstraints(constraints);
        PeriodicWorkRequest request = builder.build();

        WorkManager.getInstance().enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request);
    }
}
