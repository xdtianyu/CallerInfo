package org.xdty.callerinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class IncomingCall extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        IncomingCallListener incomingCallListener = new IncomingCallListener(context);
        telephonyManager.listen(incomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
}
