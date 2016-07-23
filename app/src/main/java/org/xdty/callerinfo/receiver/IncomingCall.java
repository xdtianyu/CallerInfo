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
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.contract.PhoneStateContract;
import org.xdty.callerinfo.di.DaggerPhoneStatusComponent;
import org.xdty.callerinfo.di.modules.AppModule;
import org.xdty.callerinfo.di.modules.PhoneStatusModule;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.callerinfo.utils.Window;
import org.xdty.phone.number.model.INumber;

import javax.inject.Inject;

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

        @Inject
        PhoneStateContract.Presenter mPresenter;

        @Inject Window mWindow;

        private boolean isShowing = false;

        private IncomingCallListener() {
            Utils.checkLocale(sContext);
            DaggerPhoneStatusComponent.builder()
                    .appModule(new AppModule(Application.getApplication()))
                    .phoneStatusModule(new PhoneStatusModule(this))
                    .build()
                    .inject(this);
            mPresenter.start();
        }

        public static void init(Context context) {
            sContext = context.getApplicationContext();

            TelephonyManager telephonyManager = (TelephonyManager) sContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(getInstance(), PhoneStateListener.LISTEN_CALL_STATE);
        }

        public static IncomingCallListener getInstance() {
            return SingletonHelper.INSTANCE;
        }

        private void setOutGoingNumber(String number) {
            mPresenter.setOutGoingNumber(number);
            onCallStateChanged(TelephonyManager.CALL_STATE_OFFHOOK, number);
        }

        @Override
        public void onCallStateChanged(int state, String number) {

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onCallStateChanged: " + state + " : " + number);
                Log.d(TAG, "onCallStateChanged: permission -> " + mPresenter.canReadPhoneState());
            }

            if (mPresenter.matchIgnore(number)) {
                return;
            }

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    mPresenter.handleRinging(number);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mPresenter.handleOffHook(number);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    mPresenter.handleIdle(number);
                    break;
            }
        }

        @Override
        public void show(INumber number) {
            isShowing = true;
            mWindow.showWindow(getContext(), number, FloatWindow.CALLER_FRONT);
        }

        @Override
        public void showFailed(boolean isOnline) {
            isShowing = true;
            if (isOnline) {
                mWindow.sendData(getContext(), FloatWindow.WINDOW_ERROR,
                        R.string.online_failed, FloatWindow.CALLER_FRONT);
            } else {
                mWindow.showTextWindow(getContext(), R.string.offline_failed,
                        FloatWindow.CALLER_FRONT);
            }
        }

        @Override
        public void showSearching() {
            mWindow.showTextWindow(getContext(), R.string.searching,
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
