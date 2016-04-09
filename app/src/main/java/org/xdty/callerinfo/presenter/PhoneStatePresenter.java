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
import org.xdty.callerinfo.contract.PhoneStateContract;
import org.xdty.callerinfo.model.CallRecord;
import org.xdty.callerinfo.model.SearchMode;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.plugin.IPluginService;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import java.util.List;

public class PhoneStatePresenter implements PhoneStateContract.Presenter, PhoneNumber.Callback {

    private final static String TAG = PhoneStatePresenter.class.getSimpleName();

    private PhoneStateContract.View mView;
    private Setting mSetting;
    private Permission mPermission;
    private CallRecord mCallRecord;

    private String mIncomingNumber;
    private IPluginService mPluginService;
    private Intent mPluginIntent;
    private PluginConnection mConnection;
    private String mLogNumber;
    private String mLogName;
    private String mLogGeo;
    private boolean mAutoHangup = false;

    public PhoneStatePresenter(PhoneStateContract.View view, Setting setting,
            Permission permission, CallRecord callRecord) {
        mView = view;
        mSetting = setting;
        mPermission = permission;
        mCallRecord = callRecord;
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
            searchNumber(number);
        }
    }

    @Override
    public void handleOffHook(String number) {
        mCallRecord.hook();
        if (mCallRecord.ringTime() != -1) {
            mCallRecord.setRingDuration(true);
            if (mSetting.isHidingOffHook()) {
                mView.hide(number);
            }
        } else {
            if (mSetting.isShowingOnOutgoing()) {
                if (TextUtils.isEmpty(number)) {
                    Log.d(TAG, "number is null. " + TextUtils.isEmpty(mIncomingNumber));
                    number = mIncomingNumber;
                    mIncomingNumber = null;
                }
                searchNumber(number);
            }
        }

    }

    @Override
    public void handleIdle(String number) {
        mCallRecord.idle();

        if (mCallRecord.ringDuration() == -1) {
            mCallRecord.setRingDuration(false);
            mCallRecord.setCallDuration(false);
        } else {
            mCallRecord.setCallDuration(true);
        }

        if (checkClose(number)) {
            return;
        }

        boolean saveLog = mSetting.isAddingCallLog();
        if (isIncoming(mIncomingNumber) && !ignoreContact(mIncomingNumber)) {
            saveCallLog();
            mIncomingNumber = null;
            if (isRingOnce()) {
                saveLog = true;
                if (TextUtils.isEmpty(mLogName)) {
                    mLogName = "";
                }
                if (mAutoHangup) {
                    mLogName += " " + mView.getContext().getString(R.string.auto_hangup);
                } else {
                    mLogName += " " + mView.getContext().getString(R.string.ring_once);
                }
            }
        }

        if (mView.isShowing()) {
            if (mLogName != null && mLogNumber != null) {
                if (saveLog && !mLogName.isEmpty()) {
                    updateCallLog(mLogNumber, mLogName);
                }
                unBindPluginService();
            }
        }

        resetCallRecord();
        mLogNumber = null;
        mLogGeo = null;
        mLogName = null;
        mAutoHangup = false;

        mView.close(number);
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
        return mCallRecord.ringTime() != -1 && !TextUtils.isEmpty(mIncomingNumber);
    }

    @Override
    public void saveCallLog() {
        new InCall(mIncomingNumber, mCallRecord.ringTime(), mCallRecord.ringDuration(),
                mCallRecord.callDuration()).save();
    }

    @Override
    public boolean isRingOnce() {
        return mCallRecord.ringTime() < 3000 && mCallRecord.callDuration() <= 0;
    }

    @Override
    public boolean ignoreContact(String number) {
        return mSetting.isIgnoreKnownContact() && mPermission.canReadContact() && Utils.isContactExists(
                mView.getContext(), number);
    }

    @Override
    public SearchMode getSearchMode(String number) {
        SearchMode mode = SearchMode.ONLINE;
        if (ignoreContact(number)) {
            if (mSetting.isShowingContactOffline()) {
                mode = SearchMode.OFFLINE;
            } else {
                mode = SearchMode.IGNORE;
            }
        }
        return mode;
    }

    @Override
    public void searchNumber(String number) {

        number = fixNumber(number);

        SearchMode mode = getSearchMode(number);
        if (mode == SearchMode.IGNORE) {
            return;
        }

        List<Caller> callers = Caller.find(Caller.class, "number=?", number);

        if (callers.size() > 0) {
            Caller caller = callers.get(0);
            if (caller.isUpdated()) {
                showNumber(caller);
                return;
            } else {
                caller.delete();
            }
        }

        new PhoneNumber(mView.getContext(), mode == SearchMode.OFFLINE, this).fetch(number);
    }

    @Override
    public void handleResponse(INumber number, boolean isOnline) {
        if (number == null) {
            return;
        }
        if (isOnline) {
            new Caller(number, !number.isOnline()).save();
        }

        showNumber(number);
    }

    @Override
    public void handleResponseFailed(INumber number, boolean isOnline) {
        mView.showFailed(isOnline);
    }

    private String fixNumber(String number) {
        String fixedNumber = number;
        if (number.startsWith("+86")) {
            fixedNumber = number.replace("+86", "");
        }

        if (number.startsWith("86")) {
            fixedNumber = number.replaceFirst("^86", "");
        }
        return fixedNumber;
    }

    private void showNumber(INumber number) {
        bindPluginService(number);
        mView.show(number);
    }

    private void bindPluginService(INumber number) {

        if (!mSetting.isAutoHangup() && !mSetting.isAddingCallLog()) {
            return;
        }

        mLogNumber = number.getNumber();
        mLogName = number.getName();
        mLogGeo = number.getProvince() + " " + number.getCity();
        mAutoHangup = false;

        if (mConnection == null) {
            mConnection = newConnection();
        }

        mConnection.update(number);

        if (mPluginIntent == null) {
            mPluginIntent = new Intent().setComponent(new ComponentName(
                    "org.xdty.callerinfo.plugin",
                    "org.xdty.callerinfo.plugin.PluginService"));
        }

        mView.getContext().startService(mPluginIntent);
        mView.getContext().bindService(mPluginIntent, mConnection,
                Context.BIND_AUTO_CREATE);
    }

    private PluginConnection newConnection() {
        return new PluginConnection() {

            INumber number;

            @Override
            public void update(INumber number) {
                this.number = number;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected: " + name.toString());
                mPluginService = IPluginService.Stub.asInterface(service);
                try {
                    if (mSetting.isAutoHangup()) {
                        String keywords = mSetting.getKeywords();
                        for (String keyword : keywords.split(" ")) {
                            if (!TextUtils.isEmpty(mLogName) &&
                                    mLogName.contains(keyword)) {
                                mPluginService.hangUpPhoneCall();
                                mAutoHangup = true;
                            }
                        }

                        String geoKeywords = mSetting.getGeoKeyword();
                        if (!geoKeywords.isEmpty() && !TextUtils.isEmpty(mLogGeo)) {
                            boolean hangUp = false;
                            for (String keyword : geoKeywords.split(" ")) {
                                if (!keyword.startsWith("!")) {
                                    if (mLogGeo.contains(keyword)) {
                                        hangUp = true;
                                        break;
                                    }
                                } else if (mLogGeo.contains(keyword.replace("!", ""))) {
                                    hangUp = false;
                                    break;
                                } else {
                                    hangUp = true;
                                }
                            }
                            if (hangUp) {
                                mPluginService.hangUpPhoneCall();
                                mAutoHangup = true;
                            }
                        }

                        String numberKeywords = mSetting.getNumberKeyword();
                        if (!numberKeywords.isEmpty()) {
                            for (String keyword : numberKeywords.split(" ")) {
                                if (!TextUtils.isEmpty(mLogNumber) &&
                                        mLogNumber.startsWith(keyword)) {
                                    mPluginService.hangUpPhoneCall();
                                    mAutoHangup = true;
                                }
                            }
                        }
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected: " + name.toString());
                mPluginService = null;
            }
        };
    }

    private void unBindPluginService() {
        mView.getContext().getApplicationContext().unbindService(mConnection);
        mView.getContext().stopService(mPluginIntent);
        mLogNumber = null;
        mLogName = null;
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

    @Override
    public void onResponseOffline(INumber number) {
        handleResponse(number, false);
    }

    @Override
    public void onResponse(INumber number) {
        handleResponse(number, true);
    }

    @Override
    public void onResponseFailed(INumber number, boolean isOnline) {
        handleResponseFailed(number, isOnline);
    }

    interface PluginConnection extends ServiceConnection {
        void update(INumber number);
    }
}
