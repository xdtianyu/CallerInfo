package org.xdty.callerinfo.model.db;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.dsl.Unique;

import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.Type;
import org.xdty.phone.number.model.cloud.CloudNumber;

import rx.functions.Action1;

public class MarkedRecord extends SugarRecord {
    @Ignore
    public final static int API_ID_USER_MARKED = 8;
    private String uid;
    @Unique
    private String number;
    private int type;
    private long time;
    private int count;
    private int source;
    private boolean isReported;
    private String typeName;

    public MarkedRecord() {
        source = API_ID_USER_MARKED;
        time = System.currentTimeMillis();
        count = 0;
        isReported = false;
    }

    public static void trySave(final INumber number, final Setting setting, final Database db) {
        if (setting.isAutoReportEnabled() && number.getType() == Type.REPORT) {
            db.findMarkedRecord(number.getNumber()).subscribe(new Action1<MarkedRecord>() {
                @Override
                public void call(MarkedRecord record) {
                    if (record == null) {
                        int type = setting.getTypeFromName(number.getName());
                        if (type >= 0) {
                            MarkedRecord markedRecord = new MarkedRecord();
                            markedRecord.setNumber(number.getNumber());
                            markedRecord.setUid(setting.getUid());
                            markedRecord.setSource(number.getApiId());
                            markedRecord.setType(type);
                            markedRecord.setCount(number.getCount());
                            markedRecord.setTypeName(number.getName());
                            db.saveMarked(markedRecord);
                        }
                    }
                }
            });
        }
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

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public CloudNumber toNumber() {
        CloudNumber number = new CloudNumber();
        number.setNumber(getNumber());
        number.setCount(getCount());
        number.setType(getType());
        number.setFrom(getSource());
        number.setName(getTypeName());
        number.setUid(getUid());
        return number;
    }
}
