package org.xdty.callerinfo.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.xdty.callerinfo.service.ScheduleService;

public class AlarmUtils {

    private static Context sContext;

    public static void install(Context context) {
        sContext = context.getApplicationContext();
    }

    public static void alarm() {
        // FIXME: cancel not working
        Intent intent = new Intent(sContext, ScheduleService.class);
        PendingIntent pIntent = PendingIntent.getService(sContext, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) sContext.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
        long now = System.currentTimeMillis();
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, now + 5 * 1000, 60 * 60 * 1000, pIntent);
    }

}
