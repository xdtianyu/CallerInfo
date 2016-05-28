package org.xdty.callerinfo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.database.DatabaseImpl;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.cloud.CloudNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScheduleService extends Service implements PhoneNumber.CloudListener {

    private static final String TAG = ScheduleService.class.getSimpleName();

    private Handler mThreadHandler;
    private Handler mMainHandler;

    private Database mDatabase;
    private Setting mSetting;
    private PhoneNumber mPhoneNumber;
    private List<String> mPutList;

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
        mDatabase = DatabaseImpl.getInstance();
        mSetting = SettingImpl.getInstance();
        mPutList = Collections.synchronizedList(new ArrayList<String>());
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mPhoneNumber = PhoneNumber.getInstance();
                mPhoneNumber.addCloudListener(ScheduleService.this);
            }
        });
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
        super.onDestroy();
    }

    // run in background thread
    private void runScheduledJobs() {

        // 1. upload marked number
        List<MarkedRecord> records = MarkedRecord.listAll(MarkedRecord.class);
        boolean isAutoReport = mSetting.isAutoReportEnabled();

        for (MarkedRecord record : records) {
            if (!record.isReported()) {
                if (!isAutoReport && record.getSource() != MarkedRecord.API_ID_USER_MARKED) {
                    continue;
                }
                mPutList.add(record.getNumber());
                // this put operation is asynchronous
                mPhoneNumber.put(record.toNumber());
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
