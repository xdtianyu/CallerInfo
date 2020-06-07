package org.xdty.callerinfo.model.setting

import org.xdty.callerinfo.model.Status

interface Setting {
    val isEulaSet: Boolean
    fun setEula()
    val ignoreRegex: String
    val isHidingOffHook: Boolean
    val isShowingOnOutgoing: Boolean
    val isIgnoreKnownContact: Boolean
    val isShowingContactOffline: Boolean
    val isAutoHangup: Boolean
    val isAddingCallLog: Boolean
    val isCatchCrash: Boolean
    val isForceChinese: Boolean
    val keywords: String
    val geoKeyword: String
    val numberKeyword: String
    val windowX: Int
    val windowY: Int
    fun setWindow(x: Int, y: Int)
    val screenWidth: Int
    val screenHeight: Int
    val statusBarHeight: Int
    val windowHeight: Int
    val defaultHeight: Int
    val isShowCloseAnim: Boolean
    val isHidingWhenTouch: Boolean
    val isTransBackOnly: Boolean
    val isEnableTextColor: Boolean
    val textPadding: Int
    val textAlignment: Int
    val textSize: Int
    val windowTransparent: Int
    val isDisableMove: Boolean
    val isAutoReportEnabled: Boolean
    val isMarkingEnabled: Boolean
    fun addPaddingMark(number: String)
    fun removePaddingMark(number: String)
    val paddingMarks: ArrayList<String>
    val uid: String
    fun updateLastScheduleTime()
    fun updateLastScheduleTime(timestamp: Long)
    fun lastScheduleTime(): Long
    fun lastCheckDataUpdateTime(): Long
    fun updateLastCheckDataUpdateTime(timestamp: Long)
    var status: Status
    val isNotMarkContact: Boolean
    val isDisableOutGoingHangup: Boolean
    val isTemporaryDisableHangup: Boolean
    val repeatedCountIndex: Int
    fun clear()
    val normalColor: Int
    val poiColor: Int
    val reportColor: Int
    val isOnlyOffline: Boolean
    fun fix()
    fun setOutgoing(isOutgoing: Boolean)
    val isOutgoingPositionEnabled: Boolean
    val isAddingRingOnceCallLog: Boolean
    val isOfflineDataAutoUpgrade: Boolean
}