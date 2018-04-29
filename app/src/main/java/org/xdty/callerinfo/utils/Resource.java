package org.xdty.callerinfo.utils;

import android.content.Context;
import android.content.res.Resources;

public final class Resource {

    Resources mResources;

    public void init(Context context) {
        mResources = context.getResources();
    }

    public Resources getResources() {
        return mResources;
    }

    public static Resource getInstance() {
        return SingletonHelper.sINSTANCE;
    }

    private final static class SingletonHelper {
        private final static Resource sINSTANCE = new Resource();
    }
}
