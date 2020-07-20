package org.xdty.callerinfo.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.appbar.AppBarLayout;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.settings.PluginBinder;
import org.xdty.callerinfo.settings.PluginStatus;
import org.xdty.callerinfo.settings.PreferenceActions;
import org.xdty.callerinfo.settings.PreferenceBinder;
import org.xdty.callerinfo.settings.PreferenceClicker;
import org.xdty.callerinfo.settings.PreferenceDelegate;
import org.xdty.callerinfo.settings.PreferenceDialogs;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Toasts;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.callerinfo.utils.Window;

import javax.inject.Inject;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SettingsFragment extends PreferenceFragment implements PreferenceActions {

    private final static String TAG = SettingsFragment.class.getSimpleName();

    private final static String PLUGIN_SETTING = "org.xdty.callerinfo.action.PLUGIN_SETTING";


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

        PreferenceDelegate delegate = new PreferenceDelegate();
        PreferenceClicker clicker = new PreferenceClicker(getActivity(), sharedPrefs, preferenceDialogs, pluginBinder, this);

        delegate.setActions(this);
        delegate.setClicker(clicker);
        delegate.setDialogs(preferenceDialogs);

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
}
