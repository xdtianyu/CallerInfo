package org.xdty.callerinfo.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.Status;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.utils.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PreferenceBinder implements Preference.OnPreferenceClickListener {

    private final static int SUMMARY_FLAG_NORMAL = 0x00000001;
    private final static int SUMMARY_FLAG_MASK = 0x00000002;
    private final static int SUMMARY_FLAG_NULL = 0x00000004;

    private Context context;
    private PreferenceClicker preferenceClicker;
    private PreferenceDialogs preferenceDialogs;
    private PluginBinder pluginBinder;
    private PreferenceActions preferenceActions;

    private HashMap<String, Integer> keyMap = new HashMap<>();
    private HashMap<String, Preference> prefMap = new HashMap<>();
    SharedPreferences sharedPrefs;

    public PreferenceBinder(
            Context context,
            SharedPreferences sharedPrefs,
            PreferenceDialogs preferenceDialogs,
            PluginBinder pluginBinder,
            PreferenceActions preferenceActions
    ) {
        this.context = context;
        this.sharedPrefs = sharedPrefs;
        this.pluginBinder = pluginBinder;
        this.preferenceDialogs = preferenceDialogs;
        this.preferenceActions = preferenceActions;
        this.preferenceClicker = new PreferenceClicker(context, sharedPrefs, preferenceDialogs, pluginBinder, preferenceActions);
    }

    public void bind() {
        bindPreference(R.string.juhe_api_key, true);
        bindPreference(R.string.window_text_size_key);
        bindPreference(R.string.window_height_key);
        bindPreferenceList(R.string.window_text_alignment_key, R.array.align_type, 1);
        bindPreference(R.string.window_transparent_key);
        bindPreference(R.string.window_text_padding_key);
        bindPreferenceList(R.string.api_type_key, R.array.api_type, 1, 1);
        bindPreference(R.string.ignore_known_contact_key);
        bindPreference(R.string.display_on_outgoing_key);
        bindPreference(R.string.catch_crash_key);
        bindPreference(R.string.ignore_regex_key, false);
        bindPreference(R.string.custom_api_url);
        bindPreference(R.string.ignore_battery_optimizations_key);
        bindPreference(R.string.auto_report_key);
        bindPreference(R.string.enable_marking_key);
        bindPreference(R.string.not_mark_contact_key);
        bindPreference(R.string.temporary_disable_blacklist_key);
        bindPreference(R.string.outgoing_window_position_key);
        bindPreference(R.string.offline_data_auto_upgrade_key);
        bindPreference(R.string.offline_data_check_now_key);

        bindDataVersionPreference();
        bindVersionPreference();
        bindPluginPreference();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return preferenceClicker.dispatchClick(preference);
    }

    public Preference findCachedPreference(CharSequence key) {
        return prefMap.get(key.toString());
    }

    private void bindPreference(int keyId) {
        bindPreference(keyId, SUMMARY_FLAG_NULL, 0);
    }

    private void bindPreference(int keyId, boolean mask) {
        if (mask) {
            bindPreference(keyId, SUMMARY_FLAG_NORMAL | SUMMARY_FLAG_MASK, 0);
        } else {
            bindPreference(keyId, SUMMARY_FLAG_NORMAL, 0);
        }
    }

    private void bindPreference(int keyId, int summaryId) {
        bindPreference(keyId, SUMMARY_FLAG_NORMAL, summaryId);
    }

    private void bindPreferenceList(int keyId, int arrayId, int index) {
        bindPreferenceList(keyId, arrayId, index, 0);
    }

    private void bindPreferenceList(int keyId, int arrayId, int defValue, int offset) {
        String key = context.getString(keyId);
        Preference preference = preferenceActions.findPreference(key);
        List<String> apiList = Arrays.asList(context.getResources().getStringArray(arrayId));
        preference.setOnPreferenceClickListener(this);
        preference.setSummary(apiList.get(sharedPrefs.getInt(key, defValue) - offset));
        keyMap.put(key, keyId);
        prefMap.put(key, preference);
    }

    private void bindPreference(int keyId, int summaryFlags, int summaryId) {
        String key = context.getString(keyId);
        Preference preference = preferenceActions.findPreference(key);
        preference.setOnPreferenceClickListener(this);

        if ((summaryFlags & SUMMARY_FLAG_NORMAL) == SUMMARY_FLAG_NORMAL) {
            String defaultSummary = summaryId == 0 ? "" : context.getString(summaryId);
            String summary = sharedPrefs.getString(key, defaultSummary);

            if (summary.isEmpty() && !defaultSummary.isEmpty()) {
                summary = defaultSummary;
            }

            boolean mask = ((summaryFlags & SUMMARY_FLAG_MASK) == SUMMARY_FLAG_MASK);
            preference.setSummary(mask ? Utils.Companion.mask(summary) : summary);
        }
        keyMap.put(key, keyId);
        prefMap.put(key, preference);
    }


    public void bindDataVersionPreference() {
        bindPreference(R.string.offline_data_version_key);
        Preference dataVersion = preferenceActions.findPreference(context.getString(R.string.offline_data_version_key));
        Status status = SettingImpl.Companion.getInstance().getStatus();
        String summary = context.getString(R.string.offline_data_version_summary, status.getVersion(),
                status.getCount(), Utils.Companion.getDate(status.getTimestamp() * 1000));
        if (status.getVersion() == 0) {
            summary = context.getString(R.string.no_offline_data);
        }
        dataVersion.setSummary(summary);
    }

    private void bindVersionPreference() {

        bindPreference(R.string.version_key);
        Preference version = preferenceActions.findPreference(context.getString(R.string.version_key));
        String versionString = BuildConfig.VERSION_NAME;
        if (BuildConfig.DEBUG) {
            versionString += "." + BuildConfig.BUILD_TYPE;
        }
        version.setSummary(versionString);

        boolean isShowHidden =
                sharedPrefs.getBoolean(context.getString(R.string.show_hidden_setting_key), false);

        if (isShowHidden) {
            version.setOnPreferenceClickListener(null);
        } else {
            removePreference(R.string.advanced_key, R.string.custom_data_key);
            removePreference(R.string.advanced_key, R.string.force_chinese_key);
            removePreference(R.string.float_window_key, R.string.window_trans_back_only_key);
        }
    }

    private void bindPluginPreference() {
        PreferenceScreen pluginPref =
                (PreferenceScreen) preferenceActions.findPreference(context.getString(R.string.plugin_key));
        pluginPref.setEnabled(false);
        pluginPref.setSummary(context.getString(R.string.plugin_not_started));
        if (Utils.Companion.isAppInstalled(context, context.getString(R.string.plugin_package_name))) {
            pluginBinder.bindPluginService();

            bindPreference(R.string.auto_hangup_key);
            bindPreference(R.string.add_call_log_key);
            bindPreference(R.string.ring_once_and_auto_hangup_key);
            bindPreference(R.string.hide_plugin_icon_key);

            bindPreference(R.string.hangup_keyword_key, R.string.hangup_keyword_summary);
            bindPreference(R.string.hangup_geo_keyword_key,
                    R.string.hangup_geo_keyword_summary);
            bindPreference(R.string.hangup_number_keyword_key,
                    R.string.hangup_number_keyword_summary);

            bindPreference(R.string.import_key);
            bindPreference(R.string.export_key);

            String pluginPkg = context.getString(R.string.plugin_package_name);
            int pluginVersion = Utils.Companion.getVersionCode(context, pluginPkg);
            if (pluginVersion < 3) {
                Preference exportPref = preferenceActions.findPreference(context.getString(R.string.export_key));
                exportPref.setEnabled(false);
                exportPref.setSummary(R.string.plugin_too_old);
                Preference importPref = preferenceActions.findPreference(context.getString(R.string.import_key));
                importPref.setEnabled(false);
                importPref.setSummary(R.string.plugin_too_old);
            }

            SwitchPreference iconPref = (SwitchPreference) preferenceActions.findPreference(
                    context.getString(R.string.hide_plugin_icon_key));

            if (pluginVersion < 10) {
                iconPref.setEnabled(false);
                iconPref.setSummary(R.string.plugin_too_old);
            } else {
                boolean iconEnabled = Utils.Companion.isComponentEnabled(
                        context.getPackageManager(), pluginPkg, pluginPkg + ".Launcher");
                iconPref.setChecked(!iconEnabled);
            }

        } else {
            removePreference(R.string.advanced_key, R.string.plugin_key);
        }
    }

    public void removePreference(int parent, int child) {
        String childKey = context.getString(child);
        String parentKey = context.getString(parent);
        Preference preference = preferenceActions.findPreference(childKey);
        PreferenceCategory category = (PreferenceCategory) preferenceActions.findPreference(parentKey);
        category.removePreference(preference);
        prefMap.put(childKey, preference);
        prefMap.put(parentKey, category);
    }

    public void addPreference(int parent, int child) {
        String childKey = context.getString(child);
        String parentKey = context.getString(parent);
        Preference preference = preferenceActions.findPreference(childKey);
        PreferenceCategory category = (PreferenceCategory) preferenceActions.findPreference(parentKey);
        category.addPreference(preference);
        keyMap.put(childKey, child);
        keyMap.put(parentKey, parent);
        prefMap.put(childKey, preference);
        prefMap.put(parentKey, category);
    }

    public int getKeyId(String key) {
        return keyMap.get(key);
    }
}