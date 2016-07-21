package org.xdty.callerinfo.model.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;

public class PermissionImpl implements Permission {

    private Context mContext;

    public PermissionImpl(Context context) {
        mContext = context;
    }

    @Override
    public boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(mContext);
        }
        return true;
    }

    @Override
    public void requestDrawOverlays(Context context, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, requestCode);
            }
        }
    }

    @Override
    public int checkPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mContext.checkSelfPermission(permission);
        }
        return PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestPermissions(Context context, @NonNull String[] permissions,
            int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context instanceof Activity) {
                ((Activity) context).requestPermissions(
                        new String[] { Manifest.permission.READ_PHONE_STATE }, requestCode);
            }
        }
    }

    @Override
    public boolean canReadPhoneState() {
        return checkPermission(
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean canReadContact() {
        return checkPermission(
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }
}
