package org.xdty.callerinfo.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.service.ScheduleService;

import javax.inject.Inject;

public final class Alarm {

    private static final String TAG = Alarm.class.getSimpleName();
    @Inject
    Setting mSetting;

    public Alarm() {
        Application.getAppComponent().inject(this);
    }

    public void alarm() {
        Log.v(TAG, "alarm");
        if (mSetting.isAutoReportEnabled() || mSetting.isMarkingEnabled()) {
            Log.v(TAG, "alarm is not installed");
            return;
        }
        Context context = Application.getApplication();
        Intent intent = new Intent(context, ScheduleService.class);
        PendingIntent pIntent = PendingIntent.getService(context, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
        long now = System.currentTimeMillis();
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, now + 5 * 1000, 60 * 60 * 1000, pIntent);
    }

}
