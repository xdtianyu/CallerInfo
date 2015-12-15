package org.xdty.callerinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.xdty.phone.number.PhoneNumber;

import wei.mark.standout.StandOutWindow;

public class IncomingCall extends BroadcastReceiver {

    public final static String TAG = IncomingCall.class.getSimpleName();

    public final static String LOCATION = "location";

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

        public String getMetadata(String name) {
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        context.getPackageName(), PackageManager.GET_META_DATA);
                if (appInfo.metaData != null) {
                    return appInfo.metaData.getString(name);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            return null;
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

        void show(final String incomingNumber) {

            if (!isShowing) {
                isShowing = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String API_KEY = getMetadata("org.xdty.phone.number.API_KEY");
                        PhoneNumber phoneNumber = PhoneNumber.key(API_KEY);
                        String text = phoneNumber.get(incomingNumber);
                        Bundle bundle = new Bundle();
                        bundle.putString(LOCATION, text);
                        StandOutWindow.show(context, FloatWindow.class, StandOutWindow.DEFAULT_ID);
                        StandOutWindow.sendData(context, FloatWindow.class,
                                StandOutWindow.DEFAULT_ID,
                                0, bundle, FloatWindow.class, 0);
                    }
                }).start();
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
