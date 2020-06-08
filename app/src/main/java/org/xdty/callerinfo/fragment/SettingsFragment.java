package org.xdty.callerinfo.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.appbar.AppBarLayout;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.exporter.Exporter;
import org.xdty.callerinfo.model.Status;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.plugin.IPluginService;
import org.xdty.callerinfo.plugin.IPluginServiceCallback;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Toasts;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.callerinfo.utils.Window;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import app.minimize.com.seek_bar_compat.SeekBarCompat;
import io.reactivex.functions.Consumer;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SettingsFragment extends PreferenceFragment implements PreferenceActions {

    private final static String TAG = SettingsFragment.class.getSimpleName();

    private final static String PLUGIN_SETTING = "org.xdty.callerinfo.action.PLUGIN_SETTING";

    private final static int SUMMARY_FLAG_NORMAL = 0x00000001;
    private final static int SUMMARY_FLAG_MASK = 0x00000002;
    private final static int SUMMARY_FLAG_NULL = 0x00000004;


    @Inject
    Window mWindow;

    @Inject
    Setting mSetting;

    @Inject
    Alarm mAlarm;

    private Intent mIntent;

    private PreferenceDialogs preferenceDialogs;
    private PreferenceBinder preferenceBinder;
    private PluginBinder pluginBinder;

    public static SettingsFragment newInstance(Intent intent) {
        SettingsFragment fragment = new SettingsFragment();
        fragment.setStartIntent(intent);
        return fragment;
    }

    private void setStartIntent(Intent intent) {
        mIntent = intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Application.Companion.getAppComponent().inject(this);

        addPreferencesFromResource(R.xml.settings);

        SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();

        preferenceDialogs = new PreferenceDialogs(getActivity(), sharedPrefs, mWindow, this);
        pluginBinder = new PluginBinder(getActivity(), preferenceDialogs, this);

        preferenceBinder = new PreferenceBinder(getActivity(), sharedPrefs, preferenceDialogs, pluginBinder, this);
        preferenceBinder.bind();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mIntent != null && mIntent.getAction() != null) {
            String action = mIntent.getAction();

            switch (action) {
                case PLUGIN_SETTING:
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            openPreference(getString(R.string.plugin_key));
                        }
                    }, 500);
                    break;
                default:
                    break;
            }
        }
    }

    private void openPreference(String key) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        final ListAdapter listAdapter = preferenceScreen.getRootAdapter();

        final int itemsCount = listAdapter.getCount();
        int itemNumber;
        for (itemNumber = 0; itemNumber < itemsCount; ++itemNumber) {
            if (listAdapter.getItem(itemNumber).equals(findPreference(key))) {
                preferenceScreen.onItemClick(null, null, itemNumber, 0);
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        if (Utils.Companion.isAppInstalled(getActivity(), getString(R.string.plugin_package_name))) {
            pluginBinder.unBindPluginService();
        }
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference instanceof PreferenceScreen) {
            setUpNestedScreen((PreferenceScreen) preference);
        }
        return false;
    }

    @Override
    public Preference findPreference(CharSequence key) {
        Preference pref = preferenceBinder.findCachedPreference(key.toString());
        if (pref == null) {
            pref = super.findPreference(key);
        }
        return pref;
    }

    public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();

        AppBarLayout appBarLayout;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                || Build.VERSION.RELEASE.equals("7.0") || Build.VERSION.RELEASE.equals("N")) {
            ListView listView = dialog.findViewById(android.R.id.list);
            ViewGroup root = (ViewGroup) listView.getParent();

            appBarLayout = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(
                    R.layout.settings_toolbar, root, false);

            int height;
            TypedValue tv = new TypedValue();
            if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data,
                        getResources().getDisplayMetrics());
            } else {
                height = appBarLayout.getHeight();
            }
            listView.setPadding(0, height, 0, 0);
            root.addView(appBarLayout, 0);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            LinearLayout root =
                    (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
            appBarLayout = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(
                    R.layout.settings_toolbar, root, false);
            root.addView(appBarLayout, 0);
        } else {
            ViewGroup root = dialog.findViewById(android.R.id.content);
            ListView content = (ListView) root.getChildAt(0);

            root.removeAllViews();

            appBarLayout = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(
                    R.layout.settings_toolbar, root, false);

            int height;
            TypedValue tv = new TypedValue();
            if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data,
                        getResources().getDisplayMetrics());
            } else {
                height = appBarLayout.getHeight();
            }

            content.setPadding((int) dpToPx(16), height, (int) dpToPx(16), 0);

            root.addView(content);
            root.addView(appBarLayout);
        }

        Toolbar toolbar = appBarLayout.findViewById(R.id.toolbar);
        toolbar.setTitle(preferenceScreen.getTitle());
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case R.string.ignore_known_contact_key:
            case R.string.not_mark_contact_key:
            case R.string.display_on_outgoing_key:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    setChecked(requestCode, false);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getActivity().getResources().getDisplayMetrics());
    }

    @Override
    public void resetOfflineDataUpgradeWorker() {
        if (!mSetting.isOfflineDataAutoUpgrade()) {
            mAlarm.cancelUpgradeWork();
        } else {
            mAlarm.enqueueUpgradeWork();
        }
    }

    @Override
    public void onConfirmed(int key) {
        switch (key) {
            case R.string.import_key:
                PluginStatus.isCheckStorageExport = false;
                pluginBinder.checkStoragePermission();
                break;
            case R.string.export_key:
                PluginStatus.isCheckStorageExport = true;
                pluginBinder.checkStoragePermission();
                break;
            case R.string.ignore_battery_optimizations_key:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent(
                            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                }
                break;
        }
    }

    @Override
    public void onConfirmCanceled(int key) {
        switch (key) {
            case R.string.auto_report_key:
                ((SwitchPreference) findPreference(
                        getString(R.string.auto_report_key))).setChecked(false);
                break;
            case R.string.enable_marking_key:
                ((SwitchPreference) findPreference(
                        getString(R.string.enable_marking_key))).setChecked(false);
                break;
            case R.string.ignore_battery_optimizations_key:
                ((SwitchPreference) findPreference(
                        getString(R.string.ignore_battery_optimizations_key))).setChecked(
                        Utils.Companion.ignoreBatteryOptimization(getActivity()));
                break;
        }
    }

    @Override
    public void setChecked(int key, boolean checked) {
        SwitchPreference preference = (SwitchPreference) findPreference(getString(key));
        preference.setChecked(checked);
    }

    @Override
    public void checkOfflineData() {
        Toasts.INSTANCE.show(getActivity(), R.string.offline_data_checking);
        mAlarm.runUpgradeWorkOnce().observeForever(new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                Log.d(TAG, "onChanged: " + workInfo);
                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    Toasts.INSTANCE.show(getActivity(), R.string.offline_data_success);
                    preferenceBinder.bindDataVersionPreference();
                } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                    Toasts.INSTANCE.show(getActivity(), R.string.offline_data_failed);
                }
                WorkManager.getInstance().getWorkInfoByIdLiveData(workInfo.getId()).removeObserver(this);
            }
        });
    }

    @Override
    public void removePreference(int parent, int child) {
        preferenceBinder.removePreference(parent, child);
    }

    @Override
    public int getKeyId(String key) {
        return preferenceBinder.getKeyId(key);
    }

    @Override
    public void addPreference(int parent, int child) {
        preferenceBinder.addPreference(parent, child);
    }

    public static class PluginStatus {
        public static boolean isCheckStorageExport = false;
        public static boolean isCheckRingOnce = false;
    }

    public static class PluginBinder implements ServiceConnection {
        private final Intent mPluginIntent = new Intent().setComponent(new ComponentName(
                "org.xdty.callerinfo.plugin",
                "org.xdty.callerinfo.plugin.PluginService"));

        private Context context;
        private IPluginService mPluginService;
        private PreferenceDialogs preferenceDialogs;
        private PreferenceActions preferenceActions;

        public PluginBinder(Context context, PreferenceDialogs preferenceDialogs, PreferenceActions preferenceActions) {
            this.context = context;
            this.preferenceDialogs = preferenceDialogs;
            this.preferenceActions = preferenceActions;
        }

        private void bindPluginService() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(mPluginIntent);
                } else {
                    context.startService(mPluginIntent);
                }
                context.bindService(mPluginIntent, this, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void unBindPluginService() {
            try {
                context.unbindService(this);
                context.stopService(mPluginIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: " + name.toString());
            mPluginService = IPluginService.Stub.asInterface(service);
            try {
                mPluginService.registerCallback(new IPluginServiceCallback.Stub() {
                    @Override
                    public void onCallPermissionResult(final boolean success) throws
                            RemoteException {
                        Log.d(TAG, "onCallPermissionResult: " + success);
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                preferenceActions.setChecked(R.string.auto_hangup_key, success);
                            }
                        });
                    }

                    @Override
                    public void onCallLogPermissionResult(final boolean success) throws
                            RemoteException {
                        Log.d(TAG, "onCallLogPermissionResult: " + success);
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (PluginStatus.isCheckRingOnce) {
                                    preferenceActions.setChecked(R.string.ring_once_and_auto_hangup_key, success);
                                } else {
                                    preferenceActions.setChecked(R.string.add_call_log_key, success);
                                }
                            }
                        });
                    }

                    @Override
                    public void onStoragePermissionResult(boolean success) throws RemoteException {
                        Log.d(TAG, "onStoragePermissionResult: " + success);
                        if (success) {
                            if (PluginStatus.isCheckStorageExport) {
                                exportData();
                            } else {
                                importData();
                            }
                        } else {
                            Toasts.INSTANCE.show(context, R.string.storage_permission_failed);
                        }
                    }
                });
                enablePluginPreference();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void enablePluginPreference() {
            PreferenceScreen pluginPref =
                    (PreferenceScreen) preferenceActions.findPreference(context.getString(R.string.plugin_key));
            pluginPref.setEnabled(true);
            pluginPref.setSummary("");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: " + name.toString());
            mPluginService = null;
        }

        public void checkStoragePermission() {
            try {
                if (mPluginService != null) {
                    mPluginService.checkStoragePermission();
                } else {
                    Log.e(TAG, "PluginService is stopped!!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        @SuppressLint("CheckResult")
        private void importData() {
            try {
                if (mPluginService != null) {
                    String data = mPluginService.importData();
                    if (data.contains("Error:")) {
                        preferenceDialogs.showTextDialog(R.string.import_data,
                                context.getString(R.string.import_failed, data));
                    } else {
                        Exporter exporter = new Exporter(context);
                        exporter.fromString(data).subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) {
                                if (s == null) {
                                    preferenceDialogs.showTextDialog(R.string.import_data,
                                            R.string.import_succeed);
                                } else {
                                    preferenceDialogs.showTextDialog(R.string.import_data,
                                            context.getString(R.string.import_failed, s));
                                }
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "PluginService is stopped!!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @SuppressLint("CheckResult")
        private void exportData() {
            Exporter exporter = new Exporter(context);
            exporter.export().subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    try {
                        String res = mPluginService.exportData(s);
                        if (res.contains("Error")) {
                            preferenceDialogs.showTextDialog(R.string.export_data,
                                    context.getString(R.string.export_failed, res));
                        } else {
                            preferenceDialogs.showTextDialog(R.string.export_data,
                                    context.getString(R.string.export_succeed, res));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        public void checkCallPermission() {
            try {
                mPluginService.checkCallPermission();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void checkCallLogPermission() {
            try {
                mPluginService.checkCallLogPermission();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setIconStatus(boolean show) {
            try {
                mPluginService.setIconStatus(show);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class PreferenceBinder implements Preference.OnPreferenceClickListener {

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


        private void bindDataVersionPreference() {
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

    public static class PreferenceClicker {
        private Point mPoint;
        private Context context;

        SharedPreferences sharedPrefs;
        PreferenceDialogs preferenceDialogs;
        PluginBinder pluginBinder;
        PreferenceActions preferenceActions;

        int versionClickCount;
        Toast toast;

        public PreferenceClicker(Context context, SharedPreferences sharedPrefs,
                                 PreferenceDialogs preferenceDialogs,
                                 PluginBinder pluginBinder,
                                 PreferenceActions preferenceActions) {
            this.sharedPrefs = sharedPrefs;
            this.preferenceDialogs = preferenceDialogs;
            this.context = context;
            this.pluginBinder = pluginBinder;
            this.preferenceActions = preferenceActions;

            WindowManager mWindowManager = (WindowManager) context.
                    getSystemService(Context.WINDOW_SERVICE);
            Display display = mWindowManager.getDefaultDisplay();
            mPoint = new Point();
            display.getSize(mPoint);
        }

        public boolean dispatchClick(Preference preference) {
            int keyId = preferenceActions.getKeyId(preference.getKey());
            switch (keyId) {
                case R.string.juhe_api_key:
                    preferenceDialogs.showApiDialog(R.string.juhe_api_key, R.string.custom_jh_api_key,
                            R.string.juhe_api_url);
                    break;
                case R.string.window_text_size_key:
                    preferenceDialogs.showSeekBarDialog(R.string.window_text_size_key, FloatWindow.TEXT_SIZE, 20, 60,
                            R.string.window_text_size, R.string.text_size);
                    break;
                case R.string.window_height_key:
                    preferenceDialogs.showSeekBarDialog(R.string.window_height_key, FloatWindow.WINDOW_HEIGHT,
                            mPoint.y / 8, mPoint.y / 4, R.string.window_height,
                            R.string.window_height_message);
                    break;
                case R.string.window_text_alignment_key:
                    preferenceDialogs.showRadioDialog(R.string.window_text_alignment_key,
                            R.string.window_text_alignment, R.array.align_type, 1);
                    break;
                case R.string.window_transparent_key:
                    preferenceDialogs.showSeekBarDialog(R.string.window_transparent_key, FloatWindow.WINDOW_TRANS, 80,
                            100, R.string.window_transparent, R.string.text_transparent);
                    break;
                case R.string.window_text_padding_key:
                    preferenceDialogs.showSeekBarDialog(R.string.window_text_padding_key, FloatWindow.TEXT_PADDING, 0,
                            mPoint.x / 2,
                            R.string.window_text_padding, R.string.text_padding);
                    break;
                case R.string.api_type_key:
                    preferenceDialogs.showRadioDialog(R.string.api_type_key, R.string.api_type, R.array.api_type, 1,
                            1);
                    break;
                case R.string.ignore_known_contact_key:
                case R.string.not_mark_contact_key:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int res = context.checkSelfPermission(
                                Manifest.permission.READ_CONTACTS);
                        if (res != PackageManager.PERMISSION_GRANTED) {
                            ((Activity) context).requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                                    keyId);
                            return true;
                        }
                    }
                    return false;
                case R.string.display_on_outgoing_key:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int res = context.checkSelfPermission(
                                Manifest.permission.PROCESS_OUTGOING_CALLS);
                        if (res != PackageManager.PERMISSION_GRANTED) {
                            ((Activity) context).requestPermissions(
                                    new String[]{Manifest.permission.PROCESS_OUTGOING_CALLS},
                                    keyId);
                            return true;
                        }
                    }
                    return false;
                case R.string.catch_crash_key:
                    if (sharedPrefs.getBoolean(context.getString(R.string.catch_crash_key), false)) {
                        preferenceDialogs.showTextDialog(R.string.catch_crash, R.string.catch_crash_message);
                    }
                    break;
                case R.string.ignore_regex_key:
                    preferenceDialogs.showEditDialog(R.string.ignore_regex_key, R.string.ignore_regex,
                            R.string.empty_string,
                            R.string.ignore_regex_hint, R.string.example, R.string.regex_example);
                    return false;
                case R.string.hangup_keyword_key:
                    preferenceDialogs.showEditDialog(R.string.hangup_keyword_key, R.string.hangup_keyword,
                            R.string.hangup_keyword_default, R.string.hangup_keyword_hint);
                    return false;
                case R.string.hangup_number_keyword_key:
                    preferenceDialogs.showEditDialog(R.string.hangup_number_keyword_key,
                            R.string.hangup_number_keyword,
                            R.string.empty_string, R.string.hangup_keyword_hint);
                    return false;
                case R.string.hangup_geo_keyword_key:
                    preferenceDialogs.showEditDialog(R.string.hangup_geo_keyword_key, R.string.hangup_geo_keyword,
                            R.string.empty_string, R.string.hangup_keyword_hint);
                    return false;
                case R.string.temporary_disable_blacklist_key:
                    if (sharedPrefs.getBoolean(context.getString(R.string.temporary_disable_blacklist_key),
                            false)) {
                        preferenceDialogs.showRadioDialog(R.string.repeated_incoming_count_key,
                                R.string.temporary_disable_blacklist,
                                R.array.repeated_incoming_count, 1);
                    }
                    return false;
                case R.string.custom_api_url:
                    preferenceDialogs.showCustomApiDialog();
                    return false;
                case R.string.auto_hangup_key:
                    pluginBinder.checkCallPermission();
                    return false;
                case R.string.add_call_log_key:
                    PluginStatus.isCheckRingOnce = false;
                    pluginBinder.checkCallLogPermission();
                    return false;
                case R.string.ring_once_and_auto_hangup_key:
                    PluginStatus.isCheckRingOnce = true;
                    pluginBinder.checkCallLogPermission();
                    return false;
                case R.string.hide_plugin_icon_key:
                    pluginBinder.setIconStatus(!sharedPrefs.getBoolean(
                            context.getString(R.string.hide_plugin_icon_key), false));
                    return false;
                case R.string.version_key:
                    versionClickCount++;
                    if (versionClickCount == 7) {
                        sharedPrefs.edit().putBoolean(context.getString(R.string.show_hidden_setting_key),
                                true).apply();

                        preferenceActions.addPreference(R.string.advanced_key, R.string.custom_data_key);
                        preferenceActions.addPreference(R.string.advanced_key, R.string.force_chinese_key);
                        preferenceActions.addPreference(R.string.float_window_key,
                                R.string.window_trans_back_only_key);
                    }
                    if (versionClickCount > 3 && versionClickCount < 7) {
                        if (toast != null) {
                            toast.cancel();
                        }
                        toast = Toast.makeText(context,
                                context.getString(R.string.show_hidden_toast, 7 - versionClickCount),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    return false;
                case R.string.export_key:
                    preferenceDialogs.showConfirmDialog(R.string.export_data, R.string.export_confirm,
                            R.string.export_key);
                    return false;
                case R.string.import_key:
                    preferenceDialogs.showConfirmDialog(R.string.import_data, R.string.import_confirm,
                            R.string.import_key);
                    return false;
                case R.string.auto_report_key:
                    if (sharedPrefs.getBoolean(context.getString(R.string.auto_report_key), false)) {
                        preferenceDialogs.showConfirmDialog(R.string.auto_report, R.string.auto_report_confirm,
                                R.string.auto_report_key);
                    }
                    return false;
                case R.string.ignore_battery_optimizations_key:
                    preferenceDialogs.showConfirmDialog(R.string.ignore_battery_optimizations,
                            R.string.ignore_battery_optimizations_description,
                            R.string.ignore_battery_optimizations_key);
                    return false;
                case R.string.enable_marking_key:
                    if (sharedPrefs.getBoolean(context.getString(R.string.enable_marking_key), false)) {
                        preferenceDialogs.showConfirmDialog(R.string.enable_marking, R.string.mark_confirm,
                                R.string.enable_marking_key);
                    }
                    return false;
                case R.string.offline_data_check_now_key:
                    preferenceActions.checkOfflineData();
                    return false;
                case R.string.offline_data_auto_upgrade_key:
                    preferenceActions.resetOfflineDataUpgradeWorker();
                    return false;
                case R.string.outgoing_window_position_key:
                    if (sharedPrefs.getBoolean(context.getString(R.string.outgoing_window_position_key),
                            false)) {
                        preferenceDialogs.showTextDialog(R.string.outgoing_window_position,
                                R.string.outgoing_window_position_message);
                    }
                    break;
            }

            return true;
        }
    }

    public static class PreferenceDialogs {

        private Context context;
        private SharedPreferences sharedPrefs;
        private Window window;
        private PreferenceActions preferenceActions;

        public PreferenceDialogs(Context context, SharedPreferences sharedPrefs, Window window, PreferenceActions preferenceActions) {
            this.context = context;
            this.sharedPrefs = sharedPrefs;
            this.window = window;
            this.preferenceActions = preferenceActions;
        }

        private void showSeekBarDialog(int keyId, final String bundleKey, int defaultValue,
                                       int max, int title, int textRes) {
            final String key = context.getString(keyId);
            int value = sharedPrefs.getInt(key, defaultValue);
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(context);
            builder.setTitle(context.getString(title));
            View layout = View.inflate(context, R.layout.dialog_seek, null);
            builder.setView(layout);

            final SeekBarCompat seekBar = layout.findViewById(R.id.seek_bar);
            seekBar.setMax(max);
            seekBar.setProgress(value);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress == 0) {
                        progress = 1;
                    }
                    window.sendData(bundleKey, progress, Window.Type.SETTING);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int value = seekBar.getProgress();
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putInt(key, value);
                    editor.apply();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    window.closeWindow();
                }
            });
            builder.show();

            window.showTextWindow(textRes, Window.Type.SETTING);
        }

        private void showApiDialog(int keyId, int title, final int url) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(context);
            builder.setTitle(context.getString(title));
            View layout = View.inflate(context, R.layout.dialog_edit, null);
            builder.setView(layout);

            final String key = context.getString(keyId);
            final EditText editText = layout.findViewById(R.id.text);
            editText.setText(sharedPrefs.getString(key, ""));

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = editText.getText().toString();
                    preferenceActions.findPreference(key).setSummary(Utils.Companion.mask(value));
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(key, value);
                    editor.apply();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.setNeutralButton(R.string.fetch, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(url))));
                }
            });
            builder.setCancelable(false);
            builder.show();
        }

        private void showRadioDialog(int keyId, int title, int listId, int defValue) {
            showRadioDialog(keyId, title, listId, defValue, 0);
        }

        private void showRadioDialog(int keyId, int title, int listId, int defValue,
                                     final int offset) {
            final String key = context.getString(keyId);
            final List<String> list = Arrays.asList(context.getResources().getStringArray(listId));
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(title));
            View layout = View.inflate(context, R.layout.dialog_radio, null);
            builder.setView(layout);
            final AlertDialog dialog = builder.create();

            final RadioGroup radioGroup = layout.findViewById(R.id.radio);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            for (String s : list) {
                RadioButton radioButton = new RadioButton(context);
                radioButton.setText(s);
                radioGroup.addView(radioButton, layoutParams);
            }

            RadioButton button =
                    ((RadioButton) radioGroup.getChildAt(
                            sharedPrefs.getInt(key, defValue) - offset));
            button.setChecked(true);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    int index = group.indexOfChild(group.findViewById(checkedId));
                    Preference preference = preferenceActions.findPreference(key);
                    if (preference != null) {
                        preference.setSummary(list.get(index));
                    }
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putInt(key, index + offset);
                    editor.apply();
                    dialog.dismiss();
                }
            });
            dialog.show();
        }

        private void showTextDialog(int title, int text) {
            showTextDialog(title, context.getString(text));
        }

        private void showTextDialog(int title, String text) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(context);
            builder.setTitle(context.getString(title));
            View layout = View.inflate(context, R.layout.dialog_text, null);
            builder.setView(layout);

            TextView textView = layout.findViewById(R.id.text);
            textView.setText(text);

            builder.setPositiveButton(R.string.ok, null);
            builder.show();
        }

        private void showConfirmDialog(int title, int text, final int key) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(context);
            builder.setTitle(context.getString(title));
            View layout = View.inflate(context, R.layout.dialog_text, null);
            builder.setView(layout);

            TextView textView = layout.findViewById(R.id.text);
            textView.setText(context.getString(text));
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preferenceActions.onConfirmCanceled(key);
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preferenceActions.onConfirmed(key);
                }
            });
            builder.show();
        }

        private void showCustomApiDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.custom_api));
            View layout = View.inflate(context, R.layout.dialog_custom_api, null);
            builder.setView(layout);

            final EditText apiUri = layout.findViewById(R.id.api_uri);
            final EditText apiKey = layout.findViewById(R.id.api_key);
            final String customApiKey = context.getString(R.string.custom_api_key);
            final String customApiUrl = context.getString(R.string.custom_api_url);
            apiUri.setText(sharedPrefs.getString(customApiUrl, ""));
            apiKey.setText(sharedPrefs.getString(customApiKey, ""));

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = apiUri.getText().toString();
                    String key = apiKey.getText().toString();
                    preferenceActions.findPreference(customApiUrl).setSummary(value);
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(customApiUrl, value);
                    editor.putString(customApiKey, key);
                    editor.apply();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.setNeutralButton(R.string.document, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.api_document_url))));
                }
            });
            builder.setCancelable(false);
            builder.show();
        }

        @SuppressWarnings("SameParameterValue")
        private void showEditDialog(int keyId, int title, final int defaultText, int hint) {
            showEditDialog(keyId, title, defaultText, hint, 0, 0);
        }

        private void showEditDialog(int keyId, int title, final int defaultText, int hint,
                                    final int help, final int helpText) {
            final String key = context.getString(keyId);
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(context);
            builder.setTitle(context.getString(title));
            View layout = View.inflate(context, R.layout.dialog_edit, null);
            builder.setView(layout);

            final EditText editText = layout.findViewById(R.id.text);
            editText.setText(sharedPrefs.getString(key, context.getString(defaultText)));
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            if (hint > 0) {
                editText.setHint(hint);
            }

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = editText.getText().toString();
                    if (value.isEmpty()) {
                        value = context.getString(defaultText);
                    }
                    preferenceActions.findPreference(key).setSummary(value);
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(key, value);
                    editor.apply();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);

            if (help != 0) {
                builder.setNeutralButton(help, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showTextDialog(help, helpText);
                    }
                });
            }

            builder.setCancelable(true);
            builder.show();
        }

    }
}
