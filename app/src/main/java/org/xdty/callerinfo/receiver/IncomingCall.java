package org.xdty.callerinfo.receiver;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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

public class IncomingCall extends BroadcastReceiver {

    private final static String TAG = IncomingCall.class.getSimpleName();

    private PhoneStateListener mPhoneStateListener;

    public IncomingCall() {
        mPhoneStateListener = PhoneStateListener.getInstance();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mPhoneStateListener.setContext(context);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onReceive: " + intent.toString() + " " +
                    Utils.bundleToString(intent.getExtras()));
        }

        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case Intent.ACTION_NEW_OUTGOING_CALL:
                    if (intent.getExtras() != null) {
                        mPhoneStateListener.setOutGoingNumber(
                                intent.getExtras().getString(Intent.EXTRA_PHONE_NUMBER));
                    }
                    break;
                case TelephonyManager.ACTION_PHONE_STATE_CHANGED:
                    if (intent.getExtras() != null) {
                        String state = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
                        String number = intent.getExtras()
                                .getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        mPhoneStateListener.onCallStateChanged(state, number);
                    }
                    break;
            }
        }

    }

    public final static class PhoneStateListener implements
            PhoneStateContract.View {

        @Inject
        PhoneStateContract.Presenter mPresenter;

        @Inject Window mWindow;

        private Context mContext;

        private PhoneStateListener() {
            try {

                DaggerPhoneStatusComponent.builder()
                        .appModule(new AppModule(Application.getApplication()))
                        .phoneStatusModule(new PhoneStatusModule(this))
                        .build()
                        .inject(this);
                mPresenter.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setContext(Context context) {
            mContext = context.getApplicationContext();
        }

        private void setOutGoingNumber(String number) {
            mPresenter.setOutGoingNumber(number);
            onCallStateChanged(TelephonyManager.EXTRA_STATE_OFFHOOK, number);
        }

        public void onCallStateChanged(int state, String number) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    onCallStateChanged(TelephonyManager.EXTRA_STATE_RINGING, number);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    onCallStateChanged(TelephonyManager.EXTRA_STATE_OFFHOOK, number);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    onCallStateChanged(TelephonyManager.EXTRA_STATE_IDLE, number);
                    break;
            }
        }

        public void onCallStateChanged(String state, String number) {

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onCallStateChanged: " + state + " : " + number);
                Log.d(TAG, "onCallStateChanged: permission -> " + mPresenter.canReadPhoneState());
            }

            if (mPresenter.matchIgnore(number)) {
                return;
            }

            switch (state) {
                case "RINGING":
                    mPresenter.handleRinging(number);
                    break;
                case "OFFHOOK":
                    mPresenter.handleOffHook(number);
                    break;
                case "IDLE":
                    mPresenter.handleIdle(number);
                    break;
            }
        }

        @Override
        public void show(INumber number) {
            mWindow.showWindow(number, Window.Type.CALLER);
        }

        @Override
        public void showFailed(boolean isOnline) {
            if (isOnline) {
                mWindow.sendData(FloatWindow.WINDOW_ERROR,
                        R.string.online_failed, Window.Type.CALLER);
            } else {
                mWindow.showTextWindow(R.string.offline_failed, Window.Type.CALLER);
            }
        }

        @Override
        public void showSearching() {
            mWindow.showTextWindow(R.string.searching, Window.Type.CALLER);
        }

        @Override
        public void hide(String incomingNumber) {
            mWindow.hideWindow();
        }

        @Override
        public void close(String incomingNumber) {
            mWindow.closeWindow();
        }

        @Override
        public boolean isShowing() {
            return mWindow.isShowing();
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public void showMark(String number) {

            KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(
                    Context.KEYGUARD_SERVICE);

            boolean isKeyguardLocked;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                isKeyguardLocked = keyguardManager.isKeyguardLocked();
            } else {
                isKeyguardLocked = keyguardManager.inKeyguardRestrictedInputMode();
            }

            if (isKeyguardLocked) {
                Utils.showMarkNotification(getContext(), number);
            } else {
                Utils.startMarkActivity(getContext(), number);
            }
        }

        public static PhoneStateListener getInstance() {
            return SingletonHelper.sINSTANCE;
        }

        private final static class SingletonHelper {
            @SuppressLint("StaticFieldLeak")
            private final static PhoneStateListener sINSTANCE = new PhoneStateListener();
        }
    }
}
