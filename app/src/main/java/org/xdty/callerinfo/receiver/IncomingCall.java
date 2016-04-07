package org.xdty.callerinfo.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.permission.PermissionImpl;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.plugin.IPluginService;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import java.util.List;

import wei.mark.standout.StandOutWindow;

public class IncomingCall extends BroadcastReceiver {

    private final static String TAG = IncomingCall.class.getSimpleName();

    private static IncomingCallListener mIncomingCallListener;
    private static String mIncomingNumber = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onReceive: " + intent.toString() + " " +
                    Utils.bundleToString(intent.getExtras()));
        }
        if (mIncomingCallListener == null) {
            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            mIncomingCallListener = new IncomingCallListener(context);
            telephonyManager.listen(mIncomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            mIncomingNumber = intent.getExtras().getString(Intent.EXTRA_PHONE_NUMBER);
        }
    }

    public static class IncomingCallListener extends PhoneStateListener {

        private final Context mContext;
        private boolean isShowing = false;
        private long ringStartTime = -1;
        private long hookStartTime = -1;
        private long idleStartTime = -1;
        private long ringTime = -1;
        private long duration = -1;
        private boolean mIgnoreContact;
        private boolean mShowContactOffline = false;
        private boolean mIsInContacts = false;
        private IPluginService mPluginService;
        private Intent mPluginIntent;
        private PluginConnection mConnection;
        private String mLogNumber;
        private String mLogName;
        private String mLogGeo;
        private boolean mAutoHangup = false;
        private boolean mIgnore = false;

        private Setting mSetting;
        private Permission mPermission;

        public IncomingCallListener(Context context) {
            mContext = context.getApplicationContext();
            mSetting = new SettingImpl(mContext);
            mPermission = new PermissionImpl(mContext);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (!TextUtils.isEmpty(incomingNumber)) {
                incomingNumber = incomingNumber.replaceAll(" ", "");
                String ignoreRegex = mSetting.getIgnoreRegex();
                mIgnore = incomingNumber.matches(ignoreRegex);
            }

            if (mIgnore) {
                return;
            }

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    ringStartTime = System.currentTimeMillis();
                    Log.d(TAG, "CALL_STATE_RINGING: " + ringStartTime);
                    if (!TextUtils.isEmpty(incomingNumber)) {
                        mIncomingNumber = incomingNumber;
                        show(incomingNumber);
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    hookStartTime = System.currentTimeMillis();
                    Log.d(TAG, "CALL_STATE_OFFHOOK: " + hookStartTime);

                    if (ringStartTime != -1) {
                        ringTime = hookStartTime - ringStartTime;
                        if (mSetting.isHidingOffHook()) {
                            hide(incomingNumber);
                        }
                    } else {
                        if (mSetting.isShowingOnOutgoing()) {
                            if (TextUtils.isEmpty(incomingNumber)) {
                                Log.d(TAG, "number is null. " + TextUtils.isEmpty(mIncomingNumber));
                                incomingNumber = mIncomingNumber;
                                mIncomingNumber = null;
                            }
                            show(incomingNumber);
                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    idleStartTime = System.currentTimeMillis();
                    Log.d(TAG, "CALL_STATE_IDLE: " + idleStartTime);

                    if (ringTime == -1) {
                        ringTime = idleStartTime - ringStartTime;
                        duration = 0;
                    } else {
                        duration = idleStartTime - hookStartTime;
                    }

                    close(incomingNumber);
                    break;
            }
        }

        void show(String incomingNumber) {

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "show window: " + TextUtils.isEmpty(incomingNumber));
            }

            if (TextUtils.isEmpty(incomingNumber)) {
                return;
            }

            if (incomingNumber.startsWith("+86")) {
                incomingNumber = incomingNumber.replace("+86", "");
            }

            if (incomingNumber.startsWith("86")) {
                incomingNumber = incomingNumber.replaceFirst("^86", "");
            }

            if (!isShowing) {
                mIgnoreContact = mSetting.isIgnoreKnownContact() && mPermission.canReadContact();
                if (mIgnoreContact && Utils.isContactExists(mContext, incomingNumber)) {
                    mIsInContacts = true;
                    if (mSetting.isShowingContactOffline()) {
                        mShowContactOffline = true;
                    } else {
                        mShowContactOffline = false;
                        return;
                    }
                } else {
                    mIsInContacts = false;
                    mShowContactOffline = false;
                }

                isShowing = true;

                final boolean hangup = mSetting.isAutoHangup();
                final boolean saveLog = mSetting.isAddingCallLog();

                List<Caller> callers = Caller.find(Caller.class, "number=?", incomingNumber);

                if (callers.size() > 0) {
                    Caller caller = callers.get(0);
                    if (caller.isUpdated()) {
                        if (hangup || saveLog) {
                            bindPluginService(hangup, caller);
                        }
                        Utils.showWindow(mContext, caller, FloatWindow.CALLER_FRONT);
                        return;
                    } else {
                        caller.delete();
                    }
                }

                new PhoneNumber(mContext, mShowContactOffline, new PhoneNumber.Callback() {
                    @Override
                    public void onResponseOffline(INumber number) {
                        if (isShowing && number != null) {
                            if (hangup || saveLog) {
                                bindPluginService(hangup, number);
                            }
                            Utils.showWindow(mContext, number, FloatWindow.CALLER_FRONT);
                        }
                    }

                    @Override
                    public void onResponse(INumber number) {
                        if (isShowing && number != null) {
                            new Caller(number, !number.isOnline()).save();
                            if (hangup || saveLog) {
                                bindPluginService(hangup, number);
                            }
                            Utils.showWindow(mContext, number, FloatWindow.CALLER_FRONT);
                        }
                    }

                    @Override
                    public void onResponseFailed(INumber number, boolean isOnline) {
                        if (isOnline) {
                            Utils.sendData(mContext, FloatWindow.WINDOW_ERROR,
                                    R.string.online_failed, FloatWindow.CALLER_FRONT);
                        } else {
                            Utils.showTextWindow(mContext, R.string.offline_failed,
                                    FloatWindow.CALLER_FRONT);
                        }
                    }
                }).fetch(incomingNumber);
            }
        }

        void hide(String incomingNumber) {
            Log.d(TAG, "hide");
            if (isShowing) {
                StandOutWindow.hide(mContext, FloatWindow.class, FloatWindow.CALLER_FRONT);
            }
        }

        void close(String incomingNumber) {
            Log.d(TAG, "ringStartTime:" + ringStartTime +
                    ", ringTime: " + ringTime + ", duration: " + duration);

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "close window: " + TextUtils.isEmpty(incomingNumber));
            }

            if (TextUtils.isEmpty(incomingNumber) && duration == -1) {
                return;
            }

            boolean saveLog = mSetting.isAddingCallLog();
            if (ringStartTime != -1 && !TextUtils.isEmpty(mIncomingNumber) && !mIsInContacts) {
                new InCall(mIncomingNumber, ringStartTime, ringTime, duration).save();
                mIncomingNumber = null;

                if (ringTime < 3000 && duration <= 0) {
                    saveLog = true;
                    if (TextUtils.isEmpty(mLogName)) {
                        mLogName = "";
                    }
                    if (mAutoHangup) {
                        mLogName += " " + mContext.getString(R.string.auto_hangup);
                    } else {
                        mLogName += " " + mContext.getString(R.string.ring_once);
                    }
                }
            }

            if (isShowing) {
                isShowing = false;
                StandOutWindow.closeAll(mContext, FloatWindow.class);
                if (mLogName != null && mLogNumber != null) {
                    if (saveLog && !mLogName.isEmpty()) {
                        updateCallLog(mLogNumber, mLogName);
                    }
                    unBindPluginService();
                }
            }

            ringStartTime = -1;
            hookStartTime = -1;
            idleStartTime = -1;
            ringTime = -1;
            duration = -1;
            mLogNumber = null;
            mLogGeo = null;
            mLogName = null;
            mAutoHangup = false;
        }

        private void bindPluginService(boolean hangup, INumber number) {

            mLogNumber = number.getNumber();
            mLogName = number.getName();
            mLogGeo = number.getProvince() + " " + number.getCity();
            mAutoHangup = false;

            if (mConnection == null) {
                mConnection = newConnection();
            }

            mConnection.update(hangup, number);

            if (mPluginIntent == null) {
                mPluginIntent = new Intent().setComponent(new ComponentName(
                        "org.xdty.callerinfo.plugin",
                        "org.xdty.callerinfo.plugin.PluginService"));
            }

            mContext.startService(mPluginIntent);
            mContext.getApplicationContext().bindService(mPluginIntent, mConnection,
                    Context.BIND_AUTO_CREATE);
        }

        private PluginConnection newConnection() {
            return new PluginConnection() {

                boolean hangup;
                INumber number;

                @Override
                public void update(boolean hangup, INumber number) {
                    this.hangup = hangup;
                    this.number = number;
                }

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.d(TAG, "onServiceConnected: " + name.toString());
                    mPluginService = IPluginService.Stub.asInterface(service);
                    try {
                        if (hangup) {
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
            mContext.getApplicationContext().unbindService(mConnection);
            mContext.stopService(mPluginIntent);
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

        interface PluginConnection extends ServiceConnection {
            void update(boolean hangup, INumber number);
        }
    }
}
