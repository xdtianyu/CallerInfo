package org.xdty.callerinfo.receiver;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.contract.PhoneStateContract;
import org.xdty.callerinfo.model.CallRecord;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.permission.PermissionImpl;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.presenter.PhoneStatePresenter;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.model.INumber;

import wei.mark.standout.StandOutWindow;

public class IncomingCall extends BroadcastReceiver {

    private final static String TAG = IncomingCall.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onReceive: " + intent.toString() + " " +
                    Utils.bundleToString(intent.getExtras()));
        }

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            IncomingCallListener.getInstance().setOutGoingNumber(
                    intent.getExtras().getString(Intent.EXTRA_PHONE_NUMBER));
        }
    }

    public static class IncomingCallListener extends PhoneStateListener implements
            PhoneStateContract.View {

        private static Context sContext;
        private boolean isShowing = false;

        private Setting mSetting;
        private Permission mPermission;
        private CallRecord mCallRecord;
        private PhoneStateContract.Presenter mPresenter;

        private IncomingCallListener() {
            mSetting = SettingImpl.getInstance();
            mPermission = new PermissionImpl(sContext);
            mCallRecord = new CallRecord();
            mPresenter = new PhoneStatePresenter(this, mSetting, mPermission, mCallRecord);
            Utils.checkLocale(sContext);
            mPresenter.start();
        }

        public static void init(Context context) {
            sContext = context.getApplicationContext();
            IncomingCallListener.getInstance();
            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(getInstance(), PhoneStateListener.LISTEN_CALL_STATE);
        }

        public static IncomingCallListener getInstance() {
            return SingletonHelper.INSTANCE;
        }

        private void setOutGoingNumber(String number) {
            mPresenter.setOutGoingNumber(number);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onCallStateChanged: " + state + " : " + incomingNumber);
            }

            if (mPresenter.matchIgnore(incomingNumber)) {
                return;
            }

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    mPresenter.handleRinging(incomingNumber);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mPresenter.handleOffHook(incomingNumber);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    mPresenter.handleIdle(incomingNumber);
                    break;
            }
        }

        @Override
        public void show(INumber number) {
            isShowing = true;
            Utils.showWindow(getContext(), number, FloatWindow.CALLER_FRONT);
        }

        @Override
        public void showFailed(boolean isOnline) {
            isShowing = true;
            if (isOnline) {
                Utils.sendData(getContext(), FloatWindow.WINDOW_ERROR,
                        R.string.online_failed, FloatWindow.CALLER_FRONT);
            } else {
                Utils.showTextWindow(getContext(), R.string.offline_failed,
                        FloatWindow.CALLER_FRONT);
            }
        }

        @Override
        public void showSearching() {
            Utils.showTextWindow(getContext(), R.string.searching,
                    FloatWindow.CALLER_FRONT);
        }

        @Override
        public void hide(String incomingNumber) {
            if (isShowing) {
                StandOutWindow.hide(sContext, FloatWindow.class, FloatWindow.CALLER_FRONT);
            }
        }

        @Override
        public void close(String incomingNumber) {
            if (isShowing) {
                isShowing = false;
                StandOutWindow.closeAll(sContext, FloatWindow.class);
            }
        }

        @Override
        public boolean isShowing() {
            return isShowing;
        }

        @Override
        public Context getContext() {
            return sContext.getApplicationContext();
        }

        @Override
        public void showMark(String number) {
            if (((KeyguardManager) sContext.getSystemService(
                    Context.KEYGUARD_SERVICE)).isKeyguardLocked()) {
                Utils.showMarkNotification(sContext, number);
            } else {
                Utils.startMarkActivity(sContext, number);
            }
        }

        private static class SingletonHelper {
            private final static IncomingCallListener INSTANCE = new IncomingCallListener();
        }
    }
}
