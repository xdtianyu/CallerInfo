package org.xdty.callerinfo.application;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.utils.AlarmUtils;
import org.xdty.callerinfo.utils.Utils;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class Application extends com.orm.SugarApp {
    public final static String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        SettingImpl.init(this);
        Utils.checkLocale(getApplicationContext());
        Setting setting = SettingImpl.getInstance();

        if (setting.isCatchCrash() || BuildConfig.DEBUG) {
            CustomActivityOnCrash.install(this);
        }

        if (setting.isAutoReportEnabled() || setting.isMarkingEnabled()) {
            AlarmUtils.install(this);
            AlarmUtils.alarm();
        }
    }
}
