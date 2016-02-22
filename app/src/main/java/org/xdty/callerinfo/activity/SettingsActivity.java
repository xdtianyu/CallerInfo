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
import android.graphics.Point;
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
        Preference winHeightPref;
        Preference textAlignPref;
        Preference winTransPref;
        Preference winPaddingPref;
        Preference apiTypePref;
        Preference customApiPref;
        Preference ignoreRegexPref;
        PreferenceScreen customDataPref;
        SwitchPreference ignoreContactPref;
        SwitchPreference outgoingPref;
        SwitchPreference crashPref;
        SwitchPreference chinesePref;
        SwitchPreference transBackPref;
        String baiduApiKey;
        String juheApiKey;
        String textSizeKey;
        String winHeightKey;
        String textAlignKey;
        String windowTransKey;
        String windowPaddingKey;
        String apiTypeKey;
        String ignoreContactKey;
        String outgoingKey;
        String crashKey;
        String chineseKey;
        String transBackKey;
        String customApiUrl;
        String customApiKey;
        String customDataKey;
        String ignoreRegexKey;

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
        String hangupKeywordKey;
        Preference hangupKeywordPref;
        String hangupGeoKeywordKey;
        Preference hangupGeoKeywordPref;
        String hangupNumberKeywordKey;
        Preference hangupNumberKeywordPref;

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
        private Point mPoint;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            WindowManager mWindowManager = (WindowManager) getActivity().
                    getSystemService(Context.WINDOW_SERVICE);
            Display display = mWindowManager.getDefaultDisplay();
            mPoint = new Point();
            display.getSize(mPoint);

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

            winHeightKey = getString(R.string.window_height_key);
            winHeightPref = findPreference(winHeightKey);
            winHeightPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSeekBarDialog(winHeightKey, FloatWindow.WINDOW_HEIGHT, mPoint.y / 8,
                            mPoint.y / 4, R.string.window_height, R.string.window_height_message);
                    return true;
                }
            });

            final List<String> alignList = Arrays.asList(
                    getResources().getStringArray(R.array.align_type));

            textAlignKey = getString(R.string.window_text_alignment_key);
            textAlignPref = findPreference(textAlignKey);
            textAlignPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showRadioDialog(textAlignKey, R.string.window_text_alignment, alignList, 1);
                    return true;
                }
            });
            textAlignPref.setSummary(alignList.get(sharedPrefs.getInt(textAlignKey, 1)));

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

            windowPaddingKey = getString(R.string.window_text_padding_key);
            winPaddingPref = findPreference(windowPaddingKey);
            winPaddingPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSeekBarDialog(windowPaddingKey, FloatWindow.TEXT_PADDING, 0, mPoint.x / 2,
                            R.string.window_text_padding, R.string.text_padding);
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
                    showRadioDialog(apiTypeKey, R.string.api_type, apiList, 0);
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

            ignoreRegexKey = getString(R.string.ignore_regex_key);
            ignoreRegexPref = findPreference(ignoreRegexKey);
            ignoreRegexPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showEditDialog(ignoreRegexKey, R.string.ignore_regex, R.string.empty_string,
                            R.string.ignore_regex_hint, R.string.example, R.string.regex_example);
                    return false;
                }
            });
            String regex = sharedPrefs.getString(ignoreRegexKey, "");
            if (!regex.isEmpty()) {
                ignoreRegexPref.setSummary(regex);
            }

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
                callLogPref = (SwitchPreference) findPreference(callLogKey);
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

                hangupKeywordKey = getString(R.string.hangup_keyword_key);
                String keyword = sharedPrefs.getString(hangupKeywordKey, "");
                if (keyword.isEmpty()) {
                    keyword = getString(R.string.hangup_keyword_summary);
                }

                hangupKeywordPref = findPreference(hangupKeywordKey);
                hangupKeywordPref.setSummary(keyword);
                hangupKeywordPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showEditDialog(hangupKeywordKey, R.string.hangup_keyword,
                                R.string.hangup_keyword_default, R.string.hangup_keyword_hint);
                        return false;
                    }
                });

                hangupGeoKeywordKey = getString(R.string.hangup_geo_keyword_key);
                String geoKeyword = sharedPrefs.getString(hangupGeoKeywordKey, "");
                if (geoKeyword.isEmpty()) {
                    geoKeyword = getString(R.string.hangup_geo_keyword_summary);
                }

                hangupGeoKeywordPref = findPreference(hangupGeoKeywordKey);
                hangupGeoKeywordPref.setSummary(geoKeyword);
                hangupGeoKeywordPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showEditDialog(hangupGeoKeywordKey, R.string.hangup_geo_keyword,
                                R.string.empty_string, R.string.hangup_keyword_hint);
                        return false;
                    }
                });

                hangupNumberKeywordKey = getString(R.string.hangup_number_keyword_key);
                String numberKeyword = sharedPrefs.getString(hangupNumberKeywordKey, "");
                if (numberKeyword.isEmpty()) {
                    numberKeyword = getString(R.string.hangup_number_keyword_summary);
                }

                hangupNumberKeywordPref = findPreference(hangupNumberKeywordKey);
                hangupNumberKeywordPref.setSummary(numberKeyword);
                hangupNumberKeywordPref.setOnPreferenceClickListener(
                        new OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                showEditDialog(hangupNumberKeywordKey,
                                        R.string.hangup_number_keyword,
                                        R.string.empty_string, R.string.hangup_keyword_hint);
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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

                content.setPadding((int) dpToPx(16), height, (int) dpToPx(16), 0);

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
            View layout = View.inflate(getActivity(), R.layout.dialog_seek, null);
            builder.setView(layout);

            final SeekBarCompat seekBar = (SeekBarCompat) layout.findViewById(R.id.seek_bar);
            seekBar.setMax(max);
            seekBar.setProgress(value);
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
            View layout = View.inflate(getActivity(), R.layout.dialog_edit, null);
            builder.setView(layout);

            final EditText editText = (EditText) layout.findViewById(R.id.text);
            editText.setText(sharedPrefs.getString(key, ""));

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = editText.getText().toString();
                    findPreference(key).setSummary(mask(value));
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(key, value);
                    editor.apply();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.setNeutralButton(R.string.fetch, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(url))));
                }
            });
            builder.setCancelable(false);
            builder.show();
        }

        private void showRadioDialog(final String key, int title, final List<String> list,
                int defValue) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(title));
            View layout = View.inflate(getActivity(), R.layout.dialog_radio, null);
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

            RadioButton button =
                    ((RadioButton) radioGroup.getChildAt(sharedPrefs.getInt(key, defValue)));
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
            View layout = View.inflate(getActivity(), R.layout.dialog_text, null);
            builder.setView(layout);

            TextView textView = (TextView) layout.findViewById(R.id.text);
            textView.setText(getString(text));

            builder.setPositiveButton(R.string.ok, null);
            builder.show();
        }

        private void showCustomApiDialog() {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.custom_api));
            View layout = View.inflate(getActivity(), R.layout.dialog_custom_api, null);
            builder.setView(layout);

            final EditText apiUri = (EditText) layout.findViewById(R.id.api_uri);
            final EditText apiKey = (EditText) layout.findViewById(R.id.api_key);
            apiUri.setText(sharedPrefs.getString(customApiUrl, ""));
            apiKey.setText(sharedPrefs.getString(customApiKey, ""));

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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
            builder.setNegativeButton(R.string.cancel, null);
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

        private void showEditDialog(final String key, int title, final int defaultText, int hint) {
            showEditDialog(key, title, defaultText, hint, 0, 0);
        }

        private void showEditDialog(final String key, int title, final int defaultText, int hint,
                final int help, final int helpText) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(title));
            View layout = View.inflate(getActivity(), R.layout.dialog_edit, null);
            builder.setView(layout);

            final EditText editText = (EditText) layout.findViewById(R.id.text);
            editText.setText(sharedPrefs.getString(key, getString(defaultText)));
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            if (hint > 0) {
                editText.setHint(hint);
            }

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = editText.getText().toString();
                    if (value.isEmpty()) {
                        value = getString(defaultText);
                    }
                    findPreference(key).setSummary(value);
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

        private float dpToPx(float dp) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                    getActivity().getResources().getDisplayMetrics());
        }
    }
}