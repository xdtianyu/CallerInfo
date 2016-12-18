package org.xdty.callerinfo.model.db;

import org.xdty.callerinfo.R;
import org.xdty.phone.number.model.cloud.CloudNumber;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.Table;

@Table(name = "MARKED_RECORD")
@Entity
public abstract class BaseMarkedRecord {
    public final static int API_ID_USER_MARKED = 8;
    public final static int TYPE_IGNORE = 32;

    @Key
    @Generated
    @Column(name = "ID")
    int id;

    @Column(name = "NUMBER", unique = true)
    String number;

    @Column(name = "UID")
    String uid;

    @Column(name = "TYPE")
    int type;

    @Column(name = "TIME")
    long time;

    @Column(name = "COUNT")
    int count;

    @Column(name = "SOURCE")
    int source;

    @Column(name = "REPORTED")
    boolean reported;

    @Column(name = "TYPE_NAME")
    String typeName;

    public BaseMarkedRecord() {
        this.source = API_ID_USER_MARKED;
        this.time = System.currentTimeMillis();
        this.count = 0;
        this.reported = false;
    }

    public CloudNumber toNumber() {
        CloudNumber number = new CloudNumber();
        number.setNumber(this.number);
        number.setCount(this.count);
        number.setType(this.type);
        number.setFrom(this.source);
        number.setName(this.typeName);
        number.setUid(this.uid);
        return number;
    }

    public boolean isIgnore() {
        return this.type == TYPE_IGNORE;
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
