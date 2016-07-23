package org.xdty.callerinfo.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.phone.number.model.INumber;

public final class Window {

    private static final String TAG = Window.class.getSimpleName();

    public void showTextWindow(Context context, int resId, int frontType) {
        Bundle bundle = new Bundle();
        bundle.putString(FloatWindow.NUMBER_INFO, context.getString(resId));
        bundle.putInt(FloatWindow.WINDOW_COLOR, ContextCompat.getColor(context,
                R.color.colorPrimary));
        Log.d(TAG, "showTextWindow: " + Utils.bundleToString(bundle));
        FloatWindow.show(context, FloatWindow.class, frontType);
        FloatWindow.sendData(context, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public void sendData(Context context, String key, int value, int frontType) {
        Bundle bundle = new Bundle();
        bundle.putInt(key, value);
        FloatWindow.show(context, FloatWindow.class, frontType);
        FloatWindow.sendData(context, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public void closeWindow(Context context) {
        Log.d(TAG, "closeWindow");
        FloatWindow.closeAll(context, FloatWindow.class);
    }

    public void showWindow(Context context, INumber number, int frontType) {

        TextColorPair textColor = Utils.getTextColorPair(context, number);

        Bundle bundle = new Bundle();
        bundle.putString(FloatWindow.NUMBER_INFO, textColor.text);
        bundle.putInt(FloatWindow.WINDOW_COLOR, textColor.color);
        Log.d(TAG, "showWindow: " + Utils.bundleToString(bundle));
        FloatWindow.show(context, FloatWindow.class,
                frontType);
        FloatWindow.sendData(context, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

}
