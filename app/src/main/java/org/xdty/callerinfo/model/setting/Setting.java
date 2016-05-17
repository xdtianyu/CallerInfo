package org.xdty.callerinfo.model.setting;

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

    int getTypeFromName(String name);

}
