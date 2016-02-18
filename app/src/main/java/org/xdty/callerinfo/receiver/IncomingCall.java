package org.xdty.callerinfo.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.plugin.IPluginService;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import java.util.List;

import wei.mark.standout.StandOutWindow;

public class IncomingCall extends BroadcastReceiver {

    public final static String TAG = IncomingCall.class.getSimpleName();

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

        private final boolean DEBUG = BuildConfig.DEBUG;
        private Context context;
        private boolean isShowing = false;
        private long ringStartTime = -1;
        private long hookStartTime = -1;
        private long idleStartTime = -1;
        private long ringTime = -1;
        private long duration = -1;
        private SharedPreferences mPrefs;
        private boolean mIgnoreContact;
        private boolean mShowContactOffline = false;
        private boolean mIsInContacts = false;
        private String mOutgoingKey;
        private String mHideKey;
        private String mKeywordKey;
        private String mKeywordDefault;
        private String mGeoKeywordKey;
        private String mNumberKeywordKey;

        private IPluginService mPluginService;
        private Intent mPluginIntent;
        private ServiceConnection mConnection;
        private String mLogNumber;
        private String mLogName;
        private String mLogGeo;
        private boolean mAutoHangup = false;
        private boolean mIgnore = false;

        public IncomingCallListener(Context context) {
            this.context = context;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            mOutgoingKey = context.getString(R.string.display_on_outgoing_key);
            mHideKey = context.getString(R.string.hide_when_off_hook_key);
            mKeywordKey = context.getString(R.string.hangup_keyword_key);
            mKeywordDefault = context.getString(R.string.hangup_keyword_default);
            mGeoKeywordKey = context.getString(R.string.hangup_geo_keyword_key);
            mNumberKeywordKey = context.getString(R.string.hangup_number_keyword_key);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (!TextUtils.isEmpty(incomingNumber)) {
                incomingNumber = incomingNumber.replaceAll(" ", "");
                String ignoreRegex =
                        mPrefs.getString(context.getString(R.string.ignore_regex_key), "");
                ignoreRegex = ignoreRegex.replace("*", "[0-9]").replace(" ", "|");
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
                        if (mPrefs.getBoolean(mHideKey, false)) {
                            hide(incomingNumber);
                        }
                    } else {
                        if (mPrefs.getBoolean(mOutgoingKey, false)) {
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

            if (DEBUG) {
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
                mIgnoreContact = mPrefs.getBoolean(
                        context.getString(R.string.ignore_known_contact_key), false);


                if (mIgnoreContact && Utils.isContactExists(context, incomingNumber)) {
                    mIsInContacts = true;
                    if (mPrefs.getBoolean(context.getString(R.string.contact_offline_key),
                            false)) {
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

                final boolean hangup =
                        mPrefs.getBoolean(context.getString(R.string.auto_hangup_key), false);
                final boolean saveLog =
                        mPrefs.getBoolean(context.getString(R.string.add_call_log_key), false);

                List<Caller> callers = Caller.find(Caller.class, "number=?", incomingNumber);

                if (callers.size() > 0) {
                    Caller caller = callers.get(0);
                    if (!caller.needUpdate()) {
                        if (hangup || saveLog) {
                            bindPluginService(hangup, caller);
                        }
                        Utils.showWindow(context, caller, FloatWindow.CALLER_FRONT);
                        return;
                    } else {
                        caller.delete();
                    }
                }

                new PhoneNumber(context, mShowContactOffline, new PhoneNumber.Callback() {
                    @Override
                    public void onResponseOffline(INumber number) {
                        if (isShowing && number != null) {
                            Utils.showWindow(context, number, FloatWindow.CALLER_FRONT);
                        }
                    }

                    @Override
                    public void onResponse(INumber number) {
                        if (isShowing && number != null) {
                            new Caller(number, !number.isOnline()).save();
                            if (hangup || saveLog) {
                                bindPluginService(hangup, number);
                            }
                            Utils.showWindow(context, number, FloatWindow.CALLER_FRONT);
                        }
                    }

                    @Override
                    public void onResponseFailed(INumber number, boolean isOnline) {
                        if (isOnline) {
                            Utils.sendData(context, FloatWindow.WINDOW_ERROR,
                                    R.string.online_failed, FloatWindow.CALLER_FRONT);
                        } else {
                            Utils.showTextWindow(context, R.string.offline_failed,
                                    FloatWindow.CALLER_FRONT);
                        }
                    }
                }).fetch(incomingNumber);
            }
        }

        void hide(String incomingNumber) {
            Log.d(TAG, "hide");
            if (isShowing) {
                StandOutWindow.hide(context, FloatWindow.class, FloatWindow.CALLER_FRONT);
            }
        }

        void close(String incomingNumber) {
            Log.d(TAG, "ringStartTime:" + ringStartTime +
                    ", ringTime: " + ringTime + ", duration: " + duration);

            if (DEBUG) {
                Log.d(TAG, "close window: " + TextUtils.isEmpty(incomingNumber));
            }

            if (TextUtils.isEmpty(incomingNumber) && duration == -1) {
                return;
            }

            boolean saveLog =
                    mPrefs.getBoolean(context.getString(R.string.add_call_log_key), false);
            if (ringStartTime != -1 && !TextUtils.isEmpty(mIncomingNumber) && !mIsInContacts) {
                new InCall(mIncomingNumber, ringStartTime, ringTime, duration).save();
                mIncomingNumber = null;

                if (ringTime < 3000 && duration <= 0) {
                    saveLog = true;
                    if (TextUtils.isEmpty(mLogName)) {
                        mLogName = "";
                    }
                    if (mAutoHangup) {
                        mLogName += " " + context.getString(R.string.auto_hangup);
                    } else {
                        mLogName += " " + context.getString(R.string.ring_once);
                    }
                }
            }

            if (isShowing) {
                isShowing = false;
                StandOutWindow.closeAll(context, FloatWindow.class);
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

        private void bindPluginService(final boolean hangup, final INumber number) {

            mLogNumber = number.getNumber();
            mLogName = number.getName();
            mLogGeo = number.getProvince() + " " + number.getCity();
            mAutoHangup = false;

            if (mConnection == null) {
                mConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.d(TAG, "onServiceConnected: " + name.toString());
                        mPluginService = IPluginService.Stub.asInterface(service);
                        try {
                            if (hangup) {
                                String keywords = mPrefs.getString(mKeywordKey, mKeywordDefault);
                                keywords = keywords.trim();
                                if (keywords.isEmpty()) {
                                    keywords = mKeywordDefault;
                                }
                                for (String keyword : keywords.split(" ")) {
                                    if (!TextUtils.isEmpty(mLogName) &&
                                            mLogName.contains(keyword)) {
                                        mPluginService.hangUpPhoneCall();
                                        mAutoHangup = true;
                                    }
                                }

                                String geoKeywords = mPrefs.getString(mGeoKeywordKey, "");
                                geoKeywords = geoKeywords.trim();
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

                                String numberKeywords = mPrefs.getString(mNumberKeywordKey, "");
                                numberKeywords = numberKeywords.trim();
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

            if (mPluginIntent == null) {
                mPluginIntent = new Intent().setComponent(new ComponentName(
                        "org.xdty.callerinfo.plugin",
                        "org.xdty.callerinfo.plugin.PluginService"));
            }

            context.startService(mPluginIntent);
            context.getApplicationContext().bindService(mPluginIntent, mConnection,
                    Context.BIND_AUTO_CREATE);
        }

        private void unBindPluginService() {
            context.getApplicationContext().unbindService(mConnection);
            context.stopService(mPluginIntent);
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
    }
}
