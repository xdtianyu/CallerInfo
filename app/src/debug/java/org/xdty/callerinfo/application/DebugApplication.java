package org.xdty.callerinfo.application;

import android.annotation.SuppressLint;
import android.os.StrictMode;

import com.facebook.stetho.Stetho;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
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

        //noinspection ConstantConditions,ResultOfMethodCallIgnored
        Observable.<Void>just(null).observeOn(Schedulers.io()).subscribe(new Consumer<Void>() {
            @Override
            public void accept(Void aVoid) throws Exception {
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
