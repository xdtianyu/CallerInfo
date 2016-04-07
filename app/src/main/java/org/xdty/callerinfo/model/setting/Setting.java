package org.xdty.callerinfo.model.setting;

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

    String getKeywords();

    String getGeoKeyword();

    String getNumberKeyword();

}
