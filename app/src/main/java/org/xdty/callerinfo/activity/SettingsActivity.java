package org.xdty.callerinfo.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jenzz.materialpreference.PreferenceCategory;
import com.jenzz.materialpreference.SwitchPreference;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.plugin.IPluginService;
import org.xdty.callerinfo.plugin.IPluginServiceCallback;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.utils.Utils;

import java.util.Arrays;
import java.util.List;

import app.minimize.com.seek_bar_compat.SeekBarCompat;

import static org.xdty.callerinfo.utils.Utils.closeWindow;
import static org.xdty.callerinfo.utils.Utils.mask;
import static org.xdty.callerinfo.utils.Utils.sendData;
import static org.xdty.callerinfo.utils.Utils.showTextWindow;

public class SettingsActivity extends AppCompatActivity {

    public final static String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment {

        public final static int REQUEST_CODE_CONTACTS_PERMISSION = 1003;
        public final static int REQUEST_CODE_OUTGOING_PERMISSION = 1004;
        SharedPreferences sharedPrefs;
        Preference bdApiPreference;
        Preference jhApiPreference;
        Preference textSizePref;
        Preference winTransPref;
        Preference apiTypePref;
        Preference customApiPref;
        PreferenceScreen customDataPref;
        SwitchPreference ignoreContactPref;
        SwitchPreference outgoingPref;
        SwitchPreference crashPref;
        SwitchPreference chinesePref;
        SwitchPreference transBackPref;
        String baiduApiKey;
        String juheApiKey;
        String textSizeKey;
        String windowTransKey;
        String apiTypeKey;
        String ignoreContactKey;
        String outgoingKey;
        String crashKey;
        String chineseKey;
        String transBackKey;
        String customApiUrl;
        String customApiKey;
        String customDataKey;

        PreferenceCategory advancedPref;
        PreferenceCategory aboutPref;
        PreferenceCategory floatWindowPref;
        Preference developerPref;

        String pluginKey;
        PreferenceScreen pluginPref;
        String hangupKey;
        SwitchPreference hangupPref;
        String callLogKey;
        SwitchPreference callLogPref;

        int versionClickCount;
        Toast toast;

        private IPluginService mPluginService;
        private ServiceConnection mConnection = new ServiceConnection() {
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
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hangupPref.setChecked(success);
                                }
                            });
                        }

                        @Override
                        public void onCallLogPermissionResult(final boolean success) throws
                                RemoteException {
                            Log.d(TAG, "onCallLogPermissionResult: " + success);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callLogPref.setChecked(success);
                                }
                            });
                        }
                    });
                    pluginPref.setEnabled(true);
                    pluginPref.setSummary("");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected: " + name.toString());
                mPluginService = null;
            }
        };
        private Intent mPluginIntent = new Intent().setComponent(new ComponentName(
                "org.xdty.callerinfo.plugin",
                "org.xdty.callerinfo.plugin.PluginService"));

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            sharedPrefs = getPreferenceManager().getSharedPreferences();

            Preference version = findPreference(getString(R.string.version_key));
            String versionString = BuildConfig.VERSION_NAME;
            if (BuildConfig.DEBUG) {
                versionString += "." + BuildConfig.BUILD_TYPE;
            }
            version.setSummary(versionString);

            baiduApiKey = getString(R.string.baidu_api_key);

            bdApiPreference = findPreference(baiduApiKey);
            bdApiPreference.setSummary(mask(sharedPrefs.getString(baiduApiKey, "")));

            bdApiPreference.setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            showApiDialog(baiduApiKey, R.string.custom_bd_api_key,
                                    R.string.baidu_api_url);
                            return true;
                        }
                    });

            juheApiKey = getString(R.string.juhe_api_key);

            jhApiPreference = findPreference(juheApiKey);
            jhApiPreference.setSummary(mask(sharedPrefs.getString(juheApiKey, "")));

            jhApiPreference.setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            showApiDialog(juheApiKey, R.string.custom_jh_api_key,
                                    R.string.juhe_api_url);
                            return true;
                        }
                    });

            textSizeKey = getString(R.string.window_text_size_key);

            textSizePref = findPreference(textSizeKey);
            textSizePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSeekBarDialog(textSizeKey, FloatWindow.TEXT_SIZE, 20, 60,
                            R.string.window_text_size, R.string.text_size);
                    return true;
                }
            });

            windowTransKey = getString(R.string.window_transparent_key);
            winTransPref = findPreference(windowTransKey);
            winTransPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSeekBarDialog(windowTransKey, FloatWindow.WINDOW_TRANS, 80, 100,
                            R.string.window_transparent, R.string.text_transparent);
                    return true;
                }
            });

            apiTypeKey = getString(R.string.api_type_key);
            apiTypePref = findPreference(apiTypeKey);

            final List<String> apiList = Arrays.asList(
                    getResources().getStringArray(R.array.api_type));

            apiTypePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showRadioDialog(apiTypeKey, R.string.api_type, apiList);
                    return true;
                }
            });
            apiTypePref.setSummary(apiList.get(sharedPrefs.getInt(apiTypeKey, 0)));

            ignoreContactKey = getString(R.string.ignore_known_contact_key);
            ignoreContactPref = (SwitchPreference) findPreference(ignoreContactKey);
            ignoreContactPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int res = getActivity().checkSelfPermission(
                                Manifest.permission.READ_CONTACTS);
                        if (res != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                                    REQUEST_CODE_CONTACTS_PERMISSION);
                            return true;
                        }
                    }
                    return false;
                }
            });

            outgoingKey = getString(R.string.display_on_outgoing_key);
            outgoingPref = (SwitchPreference) findPreference(outgoingKey);
            outgoingPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int res = getActivity().checkSelfPermission(
                                Manifest.permission.PROCESS_OUTGOING_CALLS);
                        if (res != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(
                                    new String[]{Manifest.permission.PROCESS_OUTGOING_CALLS},
                                    REQUEST_CODE_OUTGOING_PERMISSION);
                            return true;
                        }
                    }
                    return false;
                }
            });

            crashKey = getString(R.string.catch_crash_key);
            crashPref = (SwitchPreference) findPreference(crashKey);
            crashPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (sharedPrefs.getBoolean(crashKey, false)) {
                        showTextDialog(R.string.catch_crash, R.string.catch_crash_message);
                    }
                    return false;
                }
            });

            chineseKey = getString(R.string.force_chinese_key);
            chinesePref = (SwitchPreference) findPreference(chineseKey);

            transBackKey = getString(R.string.window_trans_back_only_key);
            transBackPref = (SwitchPreference) findPreference(transBackKey);

            customApiUrl = getString(R.string.custom_api_url);
            customApiKey = getString(R.string.custom_api_key);
            customApiPref = findPreference(customApiUrl);
            customApiPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showCustomApiDialog();
                    return false;
                }
            });

            customDataKey = getString(R.string.custom_data_key);
            customDataPref = (PreferenceScreen) findPreference(customDataKey);

            final String showHiddenKey = getString(R.string.show_hidden_setting_key);
            boolean isShowHidden = sharedPrefs.getBoolean(showHiddenKey, false);
            advancedPref = (PreferenceCategory) findPreference(getString(R.string.advanced_key));
            if (!isShowHidden) {

                aboutPref = (PreferenceCategory) findPreference(getString(R.string.about_key));
                floatWindowPref =
                        (PreferenceCategory) findPreference(getString(R.string.float_window_key));
                developerPref = findPreference(getString(R.string.developer_key));

                advancedPref.removePreference(customDataPref);
                advancedPref.removePreference(chinesePref);
                aboutPref.removePreference(developerPref);
                floatWindowPref.removePreference(transBackPref);

                versionClickCount = 0;
                version.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        versionClickCount++;
                        if (versionClickCount == 7) {
                            sharedPrefs.edit().putBoolean(showHiddenKey, true).apply();

                            advancedPref.addPreference(customDataPref);
                            advancedPref.addPreference(chinesePref);
                            aboutPref.addPreference(developerPref);
                            floatWindowPref.addPreference(transBackPref);
                        }
                        if (versionClickCount > 3 && versionClickCount < 7) {
                            if (toast != null) {
                                toast.cancel();
                            }
                            toast = Toast.makeText(getActivity(),
                                    getString(R.string.show_hidden_toast, 7 - versionClickCount),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        return false;
                    }
                });
            }

            pluginKey = getString(R.string.plugin_key);
            pluginPref = (PreferenceScreen) findPreference(pluginKey);
            pluginPref.setEnabled(false);
            pluginPref.setSummary(getString(R.string.plugin_not_started));
            if (Utils.isAppInstalled(getActivity(), getString(R.string.plugin_package_name))) {
                bindPluginService();

                hangupKey = getString(R.string.auto_hangup_key);
                callLogKey = getString(R.string.add_call_log_key);
                hangupPref = (SwitchPreference) findPreference(hangupKey);
                callLogPref = (SwitchPreference) findPreference(callLogKey);
                hangupPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            mPluginService.checkCallPermission();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                });
                callLogPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            mPluginService.checkCallLogPermission();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                });
            } else {
                advancedPref.removePreference(pluginPref);
            }
        }

        @Override
        public void onDestroy() {
            if (Utils.isAppInstalled(getActivity(), getString(R.string.plugin_package_name))) {
                unBindPluginService();
            }
            super.onDestroy();
        }

        private void bindPluginService() {
            try {
                getActivity().startService(mPluginIntent);
                getActivity().bindService(mPluginIntent, mConnection, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void unBindPluginService() {
            try {
                getActivity().unbindService(mConnection);
                getActivity().stopService(mPluginIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
            final Dialog dialog = preferenceScreen.getDialog();

            AppBarLayout appBarLayout;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                LinearLayout root =
                        (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
                appBarLayout = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(
                        R.layout.settings_toolbar, root, false);
                root.addView(appBarLayout, 0);
            } else {
                ViewGroup root = (ViewGroup) dialog.findViewById(android.R.id.content);
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

                content.setPadding(0, height, 0, 0);

                root.addView(content);
                root.addView(appBarLayout);
            }

            Toolbar toolbar = (Toolbar) appBarLayout.findViewById(R.id.toolbar);
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
                case REQUEST_CODE_CONTACTS_PERMISSION:
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        ignoreContactPref.setChecked(false);
                    }
                    break;
                case REQUEST_CODE_OUTGOING_PERMISSION:
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        outgoingPref.setChecked(false);
                    }
                    break;
                default:
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }

        private void showSeekBarDialog(final String key, final String bundleKey, int defaultValue,
                int max, int title, int textRes) {
            int value = sharedPrefs.getInt(key, defaultValue);
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(title));
            View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_seek, null);
            builder.setView(layout);

            final SeekBarCompat seekBar = (SeekBarCompat) layout.findViewById(R.id.seek_bar);
            seekBar.setProgress(value);
            seekBar.setMax(max);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress == 0) {
                        progress = 1;
                    }
                    sendData(getActivity(), bundleKey, progress, FloatWindow.SETTING_FRONT);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int value = seekBar.getProgress();
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putInt(key, value);
                    editor.apply();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    closeWindow(getActivity());
                }
            });
            builder.show();

            showTextWindow(getActivity(), textRes, FloatWindow.SETTING_FRONT);
        }

        private void showApiDialog(final String key, int title, final int url) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(title));
            View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_edit, null);
            builder.setView(layout);

            final EditText editText = (EditText) layout.findViewById(R.id.text);
            editText.setText(sharedPrefs.getString(key, ""));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = editText.getText().toString();
                    findPreference(key).setSummary(mask(value));
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(key, value);
                    editor.apply();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setNeutralButton(R.string.fetch, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(url))));
                }
            });
            builder.setCancelable(false);
            builder.show();
        }

        private void showRadioDialog(final String key, int title, final List<String> list) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(title));
            View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_radio, null);
            builder.setView(layout);
            final AlertDialog dialog = builder.create();

            final RadioGroup radioGroup = (RadioGroup) layout.findViewById(R.id.radio);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            for (String s : list) {
                RadioButton radioButton = new RadioButton(getActivity());
                radioButton.setText(s);
                radioGroup.addView(radioButton, layoutParams);
            }

            RadioButton button = ((RadioButton) radioGroup.getChildAt(sharedPrefs.getInt(key, 0)));
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
                    findPreference(key).setSummary(list.get(index));
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putInt(key, index);
                    editor.apply();
                    dialog.dismiss();
                }
            });
            dialog.show();
        }

        private void showTextDialog(int title, int text) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(title));
            View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_text, null);
            builder.setView(layout);

            TextView textView = (TextView) layout.findViewById(R.id.text);
            textView.setText(getString(text));

            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }

        private void showCustomApiDialog() {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.custom_api));
            View layout =
                    getActivity().getLayoutInflater().inflate(R.layout.dialog_custom_api, null);
            builder.setView(layout);

            final EditText apiUri = (EditText) layout.findViewById(R.id.api_uri);
            final EditText apiKey = (EditText) layout.findViewById(R.id.api_key);
            apiUri.setText(sharedPrefs.getString(customApiUrl, ""));
            apiKey.setText(sharedPrefs.getString(customApiKey, ""));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = apiUri.getText().toString();
                    String key = apiKey.getText().toString();
                    findPreference(customApiUrl).setSummary(value);
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(customApiUrl, value);
                    editor.putString(customApiKey, key);
                    editor.apply();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setNeutralButton(R.string.document, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.api_document_url))));
                }
            });
            builder.setCancelable(false);
            builder.show();
        }
    }
}