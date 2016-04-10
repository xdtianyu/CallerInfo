package org.xdty.callerinfo.service;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
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

    private boolean isFirstShow = false;
    private boolean isFocused = false;

    private Setting mSettings;

    public static int status() {
        return mShowingStatus;
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

        StandOutLayoutParams params = new StandOutLayoutParams(id, mSettings.getScreenWidth(),
                mSettings.getWindowHeight(), StandOutLayoutParams.CENTER,
                StandOutLayoutParams.CENTER);

        int x = mSettings.getWindowX();
        int y = mSettings.getWindowY();

        if (x != -1 && y != -1) {
            params.x = x;
            params.y = y;
        }

        if (id == SETTING_FRONT || id == SEARCH_FRONT) {
            params.y = (int) (mSettings.getDefaultHeight() * 1.5);
        }

        params.minWidth = mSettings.getScreenWidth();
        params.maxWidth = Math.max(mSettings.getScreenWidth(), mSettings.getScreenHeight());
        params.minHeight = mSettings.getDefaultHeight() / 4;
        if (isUnmovable(id)) {
            params.type = StandOutLayoutParams.TYPE_SYSTEM_OVERLAY;
        }
        return params;
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
        if (mSettings.isShowCloseAnim()) {
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
        mSettings.setWindow(window.getLayoutParams().x, window.getLayoutParams().y);
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
                if (!isFocused && mSettings.isHidingWhenTouch() && id == CALLER_FRONT
                        && getWindow(id) != null) {
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

        if (padding == 0) {
            padding = mSettings.getTextPadding();
        }

        if (id == CALLER_FRONT || id == SETTING_FRONT) {
            int alignType = mSettings.getTextAlignment();
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
            size = mSettings.getTextSize();
        }

        if (height != 0) {
            StandOutLayoutParams params = window.getLayoutParams();
            window.edit().setSize(params.width, height).commit();
        }

        if (trans == 0) {
            trans = mSettings.getWindowTransparent();
        }

        if (color != 0) {
            layout.setBackgroundColor(color);
            if (mSettings.isEnableTextColor() && id == CALLER_FRONT) {
                textView.setTextColor(color);
            }
        }

        if (text != null) {
            textView.setText(text);
        }

        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);

        if (mSettings.isTransBackOnly()) {
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

    @Override
    public boolean isDisableMove(int id) {
        return id == CALLER_FRONT && mSettings.isDisableMove();
    }
}