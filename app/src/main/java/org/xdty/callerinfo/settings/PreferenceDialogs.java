package org.xdty.callerinfo.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.Preference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.settings.dialog.CustomApiDialog;
import org.xdty.callerinfo.settings.dialog.EditDialog;
import org.xdty.callerinfo.settings.dialog.RadioDialog;
import org.xdty.callerinfo.settings.dialog.SeekBarDialog;
import org.xdty.callerinfo.settings.dialog.SettingsDialog;
import org.xdty.callerinfo.settings.dialog.TextDialog;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.callerinfo.utils.Window;

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

        new RadioDialog(context, sharedPrefs)
                .listId(listId)
                .offset(offset)
                .defaultValue(defValue)
                .check(new RadioDialog.CheckedListener() {
                    @Override
                    public void onChecked(@NotNull String value) {
                        Preference preference = preferenceActions.findPreference(key);
                        if (preference != null) {
                            preference.setSummary(value);
                        }
                    }
                })
                .key(key)
                .title(title)
                .show();
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