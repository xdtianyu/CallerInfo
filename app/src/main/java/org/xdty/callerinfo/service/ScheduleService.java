package org.xdty.callerinfo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.cloud.CloudNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class ScheduleService extends Service implements PhoneNumber.CloudListener {

    private static final String TAG = ScheduleService.class.getSimpleName();

    @Inject
    Database mDatabase;

    @Inject
    Setting mSetting;

    @Inject
    PhoneNumber mPhoneNumber;

    private Handler mThreadHandler;
    private Handler mMainHandler;
    private List<String> mPutList;

    public ScheduleService() {
        Application.getAppComponent().inject(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mThreadHandler = new Handler(handlerThread.getLooper());
        mMainHandler = new Handler(getMainLooper());

        mPutList = Collections.synchronizedList(new ArrayList<String>());
        mPhoneNumber.addCloudListener(ScheduleService.this);
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

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        mPhoneNumber.removeCloudListener(ScheduleService.this);
        mThreadHandler.removeCallbacksAndMessages(null);
        mThreadHandler.getLooper().quit();

        super.onDestroy();
    }

    // run in background thread
    private void runScheduledJobs() {

        // 1. upload marked number
        List<MarkedRecord> records = mDatabase.fetchMarkedRecordsSync();
        boolean isAutoReport = mSetting.isAutoReportEnabled();

        for (MarkedRecord record : records) {
            if (!record.isReported()) {
                if (!isAutoReport && record.getSource() != MarkedRecord.API_ID_USER_MARKED) {
                    continue;
                }
                if (!TextUtils.isEmpty(record.getTypeName())) {
                    mPutList.add(record.getNumber());
                    // this put operation is asynchronous
                    mPhoneNumber.put(record.toNumber());
                } else {
                    mDatabase.removeRecord(record);
                }
            }
        }

        // 2. check offline marked number data

        // 3. check app update

        // update last schedule time
        mSetting.updateLastScheduleTime();

        checkStopSelf();
    }

    @Override
    public void onPutResult(CloudNumber number, boolean result) {
        Log.e(TAG, "onPutResult: " + number.getNumber() + ", result: " + result);
        if (result) {
            mDatabase.updateMarkedRecord(number.getNumber());
        } else {
            mSetting.updateLastScheduleTime(0);
        }
        mPutList.remove(number.getNumber());
        checkStopSelf();
    }

    private void checkStopSelf() {
        if (mPutList.size() == 0) {
            stopSelf();
        }
    }
}
