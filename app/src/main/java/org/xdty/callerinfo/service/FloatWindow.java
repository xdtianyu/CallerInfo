package org.xdty.callerinfo.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xdty.callerinfo.R;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

public class FloatWindow extends StandOutWindow {

    public final static String TAG = FloatWindow.class.getSimpleName();

    public final static String NUMBER_INFO = "number_info";
    public final static String TEXT_SIZE = "text_size";
    public final static String WINDOW_TRANS = "window_trans";
    public final static String WINDOW_COLOR = "window_color";
    public final static String WINDOW = "window";

    public final static int CALLER_FRONT = 1000;
    public final static int VIEWER_FRONT = 1001;

    SharedPreferences sharedPreferences;

    @Override
    public String getAppName() {
        return getResources().getString(R.string.app_name);
    }

    @Override
    public int getAppIcon() {
        return R.drawable.status_icon;
    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {
        // create a new layout from body.xml
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.float_window, frame, true);
    }

    // the window will be centered
    @Override
    public StandOutLayoutParams getParams(int id, Window window) {

        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = mWindowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        int height = point.y / 8;

        StandOutLayoutParams standOutLayoutParams = new StandOutLayoutParams(id, point.x, height,
                StandOutLayoutParams.CENTER, StandOutLayoutParams.CENTER);

        sharedPreferences = getSharedPreferences(WINDOW, Context.MODE_PRIVATE);

        int x = sharedPreferences.getInt("x", -1);
        int y = sharedPreferences.getInt("y", -1);

        if (x != -1 && y != -1) {
            standOutLayoutParams.x = x;
            standOutLayoutParams.y = y;
        }

        standOutLayoutParams.minWidth = point.x;
        standOutLayoutParams.maxWidth = point.x;
        standOutLayoutParams.minHeight = height;
        if (id == CALLER_FRONT) {
            standOutLayoutParams.type = StandOutLayoutParams.TYPE_SYSTEM_OVERLAY;
        }
        return standOutLayoutParams;
    }

    // move the window by dragging the view
    @Override
    public int getFlags(int id) {
        if (id == CALLER_FRONT) {
            return super.getFlags(id) | StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
        } else {
            return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
                    | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
                    | StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE;
        }
    }

    @Override
    public String getPersistentNotificationTitle(int id) {
        return getAppName();
    }

    @Override
    public String getPersistentNotificationMessage(int id) {
        return getString(R.string.close_float_window);
    }

    @Override
    public Intent getPersistentNotificationIntent(int id) {
        return StandOutWindow.getCloseIntent(this, FloatWindow.class, id);
    }

    @Override
    public Animation getCloseAnimation(int id) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean anim = preferences.getBoolean(getString(R.string.window_close_anim_key), true);
        if (anim) {
            return super.getCloseAnimation(id);
        } else {
            return null;
        }
    }

    @Override
    public void onMove(int id, Window window, View view, MotionEvent event) {
        super.onMove(id, window, view, event);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("x", window.getLayoutParams().x);
        editor.putInt("y", window.getLayoutParams().y);
        editor.apply();
    }

    @Override
    public boolean onClose(int id, Window window) {
        super.onClose(id, window);
        stopService(getShowIntent(this, getClass(), id));
        return false;
    }

    @Override
    public void onReceiveData(int id, int requestCode, Bundle data,
            Class<? extends StandOutWindow> fromCls, int fromId) {
        int color = data.getInt(WINDOW_COLOR);
        String text = data.getString(NUMBER_INFO);
        int size = data.getInt(TEXT_SIZE);
        int trans = data.getInt(WINDOW_TRANS);
        Window window = getWindow(id);
        LinearLayout layout = (LinearLayout) window.findViewById(R.id.window_layout);
        TextView textView = (TextView) window.findViewById(R.id.number_info);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean isTransBackOnly = preferences.getBoolean(
                getString(R.string.window_trans_back_only_key), true);

        if (size == 0) {
            size = preferences.getInt(getString(R.string.window_text_size_key), 20);
        }

        if (trans == 0) {
            trans = preferences.getInt(getString(R.string.window_transparent_key), 80);
        }

        if (color != 0) {
            layout.setBackgroundColor(color);
        }

        if (text != null) {
            textView.setText(text);
        }

        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);

        if (isTransBackOnly) {
            layout.getBackground().setAlpha((int) (trans / 100.0 * 255));
        } else {
            layout.setAlpha(trans / 100f);
        }

    }
}