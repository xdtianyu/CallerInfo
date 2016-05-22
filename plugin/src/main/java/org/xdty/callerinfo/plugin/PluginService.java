package org.xdty.callerinfo.plugin;

import android.Manifest;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
                            CallLog.Calls.NUMBER + "=?", new String[] { mNumber });
                }
            }, 500);

        }

        @Override
        public void registerCallback(IPluginServiceCallback callback) throws RemoteException {
            Log.d(TAG, "registerCallback: ");
            mCallback = callback;
        }

        @Override
        public String exportData(String data) throws RemoteException {
            String filename = "CallerInfo.json";
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            String res = file.getAbsolutePath();
            Log.e(TAG, "export to: " + res);
            if (isExternalStorageWritable()) {
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    outputStream.write(data.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                    res = "Error: " + e.getMessage();
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                Log.e(TAG, "external storage is not mounted!!");
                res = "Error: external storage is not mounted!!";
            }
            return res;
        }

        @Override
        public String importData() throws RemoteException {
            String filename = "CallerInfo.json";
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            String res = file.getAbsolutePath();
            Log.e(TAG, "import from: " + res);
            if (isExternalStorageWritable()) {
                if (file.exists()) {
                    FileInputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(file);
                        StringBuilder fileContent = new StringBuilder("");

                        byte[] buffer = new byte[1024];
                        int n;

                        while ((n = inputStream.read(buffer)) != -1) {
                            fileContent.append(new String(buffer, 0, n));
                        }
                        res = fileContent.toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                        res = "Error: " + e.getMessage();
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    res = "Error: " + res + " not exist.";
                }

            } else {
                Log.e(TAG, "external storage is not mounted!!");
                res = "Error: external storage is not mounted!!";
            }
            return res;
        }

        @Override
        public void checkStoragePermission() throws RemoteException {
            Log.d(TAG, "checkWritePermission");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int res = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                int res2 = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
                if (res != PackageManager.PERMISSION_GRANTED ||
                        res2 != PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(PluginService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("type", MainActivity.REQUEST_CODE_STORAGE_PERMISSION);
                    startActivity(intent);
                } else {
                    mCallback.onStoragePermissionResult(true);
                }
            } else {
                mCallback.onStoragePermissionResult(true);
            }
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
                    case MainActivity.REQUEST_CODE_STORAGE_PERMISSION:
                        mCallback.onStoragePermissionResult(result);
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

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
