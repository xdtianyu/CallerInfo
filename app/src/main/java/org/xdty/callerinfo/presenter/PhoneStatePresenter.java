package org.xdty.callerinfo.presenter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.contract.PhoneStateContract;
import org.xdty.callerinfo.data.CallerDataSource;
import org.xdty.callerinfo.model.CallRecord;
import org.xdty.callerinfo.model.SearchMode;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.database.DatabaseImpl;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.plugin.IPluginService;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Contact;
import org.xdty.phone.number.model.INumber;

import javax.inject.Inject;

import rx.functions.Action1;

public class PhoneStatePresenter implements PhoneStateContract.Presenter {

    private final static String TAG = PhoneStatePresenter.class.getSimpleName();
    @Inject
    Setting mSetting;
    @Inject
    Permission mPermission;
    @Inject
    Database mDatabase;
    @Inject
    Alarm mAlarm;
    @Inject
    Contact mContact;
    @Inject
    CallerDataSource mCallerDataSource;

    CallRecord mCallRecord;
    private PhoneStateContract.View mView;
    private String mIncomingNumber;

    private IPluginService mPluginService;
    private Intent mPluginIntent;
    private PluginConnection mConnection;
    private boolean mAutoHangup = false;
    private boolean mWaitingCheckHangup = false;

    public PhoneStatePresenter(PhoneStateContract.View view) {
        mView = view;
        mCallRecord = new CallRecord();
        Application.getAppComponent().inject(this);
    }

    @Override
    public boolean matchIgnore(String number) {

        if (!TextUtils.isEmpty(number)) {
            number = number.replaceAll(" ", "");
            String ignoreRegex = mSetting.getIgnoreRegex();
            return number.matches(ignoreRegex);
        }

        return false;
    }

    @Override
    public void handleRinging(String number) {

        mCallRecord.ring();

        if (!TextUtils.isEmpty(number)) {
            mIncomingNumber = number;
            mCallRecord.setLogNumber(number);
            searchNumber(number);
        }
    }

    @Override
    public void handleOffHook(String number) {

        if (System.currentTimeMillis() - mCallRecord.getHook() < 1000 &&
                mCallRecord.isEqual(number)) {
            Log.e(TAG, "duplicate hook, ignore.");
            return;
        }

        mCallRecord.hook();
        if (mCallRecord.isIncoming()) {
            if (mSetting.isHidingOffHook()) {
                mView.hide(number);
            }
        } else { // outgoing call
            if (mSetting.isShowingOnOutgoing()) {
                if (TextUtils.isEmpty(number)) {
                    Log.d(TAG, "number is null. " + TextUtils.isEmpty(mIncomingNumber));
                    number = mIncomingNumber;
                    mCallRecord.setLogNumber(number);
                    mIncomingNumber = null;
                }
                searchNumber(number);
            }
        }

    }

    @Override
    public void handleIdle(String number) {
        mCallRecord.idle();

        if (checkClose(number)) {
            return;
        }

        boolean saveLog = mSetting.isAddingCallLog();
        if (isIncoming(mIncomingNumber) && !mCallerDataSource.isIgnoreContact(mIncomingNumber)) {
            saveInCall();
            mIncomingNumber = null;
            if (isRingOnce() && mSetting.isAddingRingOnceCallLog()) {
                if (mAutoHangup) {
                    // ring once cased by auto hangup
                    mCallRecord.setLogName(
                            mView.getContext().getString(R.string.auto_hangup), saveLog);

                } else {
                    mCallRecord.setLogName(mView.getContext().getString(R.string.ring_once),
                            saveLog);
                }
                saveLog = true;
            }
        }

        if (mView.isShowing()) {
            if (mCallRecord.isValid()) {
                if (mCallRecord.isNameValid()) {
                    if (saveLog) {
                        updateCallLog(mCallRecord.getLogNumber(), mCallRecord.getLogName());
                    }
                    if (mSetting.isAutoReportEnabled()) {
                        reportFetchedNumber();
                    }
                } else {
                    if (mSetting.isMarkingEnabled() && mCallRecord.isAnswered() &&
                            !mCallerDataSource.isIgnoreContact(mCallRecord.getLogNumber()) &&
                            !isNotMarkContact(mCallRecord.getLogNumber())) {
                        mView.showMark(mCallRecord.getLogNumber());
                    }
                }
            }
        }

        resetCallRecord();
        mAutoHangup = false;

        mView.close(number);
        mSetting.setOutgoing(false);
        unBindPluginService();
    }

    @Override
    public void resetCallRecord() {
        mCallRecord.reset();
    }

    @Override
    public boolean checkClose(String number) {
        return TextUtils.isEmpty(number) && mCallRecord.callDuration() == -1;
    }

    @Override
    public boolean isIncoming(String number) {
        return mCallRecord.isIncoming() && !TextUtils.isEmpty(mIncomingNumber);
    }

    @Override
    public void saveInCall() {
        mDatabase.saveInCall(
                new InCall(mIncomingNumber, mCallRecord.time(), mCallRecord.ringDuration(),
                        mCallRecord.callDuration()));
    }

    @Override
    public boolean isRingOnce() {
        return mCallRecord.ringDuration() < 3000 && mCallRecord.callDuration() <= 0;
    }

    private boolean isNotMarkContact(String number) {
        return mSetting.isNotMarkContact() && mPermission.canReadContact()
                && mContact.isExist(number);
    }

    private boolean isTriggeredRepeatIncomingCall(String number) {
        // 0 -> twice, 1 -> third, ...
        return mDatabase.getInCallCount(number) >= mSetting.getRepeatedCountIndex() + 1;
    }

