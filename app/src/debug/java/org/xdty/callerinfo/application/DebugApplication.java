package org.xdty.callerinfo.application;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.facebook.stetho.Stetho;

import org.xdty.callerinfo.R;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class DebugApplication extends com.orm.SugarApp {
    public final static String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                        .build());
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isCatch = pref.getBoolean(getString(R.string.catch_crash_key), false);
        if (isCatch) {
            CustomActivityOnCrash.install(this);
        }
    }
}
