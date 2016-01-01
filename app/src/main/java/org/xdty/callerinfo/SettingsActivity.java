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

import static org.xdty.callerinfo.utils.Utils.mask;

public class SettingsActivity extends AppCompatActivity {

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

        SharedPreferences sharedPreferences;
        Preference apiPreference;
        String baiduApi;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            sharedPreferences = getPreferenceManager().getSharedPreferences();

            Preference version = findPreference(getString(R.string.version_key));
            String versionString = BuildConfig.VERSION_NAME;
            if (BuildConfig.DEBUG) {
                versionString += "." + BuildConfig.BUILD_TYPE;
            }
            version.setSummary(versionString);

            baiduApi = getString(R.string.baidu_api_key);

            apiPreference = findPreference(baiduApi);
            final String apiKey = sharedPreferences.getString(baiduApi, "");
            apiPreference.setSummary(mask(apiKey));

            apiPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showDialog();
                    return true;
                }
            });
        }

        private void showDialog() {
            String apiKey = sharedPreferences.getString(baiduApi, "");
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
                    String key = editText.getText().toString();
                    apiPreference.setSummary(mask(key));
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(baiduApi, key);
                    editor.apply();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setCancelable(false);
            builder.show();
        }
    }
}