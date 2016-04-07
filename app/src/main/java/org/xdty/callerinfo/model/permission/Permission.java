package org.xdty.callerinfo.model.permission;

import android.support.annotation.NonNull;

public interface Permission {

    boolean canDrawOverlays();

    void requestDrawOverlays(int requestCode);

    int checkPermission(String permission);

    void requestPermissions(@NonNull String[] permissions, int requestCode);

    boolean canReadPhoneState();

    boolean canReadContact();

}
