package org.xdty.callerinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.Location;
import org.xdty.phone.number.model.Number;
import org.xdty.phone.number.model.NumberInfo;

import java.util.List;

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
                            List<Number> numbers = numberInfo.getNumbers();
                            if (numbers.size() > 0) {
                                Number number = numbers.get(0);
                                Location location = number.getLocation();

                                String province = "";
                                String city = "";
                                String operators = "";
                                if (location != null) {
                                    province = location.getProvince();
                                    city = location.getCity();
                                    operators = location.getOperators();
                                }

                                String text = "";
                                int color = android.R.color.holo_green_dark;

                                switch (number.getType()) {
                                    case NORMAL:
                                        text = context.getResources().getString(
                                                R.string.text_normal, province, city, operators);
                                        break;
                                    case POI:
                                        color = android.R.color.holo_blue_light;
                                        text = context.getResources().getString(
                                                R.string.text_poi, number.getName());
                                        break;
                                    case REPORT:
                                        color = android.R.color.holo_red_light;
                                        text = context.getResources().getString(
                                                R.string.text_report, province, city, operators,
                                                number.getCount(), number.getName());
                                        break;
                                }

                                Bundle bundle = new Bundle();
                                bundle.putString(FloatWindow.NUMBER_INFO, text);
                                bundle.putInt(FloatWindow.WINDOW_COLOR, color);
                                StandOutWindow.show(context, FloatWindow.class,
                                        FloatWindow.CALLER_FRONT);
                                StandOutWindow.sendData(context, FloatWindow.class,
                                        FloatWindow.CALLER_FRONT, 0, bundle, FloatWindow.class, 0);
                            }
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
