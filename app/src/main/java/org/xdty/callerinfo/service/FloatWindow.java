package org.xdty.callerinfo.service;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.utils.Utils;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

public class FloatWindow extends StandOutWindow {

    public final static String TAG = FloatWindow.class.getSimpleName();

    public final static String NUMBER_INFO = "number_info";
    public final static String TEXT_SIZE = "text_size";
    public final static String TEXT_PADDING = "text_padding";
    public final static String WINDOW_HEIGHT = "window_height";
    public final static String WINDOW_TRANS = "window_trans";
    public final static String WINDOW_COLOR = "window_color";
    public final static String WINDOW_ERROR = "window_error";
    private final static String WINDOW = "window";

    public final static int CALLER_FRONT = 1000;
    public final static int SET_POSITION_FRONT = 1001;
    public final static int SETTING_FRONT = 1002;
    public final static int SEARCH_FRONT = 1003;

    public final static int STATUS_CLOSE = 0;
    private final static int STATUS_SHOWING = 1;
    private final static int STATUS_HIDE = 2;

    private final static int TEXT_ALIGN_LEFT = 0;
    private final static int TEXT_ALIGN_CENTER = 1;
    private final static int TEXT_ALIGN_RIGHT = 2;

    private static int mShowingStatus = STATUS_CLOSE;

    private SharedPreferences sharedPreferences;
    private boolean isFirstShow = false;
    private boolean isFocused = false;

    public static int status() {
        return mShowingStatus;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            return super.onStartCommand(intent, flags, startId);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

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
        Utils.checkLocale(getBaseContext());

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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int defaultHeight = point.y / 8;
        int height = preferences.getInt(getString(R.string.window_height_key), defaultHeight);

        StandOutLayoutParams standOutLayoutParams = new StandOutLayoutParams(id, point.x, height,
                StandOutLayoutParams.CENTER, StandOutLayoutParams.CENTER);

        sharedPreferences = getSharedPreferences(WINDOW, Context.MODE_PRIVATE);

        int x = sharedPreferences.getInt("x", -1);
        int y = sharedPreferences.getInt("y", -1);

        if (x != -1 && y != -1) {
            standOutLayoutParams.x = x;
            standOutLayoutParams.y = y;
        }

        if (id == SETTING_FRONT || id == SEARCH_FRONT) {
            standOutLayoutParams.y = (int) (defaultHeight * 1.5);
        }

        standOutLayoutParams.minWidth = point.x;
        standOutLayoutParams.maxWidth = point.x;
        standOutLayoutParams.minHeight = defaultHeight / 4;
        if (isUnmovable(id)) {
            standOutLayoutParams.type = StandOutLayoutParams.TYPE_SYSTEM_OVERLAY;
        }
        return standOutLayoutParams;
    }

    // move the window by dragging the view
    @Override
    public int getFlags(int id) {
        if (isUnmovable(id)) {
            return super.getFlags(id) | StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
        } else {
            return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
                    | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
                    | StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE;
        }
    }

    private boolean isUnmovable(int id) {
        KeyguardManager km = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        return id == SETTING_FRONT || id == SEARCH_FRONT || km.inKeyguardRestrictedInputMode();
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
    public boolean onShow(int id, Window window) {
        isFirstShow = true;
        mShowingStatus = STATUS_SHOWING;
        return super.onShow(id, window);
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
        mShowingStatus = STATUS_CLOSE;
        return false;
    }

    @Override
    public boolean onHide(int id, Window window) {
        mShowingStatus = STATUS_HIDE;
        return super.onHide(id, window);
    }

    @Override
    public boolean onTouchBody(int id, Window window, View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_OUTSIDE:
                View layout = window.findViewById(R.id.window_layout);
                if (layout != null) {
                    layout.setBackgroundResource(0);
                }
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                boolean hideWhenTouch = preferences.getBoolean(
                        getString(R.string.hide_when_touch_key), false);
                if (!isFocused && hideWhenTouch && id == CALLER_FRONT && getWindow(id) != null) {
                    hide(id);
                }
                isFocused = false;
                break;
        }
        return super.onTouchBody(id, window, view, event);
    }

    @Override
    public void onReceiveData(int id, int requestCode, Bundle data,
            Class<? extends StandOutWindow> fromCls, int fromId) {
        int color = data.getInt(WINDOW_COLOR);
        String text = data.getString(NUMBER_INFO);
        int size = data.getInt(TEXT_SIZE);
        int height = data.getInt(WINDOW_HEIGHT);
        int trans = data.getInt(WINDOW_TRANS);
        int error = data.getInt(WINDOW_ERROR);
        int padding = data.getInt(TEXT_PADDING);
        Window window = getWindow(id);

        if (window == null) {
            return;
        }

        View layout = window.findViewById(R.id.content);
        TextView textView = (TextView) window.findViewById(R.id.number_info);
        TextView errorText = (TextView) window.findViewById(R.id.error);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean isTransBackOnly = preferences.getBoolean(
                getString(R.string.window_trans_back_only_key), true);
        boolean enableTextColor = preferences.getBoolean(
                getString(R.string.window_text_color_key), false);

        if (padding == 0) {
            padding = preferences.getInt(getString(R.string.window_text_padding_key), 0);
        }

        if (id == CALLER_FRONT || id == SETTING_FRONT) {
            int alignType = preferences.getInt(getString(R.string.window_text_alignment_key), 1);
            int gravity;
            switch (alignType) {
                case TEXT_ALIGN_LEFT:
                    gravity = Gravity.START | Gravity.CENTER;
                    textView.setPadding(padding, 0, 0, 0);
                    break;
                case TEXT_ALIGN_CENTER:
                    gravity = Gravity.CENTER;
                    textView.setPadding(0, padding, 0, 0);
                    break;
                case TEXT_ALIGN_RIGHT:
                    gravity = Gravity.END | Gravity.CENTER;
                    textView.setPadding(0, 0, padding, 0);
                    break;
                default:
                    gravity = Gravity.CENTER;
                    textView.setPadding(padding, 0, 0, 0);
                    break;
            }
            errorText.setGravity(gravity);
            textView.setGravity(gravity);
        }

        if (size == 0) {
            size = preferences.getInt(getString(R.string.window_text_size_key), 20);
        }

        if (height != 0) {
            StandOutLayoutParams params = window.getLayoutParams();
            window.edit().setSize(params.width, height).commit();
        }

        if (trans == 0) {
            trans = preferences.getInt(getString(R.string.window_transparent_key), 80);
        }

        if (color != 0) {
            layout.setBackgroundColor(color);
            if (enableTextColor && id == CALLER_FRONT) {
                textView.setTextColor(color);
            }
        }

        if (text != null) {
            textView.setText(text);
        }

        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);

        if (isTransBackOnly) {
            if (layout.getBackground() != null) {
                layout.getBackground().setAlpha((int) (trans / 100.0 * 255));
            }
        } else {
            layout.setAlpha(trans / 100f);
        }

        if (error != 0) {
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(getString(error));
        }
    }

    @Override
    public boolean onFocusChange(int id, Window window, boolean focus) {
        View layout = window.findViewById(R.id.window_layout);
        if (focus && layout != null && !isFirstShow) {
            layout.setBackgroundResource(wei.mark.standout.R.drawable.border_focused);
            isFocused = true;
        }
        isFirstShow = false;
        return true;
    }
}