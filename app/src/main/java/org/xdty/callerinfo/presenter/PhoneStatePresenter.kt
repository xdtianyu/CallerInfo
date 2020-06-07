package org.xdty.callerinfo.presenter

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import org.xdty.callerinfo.R
import org.xdty.callerinfo.application.Application.Companion.appComponent
import org.xdty.callerinfo.contract.PhoneStateContract.Presenter
import org.xdty.callerinfo.contract.PhoneStateContract.View
import org.xdty.callerinfo.data.CallerDataSource
import org.xdty.callerinfo.model.CallRecord
import org.xdty.callerinfo.model.SearchMode
import org.xdty.callerinfo.model.database.Database
import org.xdty.callerinfo.model.database.DatabaseImpl.Companion.instance
import org.xdty.callerinfo.model.db.InCall
import org.xdty.callerinfo.model.permission.Permission
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.plugin.IPluginService
import org.xdty.callerinfo.utils.Alarm
import org.xdty.callerinfo.utils.Contact
import org.xdty.phone.number.model.INumber
import javax.inject.Inject

class PhoneStatePresenter(private val mView: View) : Presenter {
    @Inject
    internal lateinit var mSetting: Setting
    @Inject
    internal lateinit var mPermission: Permission
    @Inject
    internal lateinit var mDatabase: Database
    @Inject
    internal lateinit var mAlarm: Alarm
    @Inject
    internal lateinit var mContact: Contact
    @Inject
    internal lateinit var mCallerDataSource: CallerDataSource

    internal var mCallRecord = CallRecord()
    private var mIncomingNumber: String? = null
    private var mPluginService: IPluginService? = null
    private var mPluginIntent: Intent? = null
    private var mConnection: PluginConnection? = null
    private var mAutoHangup = false
    private var mWaitingCheckHangup = false
    override fun matchIgnore(number: String): Boolean {
        var number = number
        if (!TextUtils.isEmpty(number)) {
            number = number.replace(" ".toRegex(), "")
            val ignoreRegex = mSetting.ignoreRegex.toRegex()
            return number.matches(ignoreRegex)
        }
        return false
    }

    override fun handleRinging(number: String) {
        mCallRecord.ring()
        if (!TextUtils.isEmpty(number)) {
            mIncomingNumber = number
            mCallRecord.logNumber = number
            searchNumber(number)
        }
    }

    override fun handleOffHook(number: String) {
        var number: String? = number
        if (System.currentTimeMillis() - mCallRecord.hook < 1000 &&
                mCallRecord.isEqual(number)) {
            Log.e(TAG, "duplicate hook, ignore.")
            return
        }
        mCallRecord.hook()
        if (mCallRecord.isIncoming) {
            if (mSetting.isHidingOffHook) {
                mView.hide(number!!)
            }
        } else { // outgoing call
            if (mSetting.isShowingOnOutgoing) {
                if (TextUtils.isEmpty(number)) {
                    Log.d(TAG, "number is null. " + TextUtils.isEmpty(mIncomingNumber))
                    number = mIncomingNumber
                    mCallRecord.logNumber = number
                    mIncomingNumber = null
                }
                searchNumber(number!!)
            }
        }
    }

    override fun handleIdle(number: String) {
        mCallRecord.idle()
        if (checkClose(number)) {
            return
        }
        var saveLog = mSetting.isAddingCallLog
        if (isIncoming(mIncomingNumber!!) && !mCallerDataSource.isIgnoreContact(mIncomingNumber!!)) {
            saveInCall()
            mIncomingNumber = null
            if (isRingOnce && mSetting.isAddingRingOnceCallLog) {
                if (mAutoHangup) { // ring once cased by auto hangup
                    mCallRecord.setLogName(
                            mView.context.getString(R.string.auto_hangup), saveLog)
                } else {
                    mCallRecord.setLogName(mView.context.getString(R.string.ring_once),
                            saveLog)
                }
                saveLog = true
            }
        }
        if (mView.isShowing) {
            if (mCallRecord.isValid) {
                if (mCallRecord.isNameValid) {
                    if (saveLog) {
                        updateCallLog(mCallRecord.logNumber, mCallRecord.logName)
                    }
                    if (mSetting.isAutoReportEnabled) {
                        reportFetchedNumber()
                    }
                } else {
                    if (mSetting.isMarkingEnabled && mCallRecord.isAnswered &&
                            !mCallerDataSource.isIgnoreContact(mCallRecord.logNumber!!) &&
                            !isNotMarkContact(mCallRecord.logNumber)) {
                        mView.showMark(mCallRecord.logNumber!!)
                    }
                }
            }
        }
        resetCallRecord()
        mAutoHangup = false
        mView.close(number)
        mSetting.setOutgoing(false)
        unBindPluginService()
    }

