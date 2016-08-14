package org.xdty.callerinfo.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.Type;

public class TextColorPair {

    public String text = "";
    public int color = R.color.blue_light;

    private TextColorPair() {
    }

    // generate color from name, has no geo info.
    public static TextColorPair from(String name) {
        String type = Utils.markTypeFromName(name).getText();
        return from(type, "", "", "", name, 0);
    }

    public static TextColorPair from(INumber number) {

        String province = number.getProvince();
        String city = number.getCity();
        String operators = number.getProvider();
        return from(number.getType().getText(), province, city, operators, number.getName(),
                number.getCount());
    }

    private static TextColorPair from(String type, String province,
            String city, String operators, String name, int count) {

        Context context = Application.getApplication();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (province == null) {
            province = "";
        }

        if (city == null) {
            city = "";
        }

        if (operators == null) {
            operators = "";
        }

        if (!TextUtils.isEmpty(province) && !TextUtils.isEmpty(city) && province.equals(city)) {
            city = "";
        }

        TextColorPair t = new TextColorPair();

        Type numberType = Type.fromString(type);

        if (name != null && !name.isEmpty()) {
            numberType = Utils.markTypeFromName(name);
        }

        switch (numberType) {
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
                        R.string.text_poi, province, city, operators, name);
                break;
            case REPORT:
                t.color = preferences.getInt("color_report",
                        ContextCompat.getColor(context, R.color.red_light));
                if (count == 0) {
                    t.text = context.getResources().getString(
                            R.string.text_poi, province, city, operators, name);
                } else {
                    t.text = context.getResources().getString(
                            R.string.text_report, province, city, operators,
                            count, name);
                }
                break;
        }

        t.text = t.text.trim().replaceAll(" +", " ");

        if (t.text.isEmpty() || t.text.contains(context.getString(R.string.baidu_advertising))) {
            t.text = context.getString(R.string.unknown);
        }

        return t;
    }
}
