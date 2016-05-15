package org.xdty.callerinfo.model.db;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

public class MarkedRecord extends SugarRecord {
    private String uid;
    private String number;
    private int type;
    private long time;
    private int count;
    private int source;
    private boolean isReported;

    @Ignore
    private final static int API_ID_USER_MARKED = 2;

    public MarkedRecord() {
        source = API_ID_USER_MARKED;
        time = System.currentTimeMillis();
        count = 0;
        isReported = false;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public boolean isReported() {
        return isReported;
    }

    public void setReported(boolean reported) {
        isReported = reported;
    }
}
