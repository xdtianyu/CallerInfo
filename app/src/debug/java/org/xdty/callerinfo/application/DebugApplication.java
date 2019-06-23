package org.xdty.callerinfo.application;

import android.annotation.SuppressLint;
import android.os.StrictMode;

import com.facebook.stetho.Stetho;

import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

public class DebugApplication extends Application {
    public final static String TAG = Application.class.getSimpleName();

    @SuppressLint("CheckResult")
    @Override
    public void onCreate() {

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                //.penaltyDeath()
                .build());

        Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                Stetho.initialize(
                        Stetho.newInitializerBuilder(DebugApplication.this)
                                .enableDumpapp(
                                        Stetho.defaultDumperPluginsProvider(DebugApplication.this))
                                .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(
                                        DebugApplication.this))
                                .build());
            }
        }).subscribeOn(Schedulers.io()).subscribe();

        super.onCreate();
    }
}
