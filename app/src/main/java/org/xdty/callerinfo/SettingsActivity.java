package org.xdty.callerinfo;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

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

        SharedPreferences sharedPrefs;
        Preference apiPreference;
        Preference textSizePref;
        Preference winTransPref;
        String baiduApiKey;
        String textSizeKey;
        String windowTransKey;

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

            apiPreference = findPreference(baiduApiKey);
            final String apiKey = sharedPrefs.getString(baiduApiKey, "");
            apiPreference.setSummary(mask(apiKey));

            apiPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showApiDialog();
                    return true;
                }
            });

            textSizeKey = getString(R.string.window_text_size_key);

            textSizePref = findPreference(textSizeKey);
            textSizePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSeekBarDialog(textSizeKey, FloatWindow.TEXT_SIZE, 25, 60,
                            R.string.window_text_size, R.string.text_size);
                    return true;
                }
            });

            windowTransKey = getString(R.string.window_transparent_key);
            winTransPref = findPreference(windowTransKey);
            winTransPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSeekBarDialog(windowTransKey, FloatWindow.WINDOW_TRANS, 100, 100,
                            R.string.window_transparent, R.string.text_transparent);
                    return true;
                }
            });
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
                    sendData(getActivity(), bundleKey, progress);
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

            showTextWindow(getActivity(), textRes);
        }

        private void showApiDialog() {
            String apiKey = sharedPrefs.getString(baiduApiKey, "");
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.custom_api_key));
            View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_edit, null);
            builder.setView(layout);

            final EditText editText = (EditText) layout.findViewById(R.id.text);
            editText.setText(apiKey);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = editText.getText().toString();
                    apiPreference.setSummary(mask(value));
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(baiduApiKey, value);
                    editor.apply();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setCancelable(false);
            builder.show();
        }
    }
}