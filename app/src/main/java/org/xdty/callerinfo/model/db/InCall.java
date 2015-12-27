package org.xdty.callerinfo.model.db;

import com.orm.SugarRecord;

public class InCall extends SugarRecord {
    String number;
    long time;
    long ringTime;
    long duration;

    public InCall() {
    }

    public InCall(String number, long time, long ringTime, long duration) {
        this.number = number;
        this.time = time;
        this.ringTime = ringTime;
        this.duration = duration;
    }
}
