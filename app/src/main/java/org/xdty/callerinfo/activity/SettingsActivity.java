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
import java.util.HashMap;
import java.util.List;

import app.minimize.com.seek_bar_compat.SeekBarCompat;

import static org.xdty.callerinfo.utils.Utils.closeWindow;
import static org.xdty.callerinfo.utils.Utils.mask;
import static org.xdty.callerinfo.utils.Utils.sendData;
import static org.xdty.callerinfo.utils.Utils.showTextWindow;

public class SettingsActivity extends AppCompatActivity {

    private final static String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.action_settings);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment
            implements OnPreferenceClickListener, ServiceConnection {

        public final static int REQUEST_CODE_CONTACTS_PERMISSION = 1003;
        public final static int REQUEST_CODE_OUTGOING_PERMISSION = 1004;

        private final static int SUMMARY_FLAG_NORMAL = 0x00000001;
        private final static int SUMMARY_FLAG_MASK = 0x00000002;
        private final static int SUMMARY_FLAG_NULL = 0x00000004;

        private final Intent mPluginIntent = new Intent().setComponent(new ComponentName(
                "org.xdty.callerinfo.plugin",
                "org.xdty.callerinfo.plugin.PluginService"));

        int versionClickCount;
        Toast toast;
        SharedPreferences sharedPrefs;
        private IPluginService mPluginService;

        private Point mPoint;
        private HashMap<String, Integer> keyMap = new HashMap<>();
        private HashMap<String, Preference> prefMap = new HashMap<>();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            sharedPrefs = getPreferenceManager().getSharedPreferences();

            WindowManager mWindowManager = (WindowManager) getActivity().
                    getSystemService(Context.WINDOW_SERVICE);
            Display display = mWindowManager.getDefaultDisplay();
            mPoint = new Point();
            display.getSize(mPoint);

            bindPreference(R.string.baidu_api_key, true);
            bindPreference(R.string.juhe_api_key, true);
            bindPreference(R.string.window_text_size_key);
            bindPreference(R.string.window_height_key);
            bindPreferenceList(R.string.window_text_alignment_key, R.array.align_type, 1);
            bindPreference(R.string.window_transparent_key);
            bindPreference(R.string.window_text_padding_key);
            bindPreferenceList(R.string.api_type_key, R.array.api_type, 0);
            bindPreference(R.string.ignore_known_contact_key);
            bindPreference(R.string.display_on_outgoing_key);
            bindPreference(R.string.catch_crash_key);
            bindPreference(R.string.ignore_regex_key, false);
            bindPreference(R.string.custom_api_url);

            bindVersionPreference();
            bindPluginPreference();
        }

        private void bindVersionPreference() {

            bindPreference(R.string.version_key);
            Preference version = findPreference(getString(R.string.version_key));
            String versionString = BuildConfig.VERSION_NAME;
            if (BuildConfig.DEBUG) {
                versionString += "." + BuildConfig.BUILD_TYPE;
            }
            version.setSummary(versionString);

            boolean isShowHidden =
                    sharedPrefs.getBoolean(getString(R.string.show_hidden_setting_key), false);

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
                    (PreferenceScreen) findPreference(getString(R.string.plugin_key));
            pluginPref.setEnabled(false);
            pluginPref.setSummary(getString(R.string.plugin_not_started));
            if (Utils.isAppInstalled(getActivity(), getString(R.string.plugin_package_name))) {
                bindPluginService();

                bindPreference(R.string.auto_hangup_key);
                bindPreference(R.string.add_call_log_key);

                bindPreference(R.string.hangup_keyword_key, R.string.hangup_keyword_summary);
                bindPreference(R.string.hangup_geo_keyword_key,
                        R.string.hangup_geo_keyword_summary);
                bindPreference(R.string.hangup_number_keyword_key,
                        R.string.hangup_number_keyword_summary);
            } else {
                removePreference(R.string.advanced_key, R.string.plugin_key);
            }
        }

        private void enablePluginPreference() {
            PreferenceScreen pluginPref =
                    (PreferenceScreen) findPreference(getString(R.string.plugin_key));
            pluginPref.setEnabled(true);
            pluginPref.setSummary("");
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
                getActivity().bindService(mPluginIntent, this, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void unBindPluginService() {
            try {
                getActivity().unbindService(this);
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

        @Override
        public Preference findPreference(CharSequence key) {
            Preference pref = prefMap.get(key.toString());
            if (pref == null) {
                pref = super.findPreference(key);
            }
            return pref;
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
                        setChecked(R.string.ignore_known_contact_key, false);
                    }
                    break;
                case REQUEST_CODE_OUTGOING_PERMISSION:
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        setChecked(R.string.display_on_outgoing_key, false);
                    }
                    break;
                default:
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }

        private void showSeekBarDialog(int keyId, final String bundleKey, int defaultValue,
                int max, int title, int textRes) {
            final String key = getString(keyId);
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

        private void showApiDialog(int keyId, int title, final int url) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(title));
            View layout = View.inflate(getActivity(), R.layout.dialog_edit, null);
            builder.setView(layout);

            final String key = getString(keyId);
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

        private void showRadioDialog(int keyId, int title, final List<String> list,
                int defValue) {
            final String key = getString(keyId);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.custom_api));
            View layout = View.inflate(getActivity(), R.layout.dialog_custom_api, null);
            builder.setView(layout);

            final EditText apiUri = (EditText) layout.findViewById(R.id.api_uri);
            final EditText apiKey = (EditText) layout.findViewById(R.id.api_key);
            final String customApiKey = getString(R.string.custom_api_key);
            final String customApiUrl = getString(R.string.custom_api_url);
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

        @SuppressWarnings("SameParameterValue")
        private void showEditDialog(int keyId, int title, final int defaultText, int hint) {
            showEditDialog(keyId, title, defaultText, hint, 0, 0);
        }

        private void showEditDialog(int keyId, int title, final int defaultText, int hint,
                final int help, final int helpText) {
            final String key = getString(keyId);
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

        @SuppressWarnings("SameParameterValue")
        private float dpToPx(float dp) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                    getActivity().getResources().getDisplayMetrics());
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            int keyId = getKeyId(preference.getKey());
            switch (keyId) {
                case R.string.baidu_api_key:
                    showApiDialog(R.string.baidu_api_key, R.string.custom_bd_api_key,
                            R.string.baidu_api_url);
                    break;
                case R.string.juhe_api_key:
                    showApiDialog(R.string.juhe_api_key, R.string.custom_jh_api_key,
                            R.string.juhe_api_url);
                    break;
                case R.string.window_text_size_key:
                    showSeekBarDialog(R.string.window_text_size_key, FloatWindow.TEXT_SIZE, 20, 60,
                            R.string.window_text_size, R.string.text_size);
                    break;
                case R.string.window_height_key:
                    showSeekBarDialog(R.string.window_height_key, FloatWindow.WINDOW_HEIGHT,
                            mPoint.y / 8, mPoint.y / 4, R.string.window_height,
                            R.string.window_height_message);
                    break;
                case R.string.window_text_alignment_key:
                    List<String> alignList = Arrays.asList(
                            getResources().getStringArray(R.array.align_type));
                    showRadioDialog(R.string.window_text_alignment_key,
                            R.string.window_text_alignment, alignList, 1);
                    break;
                case R.string.window_transparent_key:
                    showSeekBarDialog(R.string.window_transparent_key, FloatWindow.WINDOW_TRANS, 80,
                            100, R.string.window_transparent, R.string.text_transparent);
                    break;
                case R.string.window_text_padding_key:
                    showSeekBarDialog(R.string.window_text_padding_key, FloatWindow.TEXT_PADDING, 0,
                            mPoint.x / 2,
                            R.string.window_text_padding, R.string.text_padding);
                    break;
                case R.string.api_type_key:
                    List<String> apiList = Arrays.asList(
                            getResources().getStringArray(R.array.api_type));
                    showRadioDialog(R.string.api_type_key, R.string.api_type, apiList, 0);
                    break;
                case R.string.ignore_known_contact_key:
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
                case R.string.display_on_outgoing_key:
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
                case R.string.catch_crash_key:
                    if (sharedPrefs.getBoolean(getString(R.string.catch_crash_key), false)) {
                        showTextDialog(R.string.catch_crash, R.string.catch_crash_message);
                    }
                    break;
                case R.string.ignore_regex_key:
                    showEditDialog(R.string.ignore_regex_key, R.string.ignore_regex,
                            R.string.empty_string,
                            R.string.ignore_regex_hint, R.string.example, R.string.regex_example);
                    return false;
                case R.string.hangup_keyword_key:
                    showEditDialog(R.string.hangup_keyword_key, R.string.hangup_keyword,
                            R.string.hangup_keyword_default, R.string.hangup_keyword_hint);
                    return false;
                case R.string.hangup_number_keyword_key:
                    showEditDialog(R.string.hangup_number_keyword_key,
                            R.string.hangup_number_keyword,
                            R.string.empty_string, R.string.hangup_keyword_hint);
                    return false;
                case R.string.hangup_geo_keyword_key:
                    showEditDialog(R.string.hangup_geo_keyword_key, R.string.hangup_geo_keyword,
                            R.string.empty_string, R.string.hangup_keyword_hint);
                    return false;
                case R.string.custom_api_url:
                    showCustomApiDialog();
                    return false;
                case R.string.auto_hangup_key:
                    try {
                        mPluginService.checkCallPermission();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return false;
                case R.string.add_call_log_key:
                    try {
                        mPluginService.checkCallLogPermission();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return false;
                case R.string.version_key:
                    versionClickCount++;
                    if (versionClickCount == 7) {
                        sharedPrefs.edit().putBoolean(getString(R.string.show_hidden_setting_key),
                                true).apply();

                        addPreference(R.string.advanced_key, R.string.custom_data_key);
                        addPreference(R.string.advanced_key, R.string.force_chinese_key);
                        addPreference(R.string.float_window_key,
                                R.string.window_trans_back_only_key);
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

            return true;
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
            String key = getString(keyId);
            Preference preference = findPreference(key);
            List<String> apiList = Arrays.asList(getResources().getStringArray(arrayId));
            preference.setOnPreferenceClickListener(this);
            preference.setSummary(apiList.get(sharedPrefs.getInt(key, index)));
            keyMap.put(key, keyId);
            prefMap.put(key, preference);
        }

        private void bindPreference(int keyId, int summaryFlags, int summaryId) {
            String key = getString(keyId);
            Preference preference = findPreference(key);
            preference.setOnPreferenceClickListener(this);

            if ((summaryFlags & SUMMARY_FLAG_NORMAL) == SUMMARY_FLAG_NORMAL) {
                String defaultSummary = summaryId == 0 ? "" : getString(summaryId);
                String summary = sharedPrefs.getString(key, defaultSummary);

                if (summary.isEmpty() && !defaultSummary.isEmpty()) {
                    summary = defaultSummary;
                }

                boolean mask = ((summaryFlags & SUMMARY_FLAG_MASK) == SUMMARY_FLAG_MASK);
                preference.setSummary(mask ? mask(summary) : summary);
            }
            keyMap.put(key, keyId);
            prefMap.put(key, preference);
        }

        private int getKeyId(String key) {
            return keyMap.get(key);
        }

        private void removePreference(int parent, int child) {
            String childKey = getString(child);
            String parentKey = getString(parent);
            Preference preference = findPreference(childKey);
            PreferenceCategory category = (PreferenceCategory) findPreference(parentKey);
            category.removePreference(preference);
            prefMap.put(childKey, preference);
            prefMap.put(parentKey, category);
        }

        private void addPreference(int parent, int child) {
            String childKey = getString(child);
            String parentKey = getString(parent);
            Preference preference = findPreference(childKey);
            PreferenceCategory category = (PreferenceCategory) findPreference(parentKey);
            category.addPreference(preference);
            keyMap.put(childKey, child);
            keyMap.put(parentKey, parent);
            prefMap.put(childKey, preference);
            prefMap.put(parentKey, category);
        }

        private void setChecked(int key, boolean checked) {
            SwitchPreference preference = (SwitchPreference) findPreference(getString(key));
            preference.setChecked(checked);
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
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setChecked(R.string.auto_hangup_key, success);
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
                                setChecked(R.string.add_call_log_key, success);
                            }
                        });
                    }
                });
                enablePluginPreference();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: " + name.toString());
            mPluginService = null;
        }
    }
}