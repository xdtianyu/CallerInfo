package org.xdty.callerinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.NumberInfo;

import wei.mark.standout.StandOutWindow;

public class IncomingCall extends BroadcastReceiver {

    public final static String TAG = IncomingCall.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        IncomingCallListener incomingCallListener = new IncomingCallListener(context);
        telephonyManager.listen(incomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public static class IncomingCallListener extends PhoneStateListener {

        public final static String TAG = IncomingCallListener.class.getSimpleName();

        private Context context;

        private boolean isShowing = false;

        public IncomingCallListener(Context context) {
            this.context = context;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    show(incomingNumber);
                    Log.d(TAG, "CALL_STATE_RINGING");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(TAG, "CALL_STATE_OFFHOOK");
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.d(TAG, "CALL_STATE_IDLE");
                    close();
                    break;
            }
        }

        void show(String incomingNumber) {

            if (!isShowing) {
                isShowing = true;
                new PhoneNumber(context, new PhoneNumber.Callback() {
                    @Override
                    public void onResponse(NumberInfo numberInfo) {
                        if (isShowing && numberInfo != null) {
                            String text = numberInfo.toString();
                            Bundle bundle = new Bundle();
                            bundle.putString(FloatWindow.LOCATION, text);
                            StandOutWindow.show(context, FloatWindow.class,
                                    FloatWindow.CALLER_FRONT);
                            StandOutWindow.sendData(context, FloatWindow.class,
                                    FloatWindow.CALLER_FRONT, 0, bundle, FloatWindow.class, 0);
                        }
                    }

                    @Override
                    public void onResponseFailed(NumberInfo numberInfo) {

                    }
                }).fetch(incomingNumber);
            }
        }

        void close() {
            if (isShowing) {
                isShowing = false;
                StandOutWindow.closeAll(context, FloatWindow.class);
            }
        }
    }
}
