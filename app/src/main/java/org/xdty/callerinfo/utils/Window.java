package org.xdty.callerinfo.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.phone.number.model.INumber;

public final class Window {

    private static final String TAG = Window.class.getSimpleName();

    private Context mContext;

    public Window() {
        mContext = Application.getApplication();
    }

    public void showTextWindow(int resId, int frontType) {
        Bundle bundle = new Bundle();
        bundle.putString(FloatWindow.NUMBER_INFO, mContext.getString(resId));
        bundle.putInt(FloatWindow.WINDOW_COLOR, ContextCompat.getColor(mContext,
                R.color.colorPrimary));
        Log.d(TAG, "showTextWindow: " + Utils.bundleToString(bundle));
        FloatWindow.show(mContext, FloatWindow.class, frontType);
        FloatWindow.sendData(mContext, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public void sendData(String key, int value, int frontType) {
        Bundle bundle = new Bundle();
        bundle.putInt(key, value);
        FloatWindow.show(mContext, FloatWindow.class, frontType);
        FloatWindow.sendData(mContext, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public void closeWindow() {
        Log.d(TAG, "closeWindow");
        FloatWindow.closeAll(mContext, FloatWindow.class);
    }

    public void showWindow(INumber number, int frontType) {

        TextColorPair textColor = TextColorPair.from(number);

        Bundle bundle = new Bundle();
        bundle.putString(FloatWindow.NUMBER_INFO, textColor.text);
        bundle.putInt(FloatWindow.WINDOW_COLOR, textColor.color);
        Log.d(TAG, "showWindow: " + Utils.bundleToString(bundle));
        FloatWindow.show(mContext, FloatWindow.class,
                frontType);
        FloatWindow.sendData(mContext, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

}
