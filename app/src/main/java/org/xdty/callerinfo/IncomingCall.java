package org.xdty.callerinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.xdty.callerinfo.Utils.Utils;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.phone.number.PhoneNumber;
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

        private long ringStartTime = -1;
        private long hookStartTime = -1;
        private long idleStartTime = -1;

        private long lastInCallSaveTime = -1;
        private long lastCallerSaveTime = -1;

        public IncomingCallListener(Context context) {
            this.context = context;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    show(incomingNumber);
                    Log.d(TAG, "CALL_STATE_RINGING");
                    ringStartTime = System.currentTimeMillis();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(TAG, "CALL_STATE_OFFHOOK");
                    hookStartTime = System.currentTimeMillis();
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.d(TAG, "CALL_STATE_IDLE");
                    idleStartTime = System.currentTimeMillis();
                    close(incomingNumber);
                    break;
            }
        }

        void show(String incomingNumber) {

            if (!isShowing) {
                isShowing = true;

                List<Caller> callers = Caller.find(Caller.class, "number=?", incomingNumber);

                if (callers.size() > 0) {
                    Caller caller = callers.get(0);
                    if (caller.getLastUpdate() - System.currentTimeMillis() < 7 * 24 * 3600 * 1000) {
                        Utils.showWindow(context, caller);
                        return;
                    } else {
                        caller.delete();
                    }
                }

                new PhoneNumber(context, new PhoneNumber.Callback() {
                    @Override
                    public void onResponse(NumberInfo numberInfo) {
                        if (isShowing && numberInfo != null) {
                            List<Number> numbers = numberInfo.getNumbers();
                            if (numbers.size() > 0) {
                                Number number = numbers.get(0);

                                if (System.currentTimeMillis() - lastCallerSaveTime > 10000) {
                                    new Caller(number).save();
                                    lastCallerSaveTime = System.currentTimeMillis();
                                }

                                Utils.showWindow(context, number);
                            }
                        }
                    }

                    @Override
                    public void onResponseFailed(NumberInfo numberInfo) {

                    }
                }).fetch(incomingNumber);
            }
        }

        void close(String incomingNumber) {

            long ringTime = hookStartTime - ringStartTime;
            long duration = idleStartTime - hookStartTime;
            long time = ringStartTime;

            if (System.currentTimeMillis() - lastInCallSaveTime > 10000) {
                new InCall(incomingNumber, time, ringTime, duration).save();
                lastInCallSaveTime = System.currentTimeMillis();
            }

            if (isShowing) {
                isShowing = false;
                StandOutWindow.closeAll(context, FloatWindow.class);
            }
        }
    }
}
