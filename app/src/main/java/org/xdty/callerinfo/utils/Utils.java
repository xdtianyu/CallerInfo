package org.xdty.callerinfo.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.activity.MarkActivity;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.phone.number.model.Type;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

public final class Utils {

    private static final int NOTIFICATION_MARK = 0x01;
    private static final String TAG = Utils.class.getSimpleName();
    private static Map<Integer, String> sNumberSourceMap;

    private Utils() {
        throw new AssertionError("Utils class is not meant to be instantiated or subclassed.");
    }

    public static String getDate(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        TimeZone tz = TimeZone.getDefault();
        calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        java.util.Date currentTimeZone = new java.util.Date(timestamp);
        return sdf.format(currentTimeZone);
    }

    public static String getTime(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        TimeZone tz = TimeZone.getDefault();
        calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
        java.util.Date currentTimeZone = new java.util.Date(timestamp);
        return sdf.format(currentTimeZone);
    }

    public static String readableDate(long timestamp) {
        long current = System.currentTimeMillis();
        return DateUtils.getRelativeTimeSpanString(timestamp, current, DateUtils.DAY_IN_MILLIS)
                .toString();
    }

    public static String readableTime(long duration) {
        String result;
        Context context = Application.getApplication();
        int seconds = (int) (duration / 1000) % 60;
        int minutes = (int) ((duration / (1000 * 60)) % 60);
        int hours = (int) ((duration / (1000 * 60 * 60)) % 24);
        if (duration < 60000) {
            result = context.getString(R.string.readable_second, seconds);
        } else if (duration < 3600000) {
            result = context.getString(R.string.readable_minute, minutes, seconds);
        } else {
            result = context.getString(R.string.readable_hour, hours, minutes, seconds);
        }
        return result;
    }

    public static String mask(String s) {
        return s.replaceAll("([0-9]|[a-f])", "*");
    }

    public static void checkLocale(Context context) {

        if (SettingImpl.getInstance().isForceChinese()) {
            Locale locale = new Locale("zh");
            Locale.setDefault(locale);
            Configuration config = context.getResources().getConfiguration();
            config.locale = locale;
            context.getResources().updateConfiguration(config,
                    context.getResources().getDisplayMetrics());
        }
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static int getVersionCode(Context context, String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static String bundleToString(Bundle bundle) {
        StringBuilder out = new StringBuilder("Bundle[");

        if (bundle == null) {
            out.append("null");
        } else {
            boolean first = true;
            for (String key : bundle.keySet()) {
                if (!first) {
                    out.append(", ");
                }

                out.append(key).append('=');

                Object value = bundle.get(key);

                if (value instanceof int[]) {
                    out.append(Arrays.toString((int[]) value));
                } else if (value instanceof byte[]) {
                    out.append(Arrays.toString((byte[]) value));
                } else if (value instanceof boolean[]) {
                    out.append(Arrays.toString((boolean[]) value));
                } else if (value instanceof short[]) {
                    out.append(Arrays.toString((short[]) value));
                } else if (value instanceof long[]) {
                    out.append(Arrays.toString((long[]) value));
                } else if (value instanceof float[]) {
                    out.append(Arrays.toString((float[]) value));
                } else if (value instanceof double[]) {
                    out.append(Arrays.toString((double[]) value));
                } else if (value instanceof String[]) {
                    out.append(Arrays.toString((String[]) value));
                } else if (value instanceof CharSequence[]) {
                    out.append(Arrays.toString((CharSequence[]) value));
                } else if (value instanceof Parcelable[]) {
                    out.append(Arrays.toString((Parcelable[]) value));
                } else if (value instanceof Bundle) {
                    out.append(bundleToString((Bundle) value));
                } else {
                    out.append(value);
                }

                first = false;
            }
        }

        out.append("]");
        return out.toString();
    }

    public static String getDeviceId(Context context) {
        final TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(),
                ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }

    public static void showMarkNotification(Context context, String number) {
        Intent intent = new Intent(context, MarkActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Setting setting = SettingImpl.getInstance();
        setting.addPaddingMark(number);

        ArrayList<String> list = setting.getPaddingMarks();
        String numbers = TextUtils.join(", ", list);

        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        int requestCode = new Random().nextInt();
        PendingIntent pIntent = PendingIntent.getActivity(context, requestCode, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.status_icon)
                .setContentIntent(pIntent)
                .setContentTitle(context.getString(R.string.mark_number))
                .setContentText(numbers)
                .setAutoCancel(true)
                .setContentIntent(pIntent);
        manager.notify(NOTIFICATION_MARK, builder.build());
    }

    public static void startMarkActivity(Context context, String number) {
        Intent intent = new Intent(context, MarkActivity.class);
        intent.putExtra(MarkActivity.NUMBER, number);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static int typeFromString(String type) {

        if (TextUtils.isEmpty(type)) {
            return -1;
        }

        ArrayList<String> types = new ArrayList<>(
                Arrays.asList(Application.getApplication()
                        .getResources()
                        .getStringArray(R.array.mark_type_source)));
        for (String t : types) {
            if (t.contains(type)) {
                return types.indexOf(t);
            }

            ArrayList<String> ts = new ArrayList<>(Arrays.asList(t.split("\\|")));
            for (String s : ts) {
                if (type.contains(s)) {
                    return types.indexOf(t);
                }
            }
        }
        Log.e(TAG, "typeFromString failed: " + type);
        return -1;
    }

    public static String sourceFromId(int sourceId) {

        if (sourceId == -9999) {
            return Application.getApplication()
                    .getResources()
                    .getString(R.string.mark);
        }

        if (sNumberSourceMap == null) {
            sNumberSourceMap = new HashMap<>();

            String[] values = Application.getApplication()
                    .getResources()
                    .getStringArray(R.array.source_values);
            int[] keys = Application.getApplication()
                    .getResources()
                    .getIntArray(R.array.source_keys);

            for (int i = 0; i < keys.length; i++) {
                sNumberSourceMap.put(keys[i], values[i]);
            }
        }

        return sNumberSourceMap.get(sourceId);

    }

    public static String typeFromId(int type) {
        String[] values = Application.getApplication()
                .getResources()
                .getStringArray(R.array.mark_type);
        if (type >= 0 && type < values.length) {
            return values[type];
        } else {
            return Application.getApplication()
                    .getResources()
                    .getString(R.string.custom);
        }
    }

    public static Type markTypeFromName(String name) {
        int type = Utils.typeFromString(name);
        switch (MarkedRecord.MarkType.fromInt(type)) {
            case HARASSMENT:
            case FRAUD:
            case ADVERTISING:
                return Type.REPORT;
            case EXPRESS_DELIVERY:
            case RESTAURANT_DELIVER:
            default:
                return Type.POI;
        }
    }
}
