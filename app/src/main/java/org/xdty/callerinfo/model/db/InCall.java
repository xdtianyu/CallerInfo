package org.xdty.callerinfo.model.db;

import android.text.TextUtils;

import org.xdty.callerinfo.utils.Utils;

public class InCall extends InCallTable {

    private boolean isExpanded = false;

    public InCall() {
    }

    public InCall(String number, long time, long ringTime, long duration) {
        setNumber(number);
        setTime(time);
        setRingTime(ringTime);
        setDuration(duration);
    }

    public String getNumber() {
        String number = super.getNumber();
        if (!TextUtils.isEmpty(number)) {
            number = number.replaceAll(" ", "");
        }
        return number;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public String getReadableTime() {
        return Utils.readableDate(getTime()) + " " + Utils.getTime(getTime());
    }
}
