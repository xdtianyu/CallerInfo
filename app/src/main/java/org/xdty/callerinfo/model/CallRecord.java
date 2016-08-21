package org.xdty.callerinfo.model;

public class CallRecord {

    private long ring = -1;
    private long hook = -1;
    private long idle = -1;
    private long ringDuration = -1;
    private long callDuration = -1;

    private String mLogNumber;
    private String mLogName;
    private String mLogGeo;

    public String getLogName() {
        return mLogName;
    }

    public void setLogName(String name) {
        this.mLogName = name;
    }

    public void setLogName(String name, boolean append) {
        if (append) {
            appendName(name);
        } else {
            setLogName(name);
        }
    }

    public String getLogGeo() {
        return mLogGeo;
    }

    public void setLogGeo(String name) {
        this.mLogGeo = name;
    }

    public void ring() {
        ring = System.currentTimeMillis();
    }

    public void hook() {
        hook = System.currentTimeMillis();
    }

    public long getHook() {
        return hook;
    }

    public void idle() {
        idle = System.currentTimeMillis();

        if (isIncoming()) {
            if (hook == -1) { // missed or hangup incoming call
                ringDuration = idle - ring;
                callDuration = 0;
            } else { // answered incoming call
                ringDuration = hook - ring;
                callDuration = idle - hook;
            }
        } else {  // outgoing call
            ringDuration = 0;
            callDuration = idle - hook;
        }
    }

    public boolean isIncoming() {
        return ring != -1;
    }

    public long ringDuration() {
        return ringDuration;
    }

    public long callDuration() {
        return callDuration;
    }

    public void reset() {
        ring = -1;
        hook = -1;
        idle = -1;
        ringDuration = -1;
        callDuration = -1;

        mLogNumber = null;
        mLogGeo = null;
        mLogName = null;
    }

    public long time() {
        return ring != -1 ? ring : hook;
    }

    public String getLogNumber() {
        return mLogNumber;
    }

    public void setLogNumber(String mLogNumber) {
        this.mLogNumber = mLogNumber;
    }

    public void appendName(String s) {
        if (mLogName == null || mLogName.isEmpty()) {
            mLogName = "";
        }
        mLogName += " " + s;
    }

    public boolean isValid() {
        return mLogNumber != null && !mLogNumber.isEmpty();
    }

    public boolean isNameValid() {
        return mLogName != null && !mLogName.isEmpty();
    }

    public boolean isGeoValid() {
        return mLogGeo != null && !mLogGeo.isEmpty();
    }

    public boolean matchName(String keyword) {
        return mLogName != null && mLogName.contains(keyword);
    }

    public boolean matchGeo(String keyword) {
        return mLogGeo.contains(keyword);
    }

    public boolean matchNumber(String keyword) {
        return mLogNumber != null && mLogNumber.startsWith(keyword);
    }

    public boolean isActive() {
        return ring != -1 || hook != -1 || idle != -1;
    }

    public boolean isAnswered() {
        return isIncoming() && callDuration > 0;
    }

    public boolean isEqual(String number) {
        return (mLogNumber == null && number == null)
                || (mLogNumber != null && number != null && mLogNumber.equals(number));
    }
}
