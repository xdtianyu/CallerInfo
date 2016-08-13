package org.xdty.callerinfo.model.db;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.dsl.Unique;

import org.xdty.callerinfo.R;
import org.xdty.phone.number.model.cloud.CloudNumber;

public class MarkedRecord extends SugarRecord {
    @Ignore
    public final static int API_ID_USER_MARKED = 8;
    @Ignore
    public final static int TYPE_IGNORE = 32;
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

    public boolean isIgnore() {
        return type == TYPE_IGNORE;
    }

    public enum MarkType {
        HARASSMENT(0),
        FRAUD(1),
        ADVERTISING(2),
        EXPRESS_DELIVERY(3),
        RESTAURANT_DELIVER(4),
        CUSTOM(5);

        private int mType;

        MarkType(int type) {
            mType = type;
        }

        public static MarkType fromInt(int value) {
            if (value >= 0 && value < MarkType.values().length) {
                return MarkType.values()[value];
            }
            return CUSTOM;
        }

        public static MarkType fromResourceId(int id) {
            switch (id) {
                case R.id.fraud:
                    return FRAUD;
                case R.id.harassment:
                    return HARASSMENT;
                case R.id.advertising:
                    return ADVERTISING;
                case R.id.express:
                    return EXPRESS_DELIVERY;
                case R.id.restaurant:
                    return RESTAURANT_DELIVER;
                default:
                    return CUSTOM;
            }
        }

        public int toInt() {
            return mType;
        }
    }
}
