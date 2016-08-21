package org.xdty.callerinfo.model.setting;

import org.xdty.phone.number.model.caller.Status;

import java.util.ArrayList;

public interface Setting {

    boolean isEulaSet();

    void setEula();

    String getIgnoreRegex();

    boolean isHidingOffHook();

    boolean isShowingOnOutgoing();

    boolean isIgnoreKnownContact();

    boolean isShowingContactOffline();

    boolean isAutoHangup();

    boolean isAddingCallLog();

    boolean isCatchCrash();

    boolean isForceChinese();

    String getKeywords();

    String getGeoKeyword();

    String getNumberKeyword();

    int getWindowX();

    int getWindowY();

    void setWindow(int x, int y);

    int getScreenWidth();

    int getScreenHeight();

    int getStatusBarHeight();

    int getWindowHeight();

    int getDefaultHeight();

    boolean isShowCloseAnim();

    boolean isHidingWhenTouch();

    boolean isTransBackOnly();

    boolean isEnableTextColor();

    int getTextPadding();

    int getTextAlignment();

    int getTextSize();

    int getWindowTransparent();

    boolean isDisableMove();

    boolean isAutoReportEnabled();

    boolean isMarkingEnabled();

    void addPaddingMark(String number);

    void removePaddingMark(String number);

    ArrayList<String> getPaddingMarks();

    String getUid();

    void updateLastScheduleTime();

    void updateLastScheduleTime(long timestamp);

    long lastScheduleTime();

    long lastCheckDataUpdateTime();

    void updateLastCheckDataUpdateTime(long timestamp);

    Status getStatus();

    void setStatus(Status status);

    boolean isNotMarkContact();

    boolean isDisableOutGoingHangup();

    boolean isTemporaryDisableHangup();

    int getRepeatedCountIndex();

    void clear();

    int getNormalColor();

    int getPoiColor();

    int getReportColor();

    boolean isOnlyOffline();

    void fix();

    void setOutgoing(boolean isOutgoing);

    boolean isOutgoingPositionEnabled();

    boolean isAddingRingOnceCallLog();
}
