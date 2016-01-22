package org.xdty.callerinfo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import java.util.List;

import wei.mark.standout.StandOutWindow;

public class IncomingCall extends BroadcastReceiver {

    public final static String TAG = IncomingCall.class.getSimpleName();

    private static IncomingCallListener mIncomingCallListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mIncomingCallListener == null) {
            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            mIncomingCallListener = new IncomingCallListener(context);
            telephonyManager.listen(mIncomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    public static class IncomingCallListener extends PhoneStateListener {

        public final static String TAG = IncomingCallListener.class.getSimpleName();
        private final boolean DEBUG = BuildConfig.DEBUG;
        private Context context;
        private boolean isShowing = false;
        private long ringStartTime = -1;
        private long hookStartTime = -1;
        private long idleStartTime = -1;
        private long ringTime = -1;
        private long duration = -1;
        private String mIncomingNumber = null;
        private SharedPreferences mPrefs;
        private boolean mIgnoreContact;
        private boolean mShowContactOffline = false;
        private boolean mIsInContacts = false;
        private String mOutgoingKey;
        private String mHideKey;

        public IncomingCallListener(Context context) {
            this.context = context;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            mOutgoingKey = context.getString(R.string.display_on_outgoing_key);
            mHideKey = context.getString(R.string.hide_when_off_hook_key);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if (!TextUtils.isEmpty(incomingNumber)) {
                incomingNumber = incomingNumber.replaceAll(" ", "");
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

                List<Caller> callers = Caller.find(Caller.class, "number=?", incomingNumber);

                if (callers.size() > 0) {
                    Caller caller = callers.get(0);
                    if (!caller.needUpdate()) {
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
                            Utils.showWindow(context, number, FloatWindow.CALLER_FRONT);
                        }
                    }

                    @Override
                    public void onResponseFailed(INumber number, boolean isOnline) {
                        if (isOnline) {
                            Utils.showTextWindow(context, R.string.online_failed,
                                    FloatWindow.CALLER_FRONT);
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

            if (ringStartTime != -1 && !TextUtils.isEmpty(mIncomingNumber) && !mIsInContacts) {
                new InCall(mIncomingNumber, ringStartTime, ringTime, duration).save();
                mIncomingNumber = null;
            }

            if (isShowing) {
                isShowing = false;
                StandOutWindow.closeAll(context, FloatWindow.class);
            }

            ringStartTime = -1;
            hookStartTime = -1;
            idleStartTime = -1;
            ringTime = -1;
            duration = -1;
        }
    }
}
