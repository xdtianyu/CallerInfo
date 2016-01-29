package org.xdty.callerinfo.plugin;

import android.Manifest;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;

public class PluginService extends Service {
    private static final String TAG = PluginService.class.getSimpleName();
    private IPluginServiceCallback mCallback;

    private Handler mHandler = new Handler();

    private final IPluginService.Stub mBinder = new IPluginService.Stub() {

        private String mNumber;
        private String mName;

        @Override
        public void checkCallPermission() throws RemoteException {
            Log.d(TAG, "checkCallPermission");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int res = checkSelfPermission(Manifest.permission.CALL_PHONE);
                if (res != PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(PluginService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("type", MainActivity.REQUEST_CODE_CALL_PERMISSION);
                    startActivity(intent);
                }
            }
        }

        @Override
        public void checkCallLogPermission() throws RemoteException {
            Log.d(TAG, "checkCallLogPermission");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int res = checkSelfPermission(Manifest.permission.READ_CALL_LOG);
                int res2 = checkSelfPermission(Manifest.permission.WRITE_CALL_LOG);
                if (res != PackageManager.PERMISSION_GRANTED ||
                        res2 != PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(PluginService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("type", MainActivity.REQUEST_CODE_CALL_LOG_PERMISSION);
                    startActivity(intent);
                }
            }
        }

        @Override
        public void hangUpPhoneCall() throws RemoteException {
            Log.d(TAG, "hangUpPhoneCall: " + killPhoneCall());
        }

        @Override
        public void updateCallLog(String number, String name) throws RemoteException {
            Log.d(TAG, "updateCallLog: " + "name = [" + name + "]");
            mNumber = number;
            mName = name;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            checkSelfPermission(Manifest.permission.READ_CALL_LOG) !=
                                    PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "updateCallLog: have no READ_CALL_LOG permission");
                        return;
                    }
                    ContentValues content = new ContentValues();
                    content.put(CallLog.Calls.NUMBER, mName + " (" + mNumber + ")");
                    getContentResolver().update(CallLog.Calls.CONTENT_URI, content,
                            CallLog.Calls.NUMBER + "=?", new String[]{mNumber});
                }
            }, 500);

        }

        @Override
        public void registerCallback(IPluginServiceCallback callback) throws RemoteException {
            Log.d(TAG, "registerCallback: ");
            mCallback = callback;
        }
    };

    public PluginService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int res = super.onStartCommand(intent, flags, startId);
        if (mCallback != null) {
            int type = intent.getIntExtra("type", 0);
            boolean result = intent.getBooleanExtra("result", false);
            try {
                switch (type) {
                    case MainActivity.REQUEST_CODE_CALL_PERMISSION:
                        mCallback.onCallPermissionResult(result);
                        break;
                    case MainActivity.REQUEST_CODE_CALL_LOG_PERMISSION:
                        mCallback.onCallLogPermissionResult(result);
                        break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: " + intent.toString());
        return mBinder;
    }

    private boolean killPhoneCall() {
        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");
            methodGetITelephony.setAccessible(true);
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);
            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");
            methodEndCall.invoke(telephonyInterface);
        } catch (Exception e) {
            Log.d(TAG, "hangupPhoneCall" + e.toString());
            return false;
        }
        return true;
    }
}