    override fun resetCallRecord() {
        mCallRecord.reset()
    }

    override fun checkClose(number: String): Boolean {
        return TextUtils.isEmpty(number) && mCallRecord.callDuration() == -1L
    }

    override fun isIncoming(number: String): Boolean {
        return mCallRecord.isIncoming && !TextUtils.isEmpty(mIncomingNumber)
    }

    override fun saveInCall() {
        mDatabase.saveInCall(
                InCall(mIncomingNumber, mCallRecord.time(), mCallRecord.ringDuration(),
                        mCallRecord.callDuration()))
    }

    override val isRingOnce: Boolean
        get() = mCallRecord.ringDuration() < 3000 && mCallRecord.callDuration() <= 0

    private fun isNotMarkContact(number: String?): Boolean {
        return (mSetting.isNotMarkContact && mPermission.canReadContact()
                && mContact.isExist(number))
    }

    private fun isTriggeredRepeatIncomingCall(number: String?): Boolean { // 0 -> twice, 1 -> third, ...
        return mDatabase.getInCallCount(number!!) >= mSetting.repeatedCountIndex + 1
    }

    @SuppressLint("CheckResult")
    override fun searchNumber(number: String) {
        if (TextUtils.isEmpty(number)) {
            Log.e(TAG, "searchNumber: number is null!")
            return
        }
        val mode = mCallerDataSource.getSearchMode(number)
        if (mode === SearchMode.IGNORE) {
            return
        }
        if (mSetting.isAutoHangup || mSetting.isAddingCallLog) {
            bindPluginService()
        }
        mView.showSearching()
        mCallerDataSource.getCaller(number, mode === SearchMode.OFFLINE)
                .subscribe { caller ->
                    Log.d(TAG, "call: " + number + "->" + caller.number +
                            ", offline: " + caller.isOffline)
                    if (!caller.isEmpty) {
                        showNumber(caller)
                    } else if (mCallRecord.isActive) {
                        mView.showFailed(!caller.isOffline)
                    }
                }
    }

    override fun setOutGoingNumber(number: String) {
        mIncomingNumber = number
        mSetting.setOutgoing(true)
    }

    override fun canReadPhoneState(): Boolean {
        return mPermission.canReadPhoneState()
    }

    private fun showNumber(number: INumber) {
        mCallRecord.logNumber = number.number
        mCallRecord.logName = number.name
        mCallRecord.logGeo = number.province + " " + number.city
        if (mCallRecord.isActive) {
            mView.show(number)
        }
        checkAutoHangUp()
    }

    private fun bindPluginService() {
        Log.e(TAG, "bindPluginService")
        if (mPluginService != null) {
            Log.d(TAG, "plugin service have been started.")
            return
        }
        if (!mSetting.isAutoHangup && !mSetting.isAddingCallLog) {
            Log.d(TAG, "Plugin function is not enabled.")
            return
        }
        mAutoHangup = false
        if (mConnection == null) {
            mConnection = newConnection()
        }
        if (mPluginIntent == null) {
            mPluginIntent = Intent().setComponent(ComponentName(
                    "org.xdty.callerinfo.plugin",
                    "org.xdty.callerinfo.plugin.PluginService"))
        }
        mView.context.bindService(mPluginIntent, mConnection!!, Context.BIND_AUTO_CREATE)
    }

