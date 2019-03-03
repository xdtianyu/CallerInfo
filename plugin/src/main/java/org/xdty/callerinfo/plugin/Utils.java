package org.xdty.callerinfo.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public class Utils {

    static void hideIcon(Context context) {
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context,
                context.getPackageName() + ".Launcher");
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    static void showIcon(Context context) {
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context,
                context.getPackageName() + ".Launcher");
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
}
