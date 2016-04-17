package org.xdty.callerinfo.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.pkmmte.view.CircularImageView;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.utils.Utils;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

public class MarkWindow extends StandOutWindow {
    private static final String TAG = MarkWindow.class.getSimpleName();
    private WindowManager mWindowManager;
    private Setting mSettings;

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            mSettings = new SettingImpl(getApplicationContext());
            return super.onStartCommand(intent, flags, startId);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    @Override
    public String getAppName() {
        return getResources().getString(R.string.mark_window);
    }

    @Override
    public int getAppIcon() {
        return R.drawable.status_icon;
    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {
        Utils.checkLocale(getBaseContext());

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.mark_window, frame, true);
        
        bindCircleImage(view, R.id.express);
        bindCircleImage(view, R.id.takeout);
        bindCircleImage(view, R.id.selling);
        bindCircleImage(view, R.id.harass);
        bindCircleImage(view, R.id.bilk);
    }

    private void bindCircleImage(View view, int id) {
        final CircularImageView circularImageView = (CircularImageView) view.findViewById(id);
        circularImageView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.e(TAG, v.toString() + ": " + hasFocus);
            }
        });
        circularImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, v.toString() + "xxx");
            }
        });
    }

    @Override
    public void onMove(int id, Window window, View view, MotionEvent event) {
        super.onMove(id, window, view, event);
        int x = window.getLayoutParams().x;
        int width = mSettings.getScreenWidth();
        View layout = window.findViewById(R.id.content);
        float alpha = (float) ((width - Math.abs(x) * 1.2) / width);
        layout.setAlpha(alpha);
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (alpha < 0.6) {
                    hide(id);
                } else {
                    reset(id);
                    layout.setAlpha(1.0f);
                }
                break;
        }
    }

    public void reset(int id) {
        final Window window = getWindow(id);
        mWindowManager.updateViewLayout(window, getParams(id, window));
    }

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        StandOutLayoutParams params = new StandOutLayoutParams(id, mSettings.getScreenWidth(),
                mSettings.getWindowHeight(), StandOutLayoutParams.CENTER,
                StandOutLayoutParams.CENTER);

        int x = mSettings.getWindowX();
        int y = mSettings.getWindowY();

        if (x != -1 && y != -1) {
            params.x = x;
            params.y = y;
        }

        params.y = (int) (mSettings.getDefaultHeight() * 1.5);

        params.minWidth = mSettings.getScreenWidth();
        params.maxWidth = Math.max(mSettings.getScreenWidth(), mSettings.getScreenHeight());
        params.minHeight = mSettings.getDefaultHeight() * 2;
        params.height = mSettings.getDefaultHeight() * 5;
        return params;
    }

    @Override
    public int getFlags(int id) {
        return StandOutFlags.FLAG_BODY_MOVE_ENABLE
                | StandOutFlags.FLAG_WINDOW_FOCUS_INDICATOR_DISABLE;
    }

    @Override
    public int getThemeStyle() {
        return R.style.AppTheme;
    }

    @Override
    public boolean isDisableMove(int id) {
        return false;
    }

}
