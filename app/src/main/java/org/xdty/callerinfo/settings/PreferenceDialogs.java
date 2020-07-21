package org.xdty.callerinfo.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.Preference;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.settings.dialog.EditDialog;
import org.xdty.callerinfo.settings.dialog.SettingsDialog;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.callerinfo.utils.Window;

import java.util.Arrays;
import java.util.List;

import app.minimize.com.seek_bar_compat.SeekBarCompat;

public class PreferenceDialogs {

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

    public void showSeekBarDialog(int keyId, final String bundleKey, int defaultValue,
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

    public void showApiDialog(int keyId, int title, final int url) {
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

    public void showRadioDialog(int keyId, int title, int listId, int defValue) {
        showRadioDialog(keyId, title, listId, defValue, 0);
    }

    public void showRadioDialog(int keyId, int title, int listId, int defValue,
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

    public void showTextDialog(int title, int text) {
        showTextDialog(title, context.getString(text));
    }

    public void showTextDialog(int title, String text) {
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

    public void showConfirmDialog(int title, int text, final int key) {
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

    public void showCustomApiDialog() {
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
    public void showEditDialog(int keyId, int title, final int defaultText, int hint) {
        showEditDialog(keyId, title, defaultText, hint, 0, 0);
    }

    public void showEditDialog(int keyId, int title, final int defaultText, int hint,
                               final int help, final int helpText) {
        String key = context.getString(keyId);
        SettingsDialog dialog = new EditDialog(context, sharedPrefs)
                .key(key)
                .title(title)
                .hint(hint)
                .help(helpText)
                .defaultText(defaultText);

        dialog.listen(new SettingsDialog.Listener() {
            @Override
            public void onConfirm(String value) {
                preferenceActions.findPreference(key).setSummary(value);
            }

            @Override
            public void onHelp() {
                showTextDialog(help, helpText);
            }
        });

        dialog.show();
    }

}