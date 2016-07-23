package org.xdty.callerinfo.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.xdty.callerinfo.service.ScheduleService;

public final class AlarmUtils {

    private static final String TAG = AlarmUtils.class.getSimpleName();

    private static Context sContext;

    private AlarmUtils() {
        throw new AssertionError("AlarmUtils class is not meant to be instantiated.");
    }

    public static void install(Context context) {
        sContext = context.getApplicationContext();
    }

    public static void alarm() {
        Log.v(TAG, "alarm");
        if (sContext == null) {
            Log.v(TAG, "alarm is not installed");
            return;
        }
        Intent intent = new Intent(sContext, ScheduleService.class);
        PendingIntent pIntent = PendingIntent.getService(sContext, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) sContext.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
        long now = System.currentTimeMillis();
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, now + 5 * 1000, 60 * 60 * 1000, pIntent);
    }

}
