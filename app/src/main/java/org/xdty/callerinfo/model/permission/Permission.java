package org.xdty.callerinfo.model.permission;

import android.content.Context;
import android.support.annotation.NonNull;

public interface Permission {

    boolean canDrawOverlays();

    void requestDrawOverlays(Context context, int requestCode);

    int checkPermission(String permission);

    void requestPermissions(Context context, @NonNull String[] permissions, int requestCode);

    boolean canReadPhoneState();

    boolean canReadContact();

}
