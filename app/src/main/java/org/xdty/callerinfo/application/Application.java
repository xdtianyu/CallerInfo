package org.xdty.callerinfo.application;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.tencent.bugly.crashreport.CrashReport;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.di.AppComponent;
import org.xdty.callerinfo.di.DaggerAppComponent;
import org.xdty.callerinfo.di.modules.AppModule;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Resource;
import org.xdty.callerinfo.utils.Utils;

import javax.inject.Inject;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class Application extends android.app.Application {
    public final static String TAG = Application.class.getSimpleName();

    private static AppComponent sAppComponent;

    private static Application sApplication;

    private FirebaseAnalytics mAnalytics;

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

        mAnalytics = FirebaseAnalytics.getInstance(this);
        mAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null);

        Resource.getInstance().init(Utils.changeLang(this));

        if (mSetting.isCatchCrash() || BuildConfig.DEBUG) {
            CustomActivityOnCrash.install(this);
        } else {
            CrashReport.initCrashReport(getApplicationContext(), "0eaf845a04", false);
        }

        mSetting.fix();

        mAlarm.alarm();
    }
}
