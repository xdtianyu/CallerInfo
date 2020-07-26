package org.xdty.callerinfo.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.preference.Preference
import android.view.WindowManager
import android.widget.Toast
import org.xdty.callerinfo.R
import org.xdty.callerinfo.service.FloatWindow

class PreferenceClicker(private val context: Context, var sharedPrefs: SharedPreferences,
                        var preferenceDialogs: PreferenceDialogs,
                        var pluginBinder: PluginBinder,
                        var preferenceActions: PreferenceActions) {
    private val point: Point
    var versionClickCount = 0
    var toast: Toast? = null

    init {
        val mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = mWindowManager.defaultDisplay
        point = Point()
        display.getSize(point)
    }

    @SuppressLint("NonConstantResourceId")
    fun dispatchClick(preference: Preference): Boolean {
        val keyId = preferenceActions.getKeyId(preference.key)
        when (keyId) {
            R.string.juhe_api_key -> preferenceDialogs.showApiDialog(R.string.juhe_api_key, R.string.custom_jh_api_key,
                    R.string.juhe_api_url)
            R.string.window_text_size_key -> preferenceDialogs.showSeekBarDialog(R.string.window_text_size_key, FloatWindow.TEXT_SIZE, 20, 60,
                    R.string.window_text_size, R.string.text_size)
            R.string.window_height_key -> preferenceDialogs.showSeekBarDialog(R.string.window_height_key, FloatWindow.WINDOW_HEIGHT,
                    point.y / 8, point.y / 4, R.string.window_height,
                    R.string.window_height_message)
            R.string.window_text_alignment_key -> preferenceDialogs.showRadioDialog(R.string.window_text_alignment_key,
                    R.string.window_text_alignment, R.array.align_type, 1)
            R.string.window_transparent_key -> preferenceDialogs.showSeekBarDialog(R.string.window_transparent_key, FloatWindow.WINDOW_TRANS, 80,
                    100, R.string.window_transparent, R.string.text_transparent)
            R.string.window_text_padding_key -> preferenceDialogs.showSeekBarDialog(R.string.window_text_padding_key, FloatWindow.TEXT_PADDING, 0,
                    point.x / 2,
                    R.string.window_text_padding, R.string.text_padding)
            R.string.api_type_key -> preferenceDialogs.showRadioDialog(R.string.api_type_key, R.string.api_type, R.array.api_type, 1,
                    1)
            R.string.ignore_known_contact_key, R.string.not_mark_contact_key -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val res = context.checkSelfPermission(
                            Manifest.permission.READ_CONTACTS)
                    if (res != PackageManager.PERMISSION_GRANTED) {
                        (context as Activity).requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS),
                                keyId)
                        return true
                    }
                }
                return false
            }
            R.string.display_on_outgoing_key -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val res = context.checkSelfPermission(
                            Manifest.permission.PROCESS_OUTGOING_CALLS)
                    if (res != PackageManager.PERMISSION_GRANTED) {
                        (context as Activity).requestPermissions(arrayOf(Manifest.permission.PROCESS_OUTGOING_CALLS),
                                keyId)
                        return true
                    }
                }
                return false
            }
            R.string.catch_crash_key -> if (sharedPrefs.getBoolean(context.getString(R.string.catch_crash_key), false)) {
                preferenceDialogs.showTextDialog(R.string.catch_crash, R.string.catch_crash_message)
            }
            R.string.ignore_regex_key -> {
                preferenceDialogs.showEditDialog(R.string.ignore_regex_key, R.string.ignore_regex,
                        R.string.empty_string,
                        R.string.ignore_regex_hint, R.string.example, R.string.regex_example)
                return false
            }
            R.string.hangup_keyword_key -> {
                preferenceDialogs.showEditDialog(R.string.hangup_keyword_key, R.string.hangup_keyword,
                        R.string.hangup_keyword_default, R.string.hangup_keyword_hint)
                return false
            }
            R.string.hangup_number_keyword_key -> {
                preferenceDialogs.showEditDialog(R.string.hangup_number_keyword_key,
                        R.string.hangup_number_keyword,
                        R.string.empty_string, R.string.hangup_keyword_hint)
                return false
            }
            R.string.hangup_geo_keyword_key -> {
                preferenceDialogs.showEditDialog(R.string.hangup_geo_keyword_key, R.string.hangup_geo_keyword,
                        R.string.empty_string, R.string.hangup_keyword_hint)
                return false
            }
            R.string.temporary_disable_blacklist_key -> {
                if (sharedPrefs.getBoolean(context.getString(R.string.temporary_disable_blacklist_key),
                                false)) {
                    preferenceDialogs.showRadioDialog(R.string.repeated_incoming_count_key,
                            R.string.temporary_disable_blacklist,
                            R.array.repeated_incoming_count, 1)
                }
                return false
            }
            R.string.custom_api_url -> {
                preferenceDialogs.showCustomApiDialog()
                return false
            }
            R.string.auto_hangup_key -> {
                pluginBinder.checkCallPermission()
                return false
            }
            R.string.add_call_log_key -> {
                PluginStatus.isCheckRingOnce = false
                pluginBinder.checkCallLogPermission()
                return false
            }
            R.string.ring_once_and_auto_hangup_key -> {
                PluginStatus.isCheckRingOnce = true
                pluginBinder.checkCallLogPermission()
                return false
            }
            R.string.hide_plugin_icon_key -> {
                pluginBinder.setIconStatus(!sharedPrefs.getBoolean(
                        context.getString(R.string.hide_plugin_icon_key), false))
                return false
            }
            R.string.version_key -> {
                checkVersionCounts()
                return false
            }
            R.string.export_key -> {
                preferenceDialogs.showConfirmDialog(R.string.export_data, R.string.export_confirm,
                        R.string.export_key)
                return false
            }
            R.string.import_key -> {
                preferenceDialogs.showConfirmDialog(R.string.import_data, R.string.import_confirm,
                        R.string.import_key)
                return false
            }
            R.string.auto_report_key -> {
                if (sharedPrefs.getBoolean(context.getString(R.string.auto_report_key), false)) {
                    preferenceDialogs.showConfirmDialog(R.string.auto_report, R.string.auto_report_confirm,
                            R.string.auto_report_key)
                }
                return false
            }
            R.string.ignore_battery_optimizations_key -> {
                preferenceDialogs.showConfirmDialog(R.string.ignore_battery_optimizations,
                        R.string.ignore_battery_optimizations_description,
                        R.string.ignore_battery_optimizations_key)
                return false
            }
            R.string.enable_marking_key -> {
                if (sharedPrefs.getBoolean(context.getString(R.string.enable_marking_key), false)) {
                    preferenceDialogs.showConfirmDialog(R.string.enable_marking, R.string.mark_confirm,
                            R.string.enable_marking_key)
                }
                return false
            }
            R.string.offline_data_check_now_key -> {
                preferenceActions.checkOfflineData()
                return false
            }
            R.string.offline_data_auto_upgrade_key -> {
                preferenceActions.resetOfflineDataUpgradeWorker()
                return false
            }
            R.string.outgoing_window_position_key -> if (sharedPrefs.getBoolean(context.getString(R.string.outgoing_window_position_key),
                            false)) {
                preferenceDialogs.showTextDialog(R.string.outgoing_window_position,
                        R.string.outgoing_window_position_message)
            }
        }
        return true
    }

    private fun checkVersionCounts() {
        versionClickCount++
        if (versionClickCount == 7) {
            sharedPrefs.edit().putBoolean(context.getString(R.string.show_hidden_setting_key),
                    true).apply()
            preferenceActions.addPreference(R.string.advanced_key, R.string.custom_data_key)
            preferenceActions.addPreference(R.string.advanced_key, R.string.force_chinese_key)
            preferenceActions.addPreference(R.string.float_window_key,
                    R.string.window_trans_back_only_key)
        }
        if (versionClickCount in 4..6) {
            toast?.cancel()
            toast = Toast.makeText(context,
                    context.getString(R.string.show_hidden_toast, 7 - versionClickCount),
                    Toast.LENGTH_SHORT)
            toast?.show()
        }
    }
}