package org.xdty.callerinfo.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jenzz.materialpreference.PreferenceCategory;
import com.jenzz.materialpreference.SwitchPreference;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.service.FloatWindow;

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

        PreferenceCategory advancedPref;
        PreferenceCategory aboutPref;
        PreferenceCategory floatWindowPref;
        Preference developerPref;

        int versionClickCount;
        Toast toast;

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

            final String showHiddenKey = getString(R.string.show_hidden_setting_key);
            boolean isShowHidden = sharedPrefs.getBoolean(showHiddenKey, false);

            if (!isShowHidden) {
                advancedPref =
                        (PreferenceCategory) findPreference(getString(R.string.advanced_key));
                aboutPref = (PreferenceCategory) findPreference(getString(R.string.about_key));
                floatWindowPref =
                        (PreferenceCategory) findPreference(getString(R.string.float_window_key));
                developerPref = findPreference(getString(R.string.developer_key));

                advancedPref.removePreference(bdApiPreference);
                advancedPref.removePreference(jhApiPreference);
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

                            advancedPref.addPreference(bdApiPreference);
                            advancedPref.addPreference(jhApiPreference);
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
    }
}