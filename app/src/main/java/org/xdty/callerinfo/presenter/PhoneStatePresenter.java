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
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.database.DatabaseImpl;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.plugin.IPluginService;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import rx.functions.Action1;

public class PhoneStatePresenter implements PhoneStateContract.Presenter, PhoneNumber.Callback {

    private final static String TAG = PhoneStatePresenter.class.getSimpleName();

    private PhoneStateContract.View mView;
    private Setting mSetting;
    private Permission mPermission;
    private CallRecord mCallRecord;
    private Database mDatabase;

    private String mIncomingNumber;

    private IPluginService mPluginService;
    private Intent mPluginIntent;
    private PluginConnection mConnection;
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
        if (mCallRecord.isIncoming()) {
            if (mSetting.isHidingOffHook()) {
                mView.hide(number);
            }
        } else { // outgoing call
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

        if (checkClose(number)) {
            return;
        }

        boolean saveLog = mSetting.isAddingCallLog();
        if (isIncoming(mIncomingNumber) && !ignoreContact(mIncomingNumber)) {
            saveInCall();
            mIncomingNumber = null;
            if (isRingOnce()) {
                saveLog = true;
                if (mAutoHangup) {
                    // ring once cased by auto hangup
                    mCallRecord.appendName(mView.getContext().getString(R.string.auto_hangup));
                } else {
                    mCallRecord.appendName(mView.getContext().getString(R.string.ring_once));
                }
            }
        }

        if (mView.isShowing()) {
            if (mCallRecord.isValid()) {
                if (saveLog && mCallRecord.isNameValid()) {
                    updateCallLog(mCallRecord.getLogNumber(), mCallRecord.getLogName());
                }
                unBindPluginService();
            }
        }

        resetCallRecord();
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

    @Override
    public boolean ignoreContact(String number) {
        return mSetting.isIgnoreKnownContact() && mPermission.canReadContact()
                && Utils.isContactExists(
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

        final String fixedNumber = fixNumber(number);
        final SearchMode mode = getSearchMode(fixedNumber);

        if (mode == SearchMode.IGNORE) {
            return;
        }

        mDatabase.findCaller(number).subscribe(new Action1<Caller>() {
            @Override
            public void call(Caller caller) {
                if (caller != null) {
                    if (caller.isUpdated()) {
                        showNumber(caller);
                        return;
                    } else {
                        mDatabase.removeCaller(caller);
                    }
                }
                mView.showSearching();
                new PhoneNumber(mView.getContext(), mode == SearchMode.OFFLINE,
                        PhoneStatePresenter.this).fetch(fixedNumber);
            }
        });
    }

    @Override
    public void handleResponse(INumber number, boolean isOnline) {
        if (number == null) {
            return;
        }
        if (isOnline) {
            mDatabase.saveCaller(new Caller(number, !number.isOnline()));
        }

        showNumber(number);
    }

    @Override
    public void handleResponseFailed(INumber number, boolean isOnline) {
        if (mCallRecord.isActive()) {
            mView.showFailed(isOnline);
        }
    }

    @Override
    public void setOutGoingNumber(String number) {
        mIncomingNumber = number;
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
        if (mCallRecord.isActive()) {
            mView.show(number);
        }
    }

    private void bindPluginService(INumber number) {

        if (!mSetting.isAutoHangup() && !mSetting.isAddingCallLog()) {
            return;
        }

        mCallRecord.setLogNumber(number.getNumber());
        mCallRecord.setLogName(number.getName());
        mCallRecord.setLogGeo(number.getProvince() + " " + number.getCity());
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
                            if (mCallRecord.matchName(keyword)) {
                                mPluginService.hangUpPhoneCall();
                                mAutoHangup = true;
                            }
                        }

                        String geoKeywords = mSetting.getGeoKeyword();
                        if (!geoKeywords.isEmpty() && mCallRecord.isGeoValid()) {
                            boolean hangUp = false;
                            for (String keyword : geoKeywords.split(" ")) {
                                if (!keyword.startsWith("!")) {
                                    if (mCallRecord.matchGeo(keyword)) {
                                        hangUp = true;
                                        break;
                                    }
                                } else if (mCallRecord.matchGeo(keyword.replace("!", ""))) {
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
                                if (mCallRecord.matchNumber(keyword)) {
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

    @Override
    public void start() {
        mDatabase = DatabaseImpl.getInstance();
    }

    interface PluginConnection extends ServiceConnection {
        void update(INumber number);
    }
}
