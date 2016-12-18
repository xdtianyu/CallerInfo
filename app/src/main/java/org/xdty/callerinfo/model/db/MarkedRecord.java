package org.xdty.callerinfo.model.db;

import org.xdty.callerinfo.R;
import org.xdty.phone.number.model.cloud.CloudNumber;

public class MarkedRecord extends MarkedRecordTable {
    public final static int API_ID_USER_MARKED = 8;
    public final static int TYPE_IGNORE = 32;

    public MarkedRecord() {
        setSource(API_ID_USER_MARKED);
        setTime(System.currentTimeMillis());
        setCount(0);
        setReported(false);
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
        return getType() == TYPE_IGNORE;
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
