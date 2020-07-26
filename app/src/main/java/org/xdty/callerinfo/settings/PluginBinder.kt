package org.xdty.callerinfo.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.preference.PreferenceScreen
import android.util.Log
import org.xdty.callerinfo.R
import org.xdty.callerinfo.exporter.Exporter
import org.xdty.callerinfo.plugin.IPluginService
import org.xdty.callerinfo.plugin.IPluginServiceCallback
import org.xdty.callerinfo.utils.Toasts.show

class PluginBinder(private val context: Context, private val preferenceDialogs: PreferenceDialogs,
                   private val preferenceActions: PreferenceActions) : ServiceConnection {

    private val mPluginIntent = Intent().setComponent(ComponentName(
            "org.xdty.callerinfo.plugin",
            "org.xdty.callerinfo.plugin.PluginService"))

    private var mPluginService: IPluginService? = null

    fun bindPluginService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(mPluginIntent)
            } else {
                context.startService(mPluginIntent)
            }
            context.bindService(mPluginIntent, this, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unBindPluginService() {
        try {
            context.unbindService(this)
            context.stopService(mPluginIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        Log.d(TAG, "onServiceConnected: $name")
        mPluginService = IPluginService.Stub.asInterface(service)
        try {
            mPluginService?.registerCallback(object : IPluginServiceCallback.Stub() {
                @Throws(RemoteException::class)
                override fun onCallPermissionResult(success: Boolean) {
                    Log.d(TAG, "onCallPermissionResult: $success")
                    (context as Activity).runOnUiThread { preferenceActions.setChecked(R.string.auto_hangup_key, success) }
                }

                @Throws(RemoteException::class)
                override fun onCallLogPermissionResult(success: Boolean) {
                    Log.d(TAG, "onCallLogPermissionResult: $success")
                    (context as Activity).runOnUiThread {
                        if (PluginStatus.isCheckRingOnce) {
                            preferenceActions.setChecked(R.string.ring_once_and_auto_hangup_key, success)
                        } else {
                            preferenceActions.setChecked(R.string.add_call_log_key, success)
                        }
                    }
                }

                @Throws(RemoteException::class)
                override fun onStoragePermissionResult(success: Boolean) {
                    Log.d(TAG, "onStoragePermissionResult: $success")
                    if (success) {
                        if (PluginStatus.isCheckStorageExport) {
                            exportData()
                        } else {
                            importData()
                        }
                    } else {
                        show(context, R.string.storage_permission_failed)
                    }
                }
            })
            enablePluginPreference()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun enablePluginPreference() {
        val pluginPref = preferenceActions.findPreference(context.getString(R.string.plugin_key)) as PreferenceScreen?
        pluginPref!!.isEnabled = true
        pluginPref.summary = ""
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Log.d(TAG, "onServiceDisconnected: $name")
        mPluginService = null
    }

    fun checkStoragePermission() {
        try {
            if (mPluginService != null) {
                mPluginService!!.checkStoragePermission()
            } else {
                Log.e(TAG, "PluginService is stopped!!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("CheckResult")
    private fun importData() {
        try {
            if (mPluginService != null) {
                val data = mPluginService!!.importData()
                if (data.contains("Error:")) {
                    preferenceDialogs.showTextDialog(R.string.import_data,
                            context.getString(R.string.import_failed, data))
                } else {
                    val exporter = Exporter(context)
                    exporter.fromString(data).subscribe { s ->
                        if (s == null) {
                            preferenceDialogs.showTextDialog(R.string.import_data,
                                    R.string.import_succeed)
                        } else {
                            preferenceDialogs.showTextDialog(R.string.import_data,
                                    context.getString(R.string.import_failed, s))
                        }
                    }
                }
            } else {
                Log.e(TAG, "PluginService is stopped!!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("CheckResult")
    private fun exportData() {
        val exporter = Exporter(context)
        exporter.export().subscribe { s ->
            try {
                val res = mPluginService!!.exportData(s)
                if (res.contains("Error")) {
                    preferenceDialogs.showTextDialog(R.string.export_data,
                            context.getString(R.string.export_failed, res))
                } else {
                    preferenceDialogs.showTextDialog(R.string.export_data,
                            context.getString(R.string.export_succeed, res))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun checkCallPermission() {
        try {
            mPluginService?.checkCallPermission()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkCallLogPermission() {
        try {
            mPluginService?.checkCallLogPermission()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setIconStatus(show: Boolean) {
        try {
            mPluginService?.setIconStatus(show)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "PluginBinder"
    }
}