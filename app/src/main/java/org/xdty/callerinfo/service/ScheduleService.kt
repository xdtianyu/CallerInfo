package org.xdty.callerinfo.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import org.xdty.callerinfo.application.Application.Companion.appComponent
import org.xdty.callerinfo.model.database.Database
import org.xdty.callerinfo.model.db.MarkedRecord
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.phone.number.RxPhoneNumber
import org.xdty.phone.number.model.cloud.CloudNumber
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class ScheduleService : Service() {
    @Inject
    internal lateinit var mDatabase: Database
    @Inject
    internal lateinit var mSetting: Setting
    @Inject
    internal lateinit var mPhoneNumber: RxPhoneNumber

    private var mThreadHandler: Handler? = null
    private var mMainHandler: Handler? = null
    private var mPutList: MutableList<String?>? = null
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mThreadHandler = Handler(handlerThread.looper)
        mMainHandler = Handler(mainLooper)
        mPutList = Collections.synchronizedList(ArrayList())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        mThreadHandler!!.post { runScheduledJobs() }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mThreadHandler!!.removeCallbacksAndMessages(null)
        mThreadHandler!!.looper.quit()
        super.onDestroy()
    }

    // run in background thread
    @SuppressLint("CheckResult")
    private fun runScheduledJobs() { // 1. upload marked number
        val records: List<MarkedRecord?> = mDatabase.fetchMarkedRecordsSync()
        val isAutoReport = mSetting.isAutoReportEnabled
        for (record in records) {
            if (!record!!.isReported) {
                if (!isAutoReport && record.source != MarkedRecord.API_ID_USER_MARKED) {
                    continue
                }
                if (!TextUtils.isEmpty(record.typeName)) {
                    mPutList!!.add(record.number)
                    // this put operation is asynchronous
                    mPhoneNumber.put(record.toNumber()).subscribe { aBoolean -> onPutResult(record.toNumber(), aBoolean!!) }
                } else {
                    mDatabase.removeRecord(record)
                }
            }
        }
        // 2. check offline marked number data
        // 3. check app update
        // update last schedule time
        mSetting.updateLastScheduleTime()
        checkStopSelf()
    }

    fun onPutResult(number: CloudNumber?, result: Boolean) {
        Log.e(TAG, "onPutResult: " + number!!.number + ", result: " + result)
        if (result) {
            mDatabase.updateMarkedRecord(number.number)
        } else {
            mSetting.updateLastScheduleTime(0)
        }
        mPutList!!.remove(number.number)
        checkStopSelf()
    }

    private fun checkStopSelf() {
        if (mPutList!!.size == 0) {
            stopSelf()
        }
    }

    companion object {
        private val TAG: String? = ScheduleService::class.java.simpleName
    }

    init {
        appComponent.inject(this)
    }
}