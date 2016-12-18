package org.xdty.callerinfo.application;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.di.AppComponent;
import org.xdty.callerinfo.di.DaggerAppComponent;
import org.xdty.callerinfo.di.modules.AppModule;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.receiver.IncomingCall.IncomingCallListener;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Utils;

import javax.inject.Inject;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class Application extends android.app.Application {
    public final static String TAG = Application.class.getSimpleName();

    private static AppComponent sAppComponent;

    private static Application sApplication;

    @Inject
    Setting mSetting;

    @Inject
    Alarm mAlarm;

    public static AppComponent getAppComponent() {
        return sAppComponent;
    }

    public static Application getApplication() {
        return sApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sApplication = this;

        sAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
        sAppComponent.inject(this);

        IncomingCallListener.init(this);
        Utils.checkLocale(getApplicationContext());

        if (mSetting.isCatchCrash() || BuildConfig.DEBUG) {
            CustomActivityOnCrash.install(this);
        }

        mSetting.fix();

        mAlarm.alarm();
    }
}
