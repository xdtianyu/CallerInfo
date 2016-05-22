package org.xdty.callerinfo.exporter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.database.DatabaseImpl;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.db.MarkedRecord;

import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public final class Exporter {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private SharedPreferences mPrefs;
    private SharedPreferences mWindowPrefs;
    private Database mDatabase;
    private String uidKey;

    public Exporter(Context context) {
        context = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mWindowPrefs = context.getSharedPreferences("window", Context.MODE_PRIVATE);
        mDatabase = DatabaseImpl.getInstance();
        uidKey = context.getString(R.string.uid_key);
    }

    public Observable<String> export() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext(exportSync());
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public String exportSync() {
        Data data = new Data();
        data.setting = mPrefs.getAll();
        data.window = mWindowPrefs.getAll();
        data.timestamp = System.currentTimeMillis();
        data.versionCode = BuildConfig.VERSION_CODE;
        data.versionName = BuildConfig.VERSION_NAME;
        data.callers = mDatabase.fetchCallersSync();
        data.inCalls = mDatabase.fetchInCallsSync();
        data.markedRecords = mDatabase.fetchMarkedRecordsSync();
        return gson.toJson(data);
    }

    public Observable<String> fromString(final String s) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext(fromStringSync(s));
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public String fromStringSync(String s) {
        String res = null;
        try {
            Data data = gson.fromJson(s, Data.class);
            for (Map.Entry<String, ?> entry : data.setting.entrySet()) {
                Object value = entry.getValue();
                addPref(mPrefs, entry.getKey(), value);
            }
            for (Map.Entry<String, ?> entry : data.window.entrySet()) {
                Object value = entry.getValue();
                addPref(mWindowPrefs, entry.getKey(), value);
            }
            mDatabase.addCallers(data.callers);
            mDatabase.addInCallers(data.inCalls);
            mDatabase.addMarkedRecords(data.markedRecords);
        } catch (Exception e) {
            e.printStackTrace();
            res = e.getMessage();
        }
        return res;
    }

    private void addPref(SharedPreferences prefs, String key, Object value) {
        // ignore uid
        if (key.equals(uidKey)) {
            return;
        }

        if (value instanceof String) {
            prefs.edit().putString(key, (String) value).apply();
        } else if (value instanceof Integer) {
            prefs.edit().putInt(key, (Integer) value).apply();
        } else if (value instanceof Long) {
            prefs.edit().putLong(key, (Long) value).apply();
        } else if (value instanceof Float) {
            prefs.edit().putFloat(key, (Float) value).apply();
        } else if (value instanceof Boolean) {
            prefs.edit().putBoolean(key, (Boolean) value).apply();
        }
    }

    public static class Data {
        Map<String, ?> setting;
        Map<String, ?> window;
        List<Caller> callers;
        List<InCall> inCalls;
        List<MarkedRecord> markedRecords;
        String versionName;
        int versionCode;
        long timestamp;
    }

}
