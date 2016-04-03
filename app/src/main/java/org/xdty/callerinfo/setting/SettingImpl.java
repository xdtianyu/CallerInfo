package org.xdty.callerinfo.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.xdty.callerinfo.BuildConfig;

public class SettingImpl implements Setting {

    private SharedPreferences mPrefs;

    public SettingImpl(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public boolean isEulaSet() {
        return mPrefs.getBoolean("eula", false);
    }

    @Override
    public void setEula() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean("eula", true);
        editor.putInt("eula_version", 1);
        editor.putInt("version", BuildConfig.VERSION_CODE);
        editor.apply();
    }
}