    @Override
    public void searchNumber(final String number) {

        if (TextUtils.isEmpty(number)) {
            Log.e(TAG, "searchNumber: number is null!");
            return;
        }

        final SearchMode mode = mCallerDataSource.getSearchMode(number);

        if (mode == SearchMode.IGNORE) {
            return;
        }

        if (mSetting.isAutoHangup() || mSetting.isAddingCallLog()) {
            bindPluginService();
        }

        mView.showSearching();

        mCallerDataSource.getCaller(number, mode == SearchMode.OFFLINE)
                .subscribe(new Action1<Caller>() {
                    @Override
                    public void call(Caller caller) {
                        Log.d(TAG, "call: " + number + "->" + caller.getNumber() +
                                ", offline: " + caller.isOffline());
                        if (!caller.isEmpty()) {
                            showNumber(caller);
                        } else if (mCallRecord.isActive()) {
                            mView.showFailed(!caller.isOffline());
                        }
                    }
                });
    }

    @Override
    public void setOutGoingNumber(String number) {
        mIncomingNumber = number;
        mSetting.setOutgoing(true);
    }

    @Override
    public boolean canReadPhoneState() {
        return mPermission.canReadPhoneState();
    }

    private void showNumber(INumber number) {

        mCallRecord.setLogNumber(number.getNumber());
        mCallRecord.setLogName(number.getName());
        mCallRecord.setLogGeo(number.getProvince() + " " + number.getCity());

        if (mCallRecord.isActive()) {
            mView.show(number);
        }

        checkAutoHangUp();
    }

    private void bindPluginService() {

        Log.e(TAG, "bindPluginService");

        if (mPluginService != null) {
            Log.d(TAG, "plugin service have been started.");
            return;
        }

        if (!mSetting.isAutoHangup() && !mSetting.isAddingCallLog()) {
            Log.d(TAG, "Plugin function is not enabled.");
            return;
        }

        mAutoHangup = false;

        if (mConnection == null) {
            mConnection = newConnection();
        }

        if (mPluginIntent == null) {
            mPluginIntent = new Intent().setComponent(new ComponentName(
                    "org.xdty.callerinfo.plugin",
                    "org.xdty.callerinfo.plugin.PluginService"));
        }

        mView.getContext().bindService(mPluginIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private PluginConnection newConnection() {
        return new PluginConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected: " + name.toString());
                mPluginService = IPluginService.Stub.asInterface(service);
                if (mWaitingCheckHangup) {
                    checkAutoHangUp();
                }
                mWaitingCheckHangup = false;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected: " + name.toString());
                mPluginService = null;
            }
        };
    }

    private void unBindPluginService() {
        Log.e(TAG, "unBindPluginService");
        if (mPluginService == null) {
            Log.d(TAG, "unBindPluginService: plugin service is not started.");
            return;
        }
        mView.getContext().getApplicationContext().unbindService(mConnection);
        mPluginService = null;
    }

    private void checkAutoHangUp() {
        Log.d(TAG, "checkAutoHangUp");

        if (mPluginService == null) {
            Log.d(TAG, "checkAutoHangUp: plugin service is not started.");
            mWaitingCheckHangup = true;
            return;
        }

        if (!mCallRecord.isIncoming() && mSetting.isDisableOutGoingHangup()) {
            Log.d(TAG, "checkAutoHangUp: auto hangup is disabled when outgoing.");
            return;
        }

        if (mCallRecord.isIncoming() && mSetting.isTemporaryDisableHangup()
                && isTriggeredRepeatIncomingCall(mCallRecord.getLogNumber())) {
            Log.d(TAG, "checkAutoHangUp: auto hangup is disabled when repeated.");
            return;
        }

        try {
            if (mSetting.isAutoHangup()) {
                // hang up phone call which number name contains key words
                String keywords = mSetting.getKeywords();
                for (String keyword : keywords.split(" ")) {
                    if (mCallRecord.matchName(keyword)) {
                        mAutoHangup = true;
                        break;
                    }
                }

                // hang up phone call which number geo in black list
                String geoKeywords = mSetting.getGeoKeyword();
                if (!geoKeywords.isEmpty() && mCallRecord.isGeoValid()) {
                    boolean hangUp = false;
                    for (String keyword : geoKeywords.split(" ")) {
                        if (!keyword.startsWith("!")) {
                            // number geo is in black list
                            if (mCallRecord.matchGeo(keyword)) {
                                hangUp = true;
                                break;
                            }
                        } else if (mCallRecord.matchGeo(keyword.replace("!", ""))) {
                            // number geo is in white list
                            hangUp = false;
                            break;
                        } else {
                            // number geo is not in white list
                            hangUp = true;
                        }
                    }
                    if (hangUp) {
                        mAutoHangup = true;
                    }
                }

                // hang up phone call which number start with keyword
                String numberKeywords = mSetting.getNumberKeyword().replaceAll("\\*", "");
                if (!numberKeywords.isEmpty()) {
                    for (String keyword : numberKeywords.split(" ")) {
                        if (mCallRecord.matchNumber(keyword)) {
                            mAutoHangup = true;
                        }
                    }
                }

                // hang up phone call
                if (mAutoHangup && mPluginService != null) {
                    mPluginService.hangUpPhoneCall();
                }
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateCallLog(String number, String name) {
        Log.d(TAG, name);
        if (mPluginService != null) {
            try {
                mPluginService.updateCallLog(number, name);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void reportFetchedNumber() {
        // Currently do noting, let the alarm handle marked number.
    }

    @Override
    public void start() {
        mDatabase = DatabaseImpl.getInstance();
    }

    interface PluginConnection extends ServiceConnection {}
}
