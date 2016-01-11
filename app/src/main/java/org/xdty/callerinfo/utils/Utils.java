package org.xdty.callerinfo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.phone.number.model.Location;
import org.xdty.phone.number.model.Number;
import org.xdty.phone.number.model.Type;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class Utils {

    public static void showTextWindow(Context context, int resId, int frontType) {
        Bundle bundle = new Bundle();
        bundle.putString(FloatWindow.NUMBER_INFO, context.getString(resId));
        bundle.putInt(FloatWindow.WINDOW_COLOR, ContextCompat.getColor(context,
                R.color.colorPrimary));
        FloatWindow.show(context, FloatWindow.class, frontType);
        FloatWindow.sendData(context, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public static void sendData(Context context, String key, int value, int frontType) {
        Bundle bundle = new Bundle();
        bundle.putInt(key, value);
        FloatWindow.sendData(context, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public static void closeWindow(Context context) {
        FloatWindow.closeAll(context, FloatWindow.class);
    }

    public static void showWindow(Context context, Number number, int frontType) {

        TextColorPair textColor = Utils.getTextColorPair(context, number);

        Bundle bundle = new Bundle();
        bundle.putString(FloatWindow.NUMBER_INFO, textColor.text);
        bundle.putInt(FloatWindow.WINDOW_COLOR, textColor.color);
        FloatWindow.show(context, FloatWindow.class,
                frontType);
        FloatWindow.sendData(context, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public static TextColorPair getTextColorPair(Context context, Number number) {
        Location location = number.getLocation();

        String province = "";
        String city = "";
        String operators = "";
        if (location != null) {
            province = location.getProvince();
            city = location.getCity();
            operators = location.getOperators();
        }
        return Utils.getTextColorPair(context, number.getType().getText(), province, city,
                operators, number.getName(), number.getCount());
    }

    private static TextColorPair getTextColorPair(Context context, String type, String province,
            String city, String operators, String name, int count) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (province == null && city == null && operators == null) {
            province = context.getResources().getString(R.string.unknown);
            city = "";
            operators = "";
        }

        if (!TextUtils.isEmpty(province) && !TextUtils.isEmpty(city) && province.equals(city)) {
            city = "";
        }

        TextColorPair t = new TextColorPair();
        switch (Type.fromString(type)) {
            case NORMAL:
                t.text = context.getResources().getString(
                        R.string.text_normal, province, city, operators);
                t.color = preferences.getInt("color_normal",
                        ContextCompat.getColor(context, R.color.blue_light));
                break;
            case POI:
                t.color = preferences.getInt("color_poi",
                        ContextCompat.getColor(context, R.color.orange_dark));
                t.text = context.getResources().getString(
                        R.string.text_poi, name);
                break;
            case REPORT:
                t.color = preferences.getInt("color_report",
                        ContextCompat.getColor(context, R.color.red_light));
                if (count == 0) {
                    t.text = name;
                } else {
                    t.text = context.getResources().getString(
                            R.string.text_report, province, city, operators,
                            count, name);
                }
                break;
        }
        return t;
    }

    public static boolean isContactExists(Context context, String number) {
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String[] mPhoneNumberProjection = {
                PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME
        };
        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null,
                null, null);
        if (cur != null) {
            try {
                if (cur.moveToFirst()) {
                    return true;
                }
            } finally {
                cur.close();
            }
        }
        return false;
    }

    public static String getDate(long time) {
        Calendar calendar = Calendar.getInstance();
        TimeZone tz = TimeZone.getDefault();
        calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date currentTimeZone = new java.util.Date(time);
        return sdf.format(currentTimeZone);
    }

    public static String mask(String s) {
        return s.replaceAll("([0-9]|[a-f])", "*");
    }

}
