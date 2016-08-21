package org.xdty.callerinfo.model.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.Display;
import android.view.WindowManager;

import com.google.gson.Gson;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.model.caller.Status;

import java.util.ArrayList;
import java.util.Arrays;

public class SettingImpl implements Setting {

    private static Gson gson = new Gson();
    private static Context sContext;
    private final int mScreenWidth;
    private final int mScreenHeight;
    private SharedPreferences mPrefs;
    private SharedPreferences mWindowPrefs;

    private boolean isOutgoing;

    private SettingImpl() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(sContext);
        mWindowPrefs = sContext.getSharedPreferences("window", Context.MODE_PRIVATE);

        WindowManager mWindowManager =
                (WindowManager) sContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = mWindowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        mScreenWidth = point.x;
        mScreenHeight = point.y;
    }

    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    public static Setting getInstance() {
        if (sContext == null) {
            throw new IllegalStateException("Setting is not initialized!");
        }
        return SingletonHelper.INSTANCE;
    }

    @Override
    public int getScreenWidth() {
        return mScreenWidth;
    }

    @Override
    public int getScreenHeight() {
        return mScreenHeight;
    }

    @Override
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = sContext.getResources()
                .getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = sContext.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public boolean isEulaSet() {
        return mPrefs.getBoolean("eula", false);
    }

    @Override
    public int getWindowHeight() {
        return mPrefs.getInt(getString(R.string.window_height_key), getDefaultHeight());
    }

    @Override
    public int getDefaultHeight() {
        return mScreenHeight / 8;
    }

    @Override
    public boolean isShowCloseAnim() {
        return mPrefs.getBoolean(getString(R.string.window_close_anim_key), true);
    }

    @Override
    public boolean isTransBackOnly() {
        return mPrefs.getBoolean(getString(R.string.window_trans_back_only_key), true);
    }

    @Override
    public boolean isEnableTextColor() {
        return mPrefs.getBoolean(getString(R.string.window_text_color_key), false);
    }

    @Override
    public int getTextPadding() {
        return mPrefs.getInt(getString(R.string.window_text_padding_key), 0);
    }

    @Override
    public int getTextAlignment() {
        return mPrefs.getInt(getString(R.string.window_text_alignment_key), 1);
    }

    @Override
    public int getTextSize() {
        return mPrefs.getInt(getString(R.string.window_text_size_key), 20);
    }

    @Override
    public int getWindowTransparent() {
        return mPrefs.getInt(getString(R.string.window_transparent_key), 80);
    }

    @Override
    public boolean isDisableMove() {
        return mPrefs.getBoolean(getString(R.string.disable_move_key), false);
    }

    @Override
    public boolean isAutoReportEnabled() {
        return mPrefs.getBoolean(getString(R.string.auto_report_key), false);
    }

    @Override
    public boolean isMarkingEnabled() {
        return mPrefs.getBoolean(getString(R.string.enable_marking_key), false);
    }

    @Override
    public void addPaddingMark(String number) {
        String key = getString(R.string.padding_mark_numbers_key);
        ArrayList<String> list = getPaddingMarks();
        if (list.contains(number)) {
            return;
        }
        list.add(number);
        String paddingNumbers = gson.toJson(list);
        mPrefs.edit().putString(key, paddingNumbers).apply();
    }

    @Override
    public void removePaddingMark(String number) {
        String key = getString(R.string.padding_mark_numbers_key);
        ArrayList<String> list = getPaddingMarks();
        if (!list.contains(number)) {
            return;
        }
        list.remove(number);
        String paddingNumbers = gson.toJson(list);
        mPrefs.edit().putString(key, paddingNumbers).apply();
    }

    @Override
    public ArrayList<String> getPaddingMarks() {
        String key = getString(R.string.padding_mark_numbers_key);
        if (mPrefs.contains(key)) {
            String paddingNumbers = mPrefs.getString(key, null);
            return new ArrayList<>(
                    Arrays.asList(gson.fromJson(paddingNumbers, String[].class)));
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public String getUid() {
        String key = getString(R.string.uid_key);
        if (mPrefs.contains(key)) {
            return mPrefs.getString(key, "");
        } else {
            String uid = Utils.getDeviceId(sContext);
            mPrefs.edit().putString(key, uid).apply();
            return uid;
        }
    }

    @Override
    public void updateLastScheduleTime() {
        updateLastScheduleTime(System.currentTimeMillis());
    }

    @Override
    public void updateLastScheduleTime(long timestamp) {
        mPrefs.edit()
                .putLong(getString(R.string.last_schedule_time_key), timestamp)
                .apply();
    }

    @Override
    public long lastScheduleTime() {
        return mPrefs.getLong(getString(R.string.last_schedule_time_key), 0);
    }

    @Override
    public long lastCheckDataUpdateTime() {
        return mPrefs.getLong(getString(R.string.last_check_data_update_time_key), 0);
    }

    @Override
    public void updateLastCheckDataUpdateTime(long timestamp) {
        mPrefs.edit()
                .putLong(getString(R.string.last_check_data_update_time_key), timestamp)
                .apply();
    }

    @Override
    public Status getStatus() {
        Status status = new Status();
        status.version = mPrefs.getInt(getString(R.string.offline_status_version_key), 0);
        status.count = mPrefs.getInt(getString(R.string.offline_status_count_key), 0);
        status.new_count = mPrefs.getInt(getString(R.string.offline_status_new_count_key), 0);
        status.timestamp = mPrefs.getLong(getString(R.string.offline_status_timestamp_key), 0);
        return status;
    }

    @Override
    public void setStatus(Status status) {
        mPrefs.edit()
                .putInt(getString(R.string.offline_status_version_key), status.version)
                .apply();
        mPrefs.edit().putInt(getString(R.string.offline_status_count_key), status.count).apply();
        mPrefs.edit()
                .putInt(getString(R.string.offline_status_new_count_key), status.new_count)
                .apply();
        mPrefs.edit()
                .putLong(getString(R.string.offline_status_timestamp_key), status.timestamp)
                .apply();
    }

    @Override
    public boolean isNotMarkContact() {
        return mPrefs.getBoolean(getString(R.string.not_mark_contact_key), false);
    }

    @Override
    public boolean isDisableOutGoingHangup() {
        return mPrefs.getBoolean(getString(R.string.disable_outgoing_blacklist_key), false);
    }

    @Override
    public boolean isTemporaryDisableHangup() {
        return mPrefs.getBoolean(getString(R.string.temporary_disable_blacklist_key), false);
    }

    @Override
    public int getRepeatedCountIndex() {
        return mPrefs.getInt(getString(R.string.repeated_incoming_count_key), 1);
    }

    @Override
    public void clear() {
        mPrefs.edit().clear().apply();
        mWindowPrefs.edit().clear().apply();
    }

    @Override
    public int getNormalColor() {
        return mPrefs.getInt("color_normal", ContextCompat.getColor(sContext, R.color.blue_light));
    }

    @Override
    public int getPoiColor() {
        return mPrefs.getInt("color_poi", ContextCompat.getColor(sContext, R.color.orange_dark));
    }

    @Override
    public int getReportColor() {
        return mPrefs.getInt("color_report", ContextCompat.getColor(sContext, R.color.red_light));
    }

    @Override
    public boolean isOnlyOffline() {
        return mPrefs.getBoolean(sContext.getString(R.string.only_offline_key), false);
    }

    @Override
    public void fix() {
        // fix api type because baidu api is dead
        int type = mPrefs.getInt(getString(R.string.api_type_key), 1);
        if (type == 0) {
            mPrefs.edit().remove(getString(R.string.api_type_key)).apply();
        }
    }

    @Override
    public void setOutgoing(boolean isOutgoing) {
        this.isOutgoing = isOutgoing;
    }

    @Override
    public boolean isOutgoingPositionEnabled() {
        return mPrefs.getBoolean(getString(R.string.outgoing_window_position_key), false);
    }

    @Override
    public boolean isAddingRingOnceCallLog() {
        return mPrefs.getBoolean(getString(R.string.ring_once_and_auto_hangup_key), false);
    }

    @Override
    public boolean isHidingWhenTouch() {
        return mPrefs.getBoolean(getString(R.string.hide_when_touch_key), false);
    }

    @Override
    public void setEula() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean("eula", true);
        editor.putInt("eula_version", 1);
        editor.putInt("version", BuildConfig.VERSION_CODE);
        editor.apply();
    }

    @Override
    public String getIgnoreRegex() {
        return mPrefs.getString(getString(R.string.ignore_regex_key), "").replace("*",
                "[0-9]").replace(" ", "|");
    }

    @Override
    public boolean isHidingOffHook() {
        return mPrefs.getBoolean(getString(R.string.hide_when_off_hook_key), false);
    }

    @Override
    public boolean isShowingOnOutgoing() {
        return mPrefs.getBoolean(getString(R.string.display_on_outgoing_key), false);
    }

    @Override
    public boolean isIgnoreKnownContact() {
        return mPrefs.getBoolean(getString(R.string.ignore_known_contact_key), false);
    }

    @Override
    public boolean isShowingContactOffline() {
        return mPrefs.getBoolean(getString(R.string.contact_offline_key), false);
    }

    @Override
    public boolean isAutoHangup() {
        return mPrefs.getBoolean(getString(R.string.auto_hangup_key), false);
    }

    @Override
    public boolean isAddingCallLog() {
        return mPrefs.getBoolean(getString(R.string.add_call_log_key), false);
    }

    @Override
    public boolean isCatchCrash() {
        return mPrefs.getBoolean(getString(R.string.catch_crash_key), false);
    }

    @Override
    public boolean isForceChinese() {
        return mPrefs.getBoolean(getString(R.string.force_chinese_key), false);
    }

    @Override
    public String getKeywords() {
        String keywordKey = getString(R.string.hangup_keyword_key);
        String keywordDefault = getString(R.string.hangup_keyword_default);
        String keywords = mPrefs.getString(keywordKey, keywordDefault).trim();
        if (keywords.isEmpty()) {
            keywords = keywordDefault;
        }
        return keywords;
    }

    @Override
    public String getGeoKeyword() {
        return mPrefs.getString(getString(R.string.hangup_geo_keyword_key), "").trim();
    }

    @Override
    public String getNumberKeyword() {
        return mPrefs.getString(getString(R.string.hangup_number_keyword_key), "").trim();
    }

    @Override
    public int getWindowX() {
        String prefix = isOutgoing && isOutgoingPositionEnabled() ? "out_" : "";
        return mWindowPrefs.getInt(prefix + "x", -1);
    }

    @Override
    public int getWindowY() {
        String prefix = isOutgoing && isOutgoingPositionEnabled() ? "out_" : "";
        return mWindowPrefs.getInt(prefix + "y", -1);
    }

    @Override
    public void setWindow(int x, int y) {
        String prefix = isOutgoing && isOutgoingPositionEnabled() ? "out_" : "";
        SharedPreferences.Editor editor = mWindowPrefs.edit();
        editor.putInt(prefix + "x", x);
        editor.putInt(prefix + "y", y);
        editor.apply();
    }

    private String getString(@StringRes int resId) {
        return sContext.getString(resId);
    }

    private static class SingletonHelper {
        private final static SettingImpl INSTANCE = new SettingImpl();
    }
}
