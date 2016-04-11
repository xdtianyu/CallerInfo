package org.xdty.callerinfo.application;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.utils.Utils;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class Application extends com.orm.SugarApp {
    public final static String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.checkLocale(getApplicationContext());
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isCatch = pref.getBoolean(getString(R.string.catch_crash_key), false);
        if (isCatch) {
            CustomActivityOnCrash.install(this);
        }
    }
}
