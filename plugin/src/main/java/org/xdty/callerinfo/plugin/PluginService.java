package org.xdty.callerinfo.plugin;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class PluginService extends Service {
    private static final String TAG = PluginService.class.getSimpleName();
    private IPluginServiceCallback mCallback;

    private final IPluginService.Stub mBinder = new IPluginService.Stub() {

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
                int res = checkSelfPermission(Manifest.permission.CALL_PHONE);
                if (res != PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(PluginService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("type", MainActivity.REQUEST_CODE_CALL_LOG_PERMISSION);
                    startActivity(intent);
                }
            }
        }

        @Override
        public void hangUpPhoneCall() throws RemoteException {
            Log.d(TAG, "hangUpPhoneCall: ");
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
}
