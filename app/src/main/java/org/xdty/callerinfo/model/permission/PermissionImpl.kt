package org.xdty.callerinfo.model.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

class PermissionImpl(private val mContext: Context) : Permission {
    override fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            true
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            Settings.canDrawOverlays(mContext)
        } else {
            if (Settings.canDrawOverlays(mContext)) {
                return true
            }
            try {
                val mgr = mContext.getSystemService(
                        Context.WINDOW_SERVICE) as WindowManager
                        ?: return false //getSystemService might return null
                val viewToAdd = View(mContext)
                val params = LayoutParams(0, 0,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LayoutParams.TYPE_APPLICATION_OVERLAY else LayoutParams.TYPE_SYSTEM_ALERT, LayoutParams.FLAG_NOT_TOUCHABLE
                        or LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSPARENT)
                viewToAdd.layoutParams = params
                mgr.addView(viewToAdd, params)
                mgr.removeView(viewToAdd)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            false
        }
    }

    override fun requestDrawOverlays(context: Context, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.packageName))
            if (context is Activity) {
                context.startActivityForResult(intent, requestCode)
            }
        }
    }

    override fun checkPermission(permission: String): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mContext.checkSelfPermission(permission)
        } else PackageManager.PERMISSION_GRANTED
    }

    override fun requestPermissions(context: Context, permissions: Array<String>,
                                    requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context is Activity) {
                context.requestPermissions(permissions, requestCode)
            }
        }
    }

    override fun canReadPhoneState(): Boolean {
        return checkPermission(
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    override fun canReadContact(): Boolean {
        return checkPermission(
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

}