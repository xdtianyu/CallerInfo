package org.xdty.callerinfo.application;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.service.ScheduleService;
import org.xdty.callerinfo.utils.Utils;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class Application extends com.orm.SugarApp {
    public final static String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        SettingImpl.init(this);
        Utils.checkLocale(getApplicationContext());
        Setting setting = SettingImpl.getInstance();

        if (setting.isCatchCrash() || BuildConfig.DEBUG) {
            CustomActivityOnCrash.install(this);
        }

        if (setting.isAutoReportEnabled() || setting.isMarkingEnabled()) {
            Intent intent = new Intent(this, ScheduleService.class);
            PendingIntent pIntent = PendingIntent.getService(this, 0, intent, 0);
            AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarm.cancel(pIntent);
            long now = System.currentTimeMillis();
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, now, 60 * 60 * 1000, pIntent);
            JobScheduler s;
        }
    }
}
