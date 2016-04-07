package org.xdty.callerinfo.model.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;

public class SettingImpl implements Setting {

    private SharedPreferences mPrefs;
    private Context mContext;

    public SettingImpl(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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

    @Override
    public String getIgnoreRegex() {
        return mPrefs.getString(mContext.getString(R.string.ignore_regex_key), "").replace("*",
                "[0-9]").replace(" ", "|");
    }

    @Override
    public boolean isHidingOffHook() {
        return mPrefs.getBoolean(mContext.getString(R.string.hide_when_off_hook_key), false);
    }

    @Override
    public boolean isShowingOnOutgoing() {
        return mPrefs.getBoolean(mContext.getString(R.string.display_on_outgoing_key), false);
    }

    @Override
    public boolean isIgnoreKnownContact() {
        return mPrefs.getBoolean(mContext.getString(R.string.ignore_known_contact_key), false);
    }

    @Override
    public boolean isShowingContactOffline() {
        return mPrefs.getBoolean(mContext.getString(R.string.contact_offline_key), false);
    }

    @Override
    public boolean isAutoHangup() {
        return mPrefs.getBoolean(mContext.getString(R.string.auto_hangup_key), false);
    }

    @Override
    public boolean isAddingCallLog() {
        return mPrefs.getBoolean(mContext.getString(R.string.add_call_log_key), false);
    }

    @Override
    public String getKeywords() {
        String keywordKey = mContext.getString(R.string.hangup_keyword_key);
        String keywordDefault = mContext.getString(R.string.hangup_keyword_default);
        String keywords = mPrefs.getString(keywordKey, keywordDefault).trim();
        if (keywords.isEmpty()) {
            keywords = keywordDefault;
        }
        return keywords;
    }

    @Override
    public String getGeoKeyword() {
        return mPrefs.getString(mContext.getString(R.string.hangup_geo_keyword_key), "").trim();
    }

    @Override
    public String getNumberKeyword() {
        return mPrefs.getString(mContext.getString(R.string.hangup_number_keyword_key), "").trim();
    }
}
