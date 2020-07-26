package org.xdty.callerinfo.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.Preference;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;

import org.jetbrains.annotations.Nullable;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.settings.dialog.CustomApiDialog;
import org.xdty.callerinfo.settings.dialog.EditDialog;
import org.xdty.callerinfo.settings.dialog.SeekBarDialog;
import org.xdty.callerinfo.settings.dialog.SettingsDialog;
import org.xdty.callerinfo.settings.dialog.TextDialog;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.callerinfo.utils.Window;

import java.util.Arrays;
import java.util.List;

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

        new SeekBarDialog(context, sharedPrefs)
                .max(max)
                .defaultValue(defaultValue)
                .seek(new SeekBarDialog.SeekListener() {
                    @Override
                    public void onSeek(int progress) {
                        if (progress == 0) {
                            progress = 1;
                        }
                        window.sendData(bundleKey, progress, Window.Type.SETTING);
                    }
                })
                .title(title)
                .key(keyId)
                .confirm(new SettingsDialog.ConfirmListener())
                .cancel(R.string.cancel, null)
                .dismiss(dialog -> window.closeWindow())
                .show();

        window.showTextWindow(textRes, Window.Type.SETTING);
    }

    public void showApiDialog(int keyId, int title, final int url) {
        final String key = context.getString(keyId);

        new EditDialog(context, sharedPrefs)
                .key(key)
                .title(title)
                .confirm(new SettingsDialog.ConfirmListener() {
                    @Override
                    public void onConfirm(String value) {
                        preferenceActions.findPreference(key).setSummary(Utils.Companion.mask(value));
                    }
                })
                .cancel(R.string.cancel, null)
                .help(R.string.fetch, new SettingsDialog.HelpListener() {
                    @Override
                    public void onHelp() {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(url))));
                    }
                })
                .show();
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
        new TextDialog(context, sharedPrefs)
                .title(title)
                .text(text)
                .show();
    }

    public void showConfirmDialog(int title, int text, final int key) {
        new TextDialog(context, sharedPrefs)
                .title(title)
                .text(text)
                .cancel(new SettingsDialog.CancelListener() {
                    @Override
                    public void onCancel() {
                        preferenceActions.onConfirmCanceled(key);
                    }
                })
                .confirm(new SettingsDialog.ConfirmListener() {
                    @Override
                    public void onConfirm(@Nullable String value) {
                        preferenceActions.onConfirmed(key);
                    }
                })
                .show();
    }

    public void showCustomApiDialog() {
        String key = context.getString(R.string.custom_api_url);
        new CustomApiDialog(context, sharedPrefs)
                .title(R.string.custom_api)
                .key(key)
                .confirm(new SettingsDialog.ConfirmListener() {
                    @Override
                    public void onConfirm(@Nullable String value) {
                        preferenceActions.findPreference(key).setSummary(value);
                    }
                })
                .cancel(R.string.cancel, null)
                .help(R.string.document, new SettingsDialog.HelpListener() {
                    @Override
                    public void onHelp() {
                        context.startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(context.getString(R.string.api_document_url))));
                    }
                })
                .show();
    }

    @SuppressWarnings("SameParameterValue")
    public void showEditDialog(int keyId, int title, final int defaultText, int hint) {
        showEditDialog(keyId, title, defaultText, hint, 0, 0);
    }

    public void showEditDialog(int keyId, int title, final int defaultText, int hint,
                               final int help, final int helpText) {
        String key = context.getString(keyId);
        new EditDialog(context, sharedPrefs)
                .key(key)
                .title(title)
                .hint(hint)
                .defaultText(defaultText)
                .confirm(new SettingsDialog.ConfirmListener() {
                    @Override
                    public void onConfirm(String value) {
                        preferenceActions.findPreference(key).setSummary(value);
                    }
                })
                .help(helpText, new SettingsDialog.HelpListener() {
                    @Override
                    public void onHelp() {
                        showTextDialog(help, helpText);
                    }
                })
                .show();
    }

}