    private fun newConnection(): PluginConnection {
        return object : PluginConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d(TAG, "onServiceConnected: $name")
                mPluginService = IPluginService.Stub.asInterface(service)
                if (mWaitingCheckHangup) {
                    checkAutoHangUp()
                }
                mWaitingCheckHangup = false
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.d(TAG, "onServiceDisconnected: $name")
                mPluginService = null
            }
        }
    }

    private fun unBindPluginService() {
        Log.e(TAG, "unBindPluginService")
        if (mPluginService == null) {
            Log.d(TAG, "unBindPluginService: plugin service is not started.")
            return
        }
        mView.context.applicationContext.unbindService(mConnection!!)
        mPluginService = null
    }

    private fun checkAutoHangUp() {
        Log.d(TAG, "checkAutoHangUp")
        if (mPluginService == null) {
            Log.d(TAG, "checkAutoHangUp: plugin service is not started.")
            mWaitingCheckHangup = true
            return
        }
        if (!mCallRecord.isIncoming && mSetting.isDisableOutGoingHangup) {
            Log.d(TAG, "checkAutoHangUp: auto hangup is disabled when outgoing.")
            return
        }
        if (mCallRecord.isIncoming && mSetting.isTemporaryDisableHangup
                && isTriggeredRepeatIncomingCall(mCallRecord.logNumber)) {
            Log.d(TAG, "checkAutoHangUp: auto hangup is disabled when repeated.")
            return
        }
        try {
            if (mSetting.isAutoHangup) { // hang up phone call which number name contains key words
                val keywords = mSetting.keywords
                for (keyword in keywords.split(" ").toTypedArray()) {
                    if (mCallRecord.matchName(keyword)) {
                        Log.d(TAG, "checkAutoHangUp: match keywords")
                        mAutoHangup = true
                        break
                    }
                }
                // hang up phone call which number geo in black list
                val geoKeywords = mSetting.geoKeyword
                if (!geoKeywords.isEmpty() && mCallRecord.isGeoValid) {
                    var hangUp = false
                    for (keyword in geoKeywords.split(" ").toTypedArray()) {
                        if (!keyword.startsWith("!")) { // number geo is in black list
                            if (mCallRecord.matchGeo(keyword)) {
                                Log.d(TAG, "checkAutoHangUp: match geo blacklist")
                                hangUp = true
                                break
                            }
                        } else if (mCallRecord.matchGeo(keyword.replace("!", ""))) { // number geo is in white list
                            Log.d(TAG, "checkAutoHangUp: match geo whitelist")
                            hangUp = false
                            break
                        } else { // number geo is not in white list
                            Log.d(TAG, "checkAutoHangUp: match geo is not in white list")
                            hangUp = true
                        }
                    }
                    if (hangUp) {
                        Log.d(TAG, "checkAutoHangUp: geo hangup")
                        mAutoHangup = true
                    }
                }
                // hang up phone call which number start with keyword
                val numberKeywords = mSetting.numberKeyword.replace("\\*".toRegex(), "")
                if (!numberKeywords.isEmpty()) {
                    for (keyword in numberKeywords.split(" ").toTypedArray()) {
                        if (mCallRecord.matchNumber(keyword)) {
                            Log.d(TAG, "checkAutoHangUp: match number")
                            mAutoHangup = true
                        }
                    }
                }
                // hang up phone call
                if (mAutoHangup && mPluginService != null) {
                    Log.d(TAG, "hangUpPhoneCall")
                    mPluginService!!.hangUpPhoneCall()
                }
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun updateCallLog(number: String?, name: String?) {
        Log.d(TAG, name)
        if (mPluginService != null) {
            try {
                mPluginService!!.updateCallLog(number, name)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    private fun reportFetchedNumber() { // Currently do noting, let the alarm handle marked number.
    }

    override fun start() {
        mDatabase = instance
    }

    internal interface PluginConnection : ServiceConnection
    companion object {
        private val TAG = PhoneStatePresenter::class.java.simpleName
    }

    init {
        appComponent.inject(this)
    }
}