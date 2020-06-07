package org.xdty.callerinfo.presenter

import android.annotation.SuppressLint
import org.xdty.callerinfo.application.Application
import org.xdty.callerinfo.contract.MainContract.Presenter
import org.xdty.callerinfo.contract.MainContract.View
import org.xdty.callerinfo.data.CallerDataSource
import org.xdty.callerinfo.data.CallerDataSource.OnDataUpdateListener
import org.xdty.callerinfo.model.database.Database
import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.InCall
import org.xdty.callerinfo.model.permission.Permission
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.utils.Alarm
import org.xdty.callerinfo.utils.Contact
import org.xdty.phone.number.RxPhoneNumber
import org.xdty.phone.number.model.caller.Status
import javax.inject.Inject

@SuppressLint("CheckResult")
class MainPresenter(private val mView: View) : Presenter, OnDataUpdateListener {
    private val mInCallList: MutableList<InCall> = ArrayList()
    @Inject
    internal lateinit var mSetting: Setting
    @Inject
    internal lateinit var mPermission: Permission
    @Inject
    internal lateinit var mPhoneNumber: RxPhoneNumber
    @Inject
    internal lateinit var mDatabase: Database
    @Inject
    internal lateinit var mAlarm: Alarm
    @Inject
    internal lateinit var mContact: Contact
    @Inject
    internal lateinit var mCallerDataSource: CallerDataSource

    private var isInvalidateDataUpdate = false
    private var isWaitDataUpdate = false

    init {
        Application.appComponent.inject(this)
    }

    override fun result(requestCode: Int, resultCode: Int) {}
    override fun loadInCallList() {
        mDatabase.fetchInCalls().subscribe { inCalls ->
            mInCallList.clear()
            mInCallList.addAll(inCalls)
            mView.showCallLogs(mInCallList)
            if (mInCallList.size == 0) {
                mView.showNoCallLog(true)
            } else {
                mView.showNoCallLog(false)
            }
            mView.showLoading(false)
        }
    }

    override fun loadCallerMap() {
        mCallerDataSource.loadCallerMap().subscribe { callerMap -> mView.attachCallerMap(callerMap) }
    }

    override fun removeInCallFromList(inCall: InCall) {
        mInCallList.remove(inCall)
    }

    override fun removeInCall(inCall: InCall) {
        mDatabase.removeInCall(inCall)
    }

    override fun clearAll() {
        mDatabase.clearAllInCalls().subscribe { loadInCallList() }
    }

    override fun search(number: String) {
        if (number.isEmpty()) {
            return
        }
        mView.showSearching()
        mCallerDataSource.getCaller(number).subscribe { caller ->
            if (caller.number != null) {
                mView.showSearchResult(caller)
            } else {
                mView.showSearchFailed(!caller.isOffline)
            }
        }
    }

    override fun checkEula() {
        if (!mSetting.isEulaSet) {
            mView.showEula()
        }
    }

    override fun setEula() {
        mSetting.setEula()
    }

    override fun canDrawOverlays(): Boolean {
        return mPermission.canDrawOverlays()
    }

    override fun checkPermission(permission: String): Int {
        return mPermission.checkPermission(permission)
    }

    override fun start() {
        mCallerDataSource.setOnDataUpdateListener(this)
        loadCallerMap()
        //        if (System.currentTimeMillis() - mSetting.lastCheckDataUpdateTime() > 6 * 3600 * 1000) {
//            mPhoneNumber.checkUpdate().subscribe(new Consumer<Status>() {
//                @Override
//                public void accept(Status status) throws Exception {
//                    if (status != null && status.count > 0) {
//                        mView.notifyUpdateData(status);
//                        mSetting.setStatus(status);
//                    }
//                }
//            });
//        }
    }

    override fun clearSearch() {}
    override fun dispatchUpdate(status: Status) {
        mView.showUpdateData(status)
        mPhoneNumber.upgradeData().subscribe { result ->
            mView.updateDataFinished(result)
            if (result) {
                mSetting.updateLastCheckDataUpdateTime(System.currentTimeMillis())
            }
        }
    }

    override fun getCaller(number: String): Caller {
        return mCallerDataSource.getCallerFromCache(number)
    }

    override fun clearCache() {
        mCallerDataSource.clearCache().subscribe { loadInCallList() }
    }

    override fun itemOnLongClicked(inCall: InCall) {
        mView.showBottomSheet(inCall)
    }

    override fun invalidateDataUpdate(isInvalidate: Boolean) {
        isInvalidateDataUpdate = isInvalidate
        if (isWaitDataUpdate) {
            mView.showCallLogs(mInCallList)
            isWaitDataUpdate = false
        }
    }

    override fun onDataUpdate(caller: Caller) {
        isWaitDataUpdate = isInvalidateDataUpdate
        if (!isWaitDataUpdate) {
            mView.showCallLogs(mInCallList)
        }
    }
}