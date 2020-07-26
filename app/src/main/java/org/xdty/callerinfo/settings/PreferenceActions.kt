package org.xdty.callerinfo.settings

import android.preference.Preference

interface PreferenceActions {
    fun findPreference(key: CharSequence?): Preference?
    fun setChecked(key: Int, checked: Boolean)
    fun onConfirmed(key: Int)
    fun onConfirmCanceled(key: Int)
    fun checkOfflineData()
    fun resetOfflineDataUpgradeWorker()
    fun addPreference(parent: Int, child: Int)
    fun removePreference(parent: Int, child: Int)
    fun getKeyId(key: String?): Int
}