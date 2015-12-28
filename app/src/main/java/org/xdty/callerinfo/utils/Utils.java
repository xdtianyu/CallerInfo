package org.xdty.callerinfo.utils;

import android.content.Context;
import android.os.Bundle;

import org.xdty.callerinfo.FloatWindow;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.phone.number.model.Location;
import org.xdty.phone.number.model.Number;
import org.xdty.phone.number.model.Type;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import wei.mark.standout.StandOutWindow;

public class Utils {

    public static void showWindow(Context context, Number number) {
        showWindow(context, Utils.getTextColorPair(context, number), false);
    }

    public static void showMovableWindow(Context context, Number number) {
        showWindow(context, Utils.getTextColorPair(context, number), true);
    }


    private static void showWindow(Context context, TextColorPair textColor, boolean movable) {

        int frontType = movable ? FloatWindow.VIEWER_FRONT : FloatWindow.CALLER_FRONT;

        Bundle bundle = new Bundle();
        bundle.putString(FloatWindow.NUMBER_INFO, textColor.text);
        bundle.putInt(FloatWindow.WINDOW_COLOR, textColor.color);
        StandOutWindow.show(context, FloatWindow.class,
                frontType);
        StandOutWindow.sendData(context, FloatWindow.class,
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

        if (province == null && city == null && operators == null) {
            province = context.getResources().getString(R.string.unknown);
            city = "";
            operators = "";
        }

        TextColorPair t = new TextColorPair();
        switch (Type.fromString(type)) {
            case NORMAL:
                t.text = context.getResources().getString(
                        R.string.text_normal, province, city, operators);
                break;
            case POI:
                t.color = R.color.orange_dark;
                t.text = context.getResources().getString(
                        R.string.text_poi, name);
                break;
            case REPORT:
                t.color = R.color.red_light;
                t.text = context.getResources().getString(
                        R.string.text_report, province, city, operators,
                        count, name);
                break;
        }
        return t;
    }

    public static String getDate(long time) {
        Calendar calendar = Calendar.getInstance();
        TimeZone tz = TimeZone.getDefault();
        calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date currentTimeZone=new java.util.Date(time);
        return sdf.format(currentTimeZone);
    }

}
