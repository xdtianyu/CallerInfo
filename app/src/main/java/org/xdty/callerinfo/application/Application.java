package org.xdty.callerinfo.application;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.di.AppComponent;
import org.xdty.callerinfo.di.DaggerAppComponent;
import org.xdty.callerinfo.di.modules.AppModule;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.receiver.IncomingCall.IncomingCallListener;
import org.xdty.callerinfo.utils.AlarmUtils;
import org.xdty.callerinfo.utils.Utils;

import javax.inject.Inject;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class Application extends com.orm.SugarApp {
    public final static String TAG = Application.class.getSimpleName();

    private static AppComponent sAppComponent;

    @Inject
    Setting mSetting;

    public static AppComponent getAppComponent() {
        return sAppComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
        sAppComponent.inject(this);

        IncomingCallListener.init(this);
        Utils.checkLocale(getApplicationContext());

        if (mSetting.isCatchCrash() || BuildConfig.DEBUG) {
            CustomActivityOnCrash.install(this);
        }

        if (mSetting.isAutoReportEnabled() || mSetting.isMarkingEnabled()) {
            AlarmUtils.install(this);
            AlarmUtils.alarm();
        }
    }
}
