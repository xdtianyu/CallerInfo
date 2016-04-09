package org.xdty.callerinfo.model;

public class CallRecord {

    private long ring = -1;
    private long hook = -1;
    private long idle = -1;
    private long ringDuration = -1;
    private long callDuration = -1;

    public void ring() {
        ring = System.currentTimeMillis();
    }

    public void hook() {
        hook = System.currentTimeMillis();
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
    }

    public long time() {
        return ring != -1 ? ring : hook;
    }
}
