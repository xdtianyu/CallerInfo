package org.xdty.callerinfo.model.permission

import android.content.Context

interface Permission {
    fun canDrawOverlays(): Boolean
    fun requestDrawOverlays(context: Context, requestCode: Int)
    fun checkPermission(permission: String): Int
    fun requestPermissions(context: Context, permissions: Array<String>, requestCode: Int)
    fun canReadPhoneState(): Boolean
    fun canReadContact(): Boolean
}