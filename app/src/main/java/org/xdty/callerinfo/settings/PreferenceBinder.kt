package org.xdty.callerinfo.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.Preference
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceCategory
import android.preference.PreferenceScreen
import android.preference.SwitchPreference
import org.xdty.callerinfo.BuildConfig
import org.xdty.callerinfo.R
import org.xdty.callerinfo.model.setting.SettingImpl.Companion.instance
import org.xdty.callerinfo.utils.Utils.Companion.getDate
import org.xdty.callerinfo.utils.Utils.Companion.getVersionCode
import org.xdty.callerinfo.utils.Utils.Companion.isAppInstalled
import org.xdty.callerinfo.utils.Utils.Companion.isComponentEnabled
import org.xdty.callerinfo.utils.Utils.Companion.mask
import java.util.*

class PreferenceBinder(
        private val context: Context,
        var sharedPrefs: SharedPreferences,
        preferenceDialogs: PreferenceDialogs,
        private val pluginBinder: PluginBinder,
        private val preferenceActions: PreferenceActions
) : OnPreferenceClickListener {

    private val preferenceClicker = PreferenceClicker(context, sharedPrefs, preferenceDialogs,
            pluginBinder, preferenceActions)
    private val keyMap = HashMap<String, Int>()
    private val prefMap = HashMap<String, Preference?>()

    fun bind() {
        bindPreference(R.string.juhe_api_key, true)
        bindPreference(R.string.window_text_size_key)
        bindPreference(R.string.window_height_key)
        bindPreferenceList(R.string.window_text_alignment_key, R.array.align_type, 1)
        bindPreference(R.string.window_transparent_key)
        bindPreference(R.string.window_text_padding_key)
        bindPreferenceList(R.string.api_type_key, R.array.api_type, 1, 1)
        bindPreference(R.string.ignore_known_contact_key)
        bindPreference(R.string.display_on_outgoing_key)
        bindPreference(R.string.catch_crash_key)
        bindPreference(R.string.ignore_regex_key, false)
        bindPreference(R.string.custom_api_url)
        bindPreference(R.string.ignore_battery_optimizations_key)
        bindPreference(R.string.auto_report_key)
        bindPreference(R.string.enable_marking_key)
        bindPreference(R.string.not_mark_contact_key)
        bindPreference(R.string.temporary_disable_blacklist_key)
        bindPreference(R.string.outgoing_window_position_key)
        bindPreference(R.string.offline_data_auto_upgrade_key)
        bindPreference(R.string.offline_data_check_now_key)
        bindDataVersionPreference()
        bindVersionPreference()
        bindPluginPreference()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        return preferenceClicker.dispatchClick(preference)
    }

    fun findCachedPreference(key: CharSequence): Preference? {
        return prefMap[key.toString()]
    }

    private fun bindPreference(keyId: Int, mask: Boolean) {
        if (mask) {
            bindPreference(keyId, SUMMARY_FLAG_NORMAL or SUMMARY_FLAG_MASK, 0)
        } else {
            bindPreference(keyId, SUMMARY_FLAG_NORMAL, 0)
        }
    }

    private fun bindPreference(keyId: Int, summaryId: Int) {
        bindPreference(keyId, SUMMARY_FLAG_NORMAL, summaryId)
    }

    private fun bindPreferenceList(keyId: Int, arrayId: Int, index: Int) {
        bindPreferenceList(keyId, arrayId, index, 0)
    }

    private fun bindPreferenceList(keyId: Int, arrayId: Int, defValue: Int, offset: Int) {
        val key = context.getString(keyId)
        val preference = preferenceActions.findPreference(key)
        val apiList = listOf(*context.resources.getStringArray(arrayId))
        preference!!.onPreferenceClickListener = this
        preference.summary = apiList[sharedPrefs.getInt(key, defValue) - offset]
        keyMap[key] = keyId
        prefMap[key] = preference
    }

    private fun bindPreference(keyId: Int, summaryFlags: Int = SUMMARY_FLAG_NULL, summaryId: Int = 0) {
        val key = context.getString(keyId)
        val preference = preferenceActions.findPreference(key)
        preference!!.onPreferenceClickListener = this
        if (summaryFlags and SUMMARY_FLAG_NORMAL == SUMMARY_FLAG_NORMAL) {
            val defaultSummary = if (summaryId == 0) "" else context.getString(summaryId)
            var summary = sharedPrefs.getString(key, defaultSummary)
            if (summary!!.isEmpty() && !defaultSummary.isEmpty()) {
                summary = defaultSummary
            }
            val mask = summaryFlags and SUMMARY_FLAG_MASK == SUMMARY_FLAG_MASK
            preference.summary = if (mask) mask(summary) else summary
        }
        keyMap[key] = keyId
        prefMap[key] = preference
    }

    fun bindDataVersionPreference() {
        bindPreference(R.string.offline_data_version_key)
        val dataVersion = preferenceActions.findPreference(context.getString(R.string.offline_data_version_key))
        val status = instance.status
        var summary = context.getString(R.string.offline_data_version_summary, status.version,
                status.count, getDate(status.timestamp * 1000))
        if (status.version == 0) {
            summary = context.getString(R.string.no_offline_data)
        }
        dataVersion!!.summary = summary
    }

    private fun bindVersionPreference() {
        bindPreference(R.string.version_key)
        val version = preferenceActions.findPreference(context.getString(R.string.version_key))
        var versionString = BuildConfig.VERSION_NAME
        if (BuildConfig.DEBUG) {
            versionString += "." + BuildConfig.BUILD_TYPE
        }
        version!!.summary = versionString
        val isShowHidden = sharedPrefs.getBoolean(context.getString(R.string.show_hidden_setting_key), false)
        if (isShowHidden) {
            version.onPreferenceClickListener = null
        } else {
            removePreference(R.string.advanced_key, R.string.custom_data_key)
            removePreference(R.string.advanced_key, R.string.force_chinese_key)
            removePreference(R.string.float_window_key, R.string.window_trans_back_only_key)
        }
    }

    private fun bindPluginPreference() {
        val pluginPref = preferenceActions.findPreference(context.getString(R.string.plugin_key)) as PreferenceScreen?
        pluginPref!!.isEnabled = false
        pluginPref.summary = context.getString(R.string.plugin_not_started)
        if (isAppInstalled(context, context.getString(R.string.plugin_package_name))) {
            pluginBinder.bindPluginService()
            bindPreference(R.string.auto_hangup_key)
            bindPreference(R.string.add_call_log_key)
            bindPreference(R.string.ring_once_and_auto_hangup_key)
            bindPreference(R.string.hide_plugin_icon_key)
            bindPreference(R.string.hangup_keyword_key, R.string.hangup_keyword_summary)
            bindPreference(R.string.hangup_geo_keyword_key,
                    R.string.hangup_geo_keyword_summary)
            bindPreference(R.string.hangup_number_keyword_key,
                    R.string.hangup_number_keyword_summary)
            bindPreference(R.string.import_key)
            bindPreference(R.string.export_key)
            val pluginPkg = context.getString(R.string.plugin_package_name)
            val pluginVersion = getVersionCode(context, pluginPkg)
            if (pluginVersion < 3) {
                val exportPref = preferenceActions.findPreference(context.getString(R.string.export_key))
                exportPref!!.isEnabled = false
                exportPref.setSummary(R.string.plugin_too_old)
                val importPref = preferenceActions.findPreference(context.getString(R.string.import_key))
                importPref!!.isEnabled = false
                importPref.setSummary(R.string.plugin_too_old)
            }
            val iconPref = preferenceActions.findPreference(
                    context.getString(R.string.hide_plugin_icon_key)) as SwitchPreference?
            if (pluginVersion < 10) {
                iconPref!!.isEnabled = false
                iconPref.setSummary(R.string.plugin_too_old)
            } else {
                val iconEnabled = isComponentEnabled(
                        context.packageManager, pluginPkg, "$pluginPkg.Launcher")
                iconPref!!.isChecked = !iconEnabled
            }
        } else {
            removePreference(R.string.advanced_key, R.string.plugin_key)
        }
    }

    fun removePreference(parent: Int, child: Int) {
        val childKey = context.getString(child)
        val parentKey = context.getString(parent)
        val preference = preferenceActions.findPreference(childKey)
        val category = preferenceActions.findPreference(parentKey) as PreferenceCategory?
        category!!.removePreference(preference)
        prefMap[childKey] = preference
        prefMap[parentKey] = category
    }

    fun addPreference(parent: Int, child: Int) {
        val childKey = context.getString(child)
        val parentKey = context.getString(parent)
        val preference = preferenceActions.findPreference(childKey)
        val category = preferenceActions.findPreference(parentKey) as PreferenceCategory?
        category!!.addPreference(preference)
        keyMap[childKey] = child
        keyMap[parentKey] = parent
        prefMap[childKey] = preference
        prefMap[parentKey] = category
    }

    fun getKeyId(key: String): Int {
        return keyMap[key]!!
    }

    companion object {
        private const val SUMMARY_FLAG_NORMAL = 0x00000001
        private const val SUMMARY_FLAG_MASK = 0x00000002
        private const val SUMMARY_FLAG_NULL = 0x00000004
    }
}