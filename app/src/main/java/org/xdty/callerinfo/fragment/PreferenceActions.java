package org.xdty.callerinfo.fragment;

import android.preference.Preference;

public interface PreferenceActions {
    Preference findPreference(CharSequence key);

    void setChecked(int key, boolean checked);

    void onConfirmed(int key);

    void onConfirmCanceled(int key);

    void checkOfflineData();

    void resetOfflineDataUpgradeWorker();

    void addPreference(int parent, int child);

    void removePreference(int parent, int child);

    int getKeyId(String key);
}
