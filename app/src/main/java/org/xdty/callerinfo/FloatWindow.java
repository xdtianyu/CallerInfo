package org.xdty.callerinfo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

public class FloatWindow extends StandOutWindow {

    public final static String LOCATION = "location";
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

        int height = point.y/10;

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
    public String getPersistentNotificationMessage(int id) {
        return "Click to close the FloatWindow";
    }

    @Override
    public Intent getPersistentNotificationIntent(int id) {
        return StandOutWindow.getCloseIntent(this, FloatWindow.class, id);
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
    public void onReceiveData(int id, int requestCode, Bundle data,
            Class<? extends StandOutWindow> fromCls, int fromId) {
        String text = data.getString(LOCATION);
        Window window = getWindow(id);
        TextView textView = (TextView) window.findViewById(R.id.location);
        textView.setText(text);
    }
}