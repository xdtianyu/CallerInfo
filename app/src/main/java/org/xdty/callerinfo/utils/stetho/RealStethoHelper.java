package org.xdty.callerinfo.utils.stetho;

import android.content.Context;

import com.facebook.stetho.Stetho;

public class RealStethoHelper implements StethoHelper {
    @Override
    public void init(Context context) {
        Stetho.initialize(
                Stetho.newInitializerBuilder(context)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(context))
                        .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(context))
                        .build());
    }
}
