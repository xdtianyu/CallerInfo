package org.xdty.callerinfo.application;

import android.os.StrictMode;

import com.facebook.stetho.Stetho;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class DebugApplication extends Application {
    public final static String TAG = Application.class.getSimpleName();

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

        Observable.<Void>just(null).observeOn(Schedulers.io()).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                Stetho.initialize(
                        Stetho.newInitializerBuilder(DebugApplication.this)
                                .enableDumpapp(
                                        Stetho.defaultDumperPluginsProvider(DebugApplication.this))
                                .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(
                                        DebugApplication.this))
                                .build());
            }
        });
        super.onCreate();
    }
}
