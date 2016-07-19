package org.xdty.callerinfo.application;

import org.xdty.callerinfo.AppComponent;
import org.xdty.callerinfo.AppModule;
import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.DaggerAppComponent;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.receiver.IncomingCall.IncomingCallListener;
import org.xdty.callerinfo.utils.AlarmUtils;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.PhoneNumber;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class Application extends com.orm.SugarApp {
    public final static String TAG = Application.class.getSimpleName();

    private static AppComponent sAppComponent;

    public static AppComponent getAppComponent() {
        return sAppComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();

        SettingImpl.init(this);
        PhoneNumber.init(this);
        IncomingCallListener.init(this);
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
