package org.xdty.callerinfo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

public class ScheduleService extends Service {

    private static final String TAG = ScheduleService.class.getSimpleName();

    private Handler mThreadHandler;
    private Handler mMainHandler;

    public ScheduleService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mThreadHandler = new Handler(handlerThread.getLooper());
        mMainHandler = new Handler(getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                runScheduledJobs();
            }
        });
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runScheduledJobs() {
        // 1. upload marked number
        // 2. download offline marked number data
        // 3. check app update
    }
}
