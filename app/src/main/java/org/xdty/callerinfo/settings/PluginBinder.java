package org.xdty.callerinfo.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceScreen;
import android.util.Log;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.exporter.Exporter;
import org.xdty.callerinfo.plugin.IPluginService;
import org.xdty.callerinfo.plugin.IPluginServiceCallback;
import org.xdty.callerinfo.utils.Toasts;

import io.reactivex.functions.Consumer;

public class PluginBinder implements ServiceConnection {

    private static final String TAG = "PluginBinder";

    private final Intent mPluginIntent = new Intent().setComponent(new ComponentName(
            "org.xdty.callerinfo.plugin",
            "org.xdty.callerinfo.plugin.PluginService"));

    private Context context;
    private IPluginService mPluginService;
    private PreferenceDialogs preferenceDialogs;
    private PreferenceActions preferenceActions;

    public PluginBinder(Context context, PreferenceDialogs preferenceDialogs, PreferenceActions preferenceActions) {
        this.context = context;
        this.preferenceDialogs = preferenceDialogs;
        this.preferenceActions = preferenceActions;
    }

    public void bindPluginService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(mPluginIntent);
            } else {
                context.startService(mPluginIntent);
            }
            context.bindService(mPluginIntent, this, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unBindPluginService() {
        try {
            context.unbindService(this);
            context.stopService(mPluginIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected: " + name.toString());
        mPluginService = IPluginService.Stub.asInterface(service);
        try {
            mPluginService.registerCallback(new IPluginServiceCallback.Stub() {
                @Override
                public void onCallPermissionResult(final boolean success) throws
                        RemoteException {
                    Log.d(TAG, "onCallPermissionResult: " + success);
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            preferenceActions.setChecked(R.string.auto_hangup_key, success);
                        }
                    });
                }

                @Override
                public void onCallLogPermissionResult(final boolean success) throws
                        RemoteException {
                    Log.d(TAG, "onCallLogPermissionResult: " + success);
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (PluginStatus.isCheckRingOnce) {
                                preferenceActions.setChecked(R.string.ring_once_and_auto_hangup_key, success);
                            } else {
                                preferenceActions.setChecked(R.string.add_call_log_key, success);
                            }
                        }
                    });
                }

                @Override
                public void onStoragePermissionResult(boolean success) throws RemoteException {
                    Log.d(TAG, "onStoragePermissionResult: " + success);
                    if (success) {
                        if (PluginStatus.isCheckStorageExport) {
                            exportData();
                        } else {
                            importData();
                        }
                    } else {
                        Toasts.INSTANCE.show(context, R.string.storage_permission_failed);
                    }
                }
            });
            enablePluginPreference();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enablePluginPreference() {
        PreferenceScreen pluginPref =
                (PreferenceScreen) preferenceActions.findPreference(context.getString(R.string.plugin_key));
        pluginPref.setEnabled(true);
        pluginPref.setSummary("");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "onServiceDisconnected: " + name.toString());
        mPluginService = null;
    }

    public void checkStoragePermission() {
        try {
            if (mPluginService != null) {
                mPluginService.checkStoragePermission();
            } else {
                Log.e(TAG, "PluginService is stopped!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("CheckResult")
    private void importData() {
        try {
            if (mPluginService != null) {
                String data = mPluginService.importData();
                if (data.contains("Error:")) {
                    preferenceDialogs.showTextDialog(R.string.import_data,
                            context.getString(R.string.import_failed, data));
                } else {
                    Exporter exporter = new Exporter(context);
                    exporter.fromString(data).subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            if (s == null) {
                                preferenceDialogs.showTextDialog(R.string.import_data,
                                        R.string.import_succeed);
                            } else {
                                preferenceDialogs.showTextDialog(R.string.import_data,
                                        context.getString(R.string.import_failed, s));
                            }
                        }
                    });
                }
            } else {
                Log.e(TAG, "PluginService is stopped!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("CheckResult")
    private void exportData() {
        Exporter exporter = new Exporter(context);
        exporter.export().subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) {
                try {
                    String res = mPluginService.exportData(s);
                    if (res.contains("Error")) {
                        preferenceDialogs.showTextDialog(R.string.export_data,
                                context.getString(R.string.export_failed, res));
                    } else {
                        preferenceDialogs.showTextDialog(R.string.export_data,
                                context.getString(R.string.export_succeed, res));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void checkCallPermission() {
        try {
            mPluginService.checkCallPermission();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkCallLogPermission() {
        try {
            mPluginService.checkCallLogPermission();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setIconStatus(boolean show) {
        try {
            mPluginService.setIconStatus(show);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}