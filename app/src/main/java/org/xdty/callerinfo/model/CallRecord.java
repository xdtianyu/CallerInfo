package org.xdty.callerinfo.model;

public class CallRecord {

    private long ringTime = -1;
    private long hookTime = -1;
    private long idleTime = -1;
    private long ringDuration = -1;
    private long callDuration = -1;

    public void ring() {
        ringTime = System.currentTimeMillis();
    }

    public void hook() {
        hookTime = System.currentTimeMillis();
    }

    public void idle() {
        idleTime = System.currentTimeMillis();
    }

    public long ringTime() {
        return ringTime;
    }

    public long hookTime() {
        return hookTime;
    }

    public long idleTime() {
        return idleTime;
    }

    public long ringDuration() {
        return ringDuration;
    }

    public long callDuration() {
        return callDuration;
    }

    public void setRingDuration(boolean picked) {
        if (picked) {
            ringDuration = hookTime() - ringTime();
        } else {
            ringTime = idleTime() - ringTime();
        }

    }

    public void setCallDuration(boolean picked) {
        if (picked) {
            callDuration = idleTime() - hookTime();
        } else {
            callDuration = 0;
        }
    }

    public void reset() {
        ringTime = -1;
        hookTime = -1;
        idleTime = -1;
        ringDuration = -1;
        callDuration = -1;
    }
}
