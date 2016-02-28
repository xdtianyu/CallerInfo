package org.xdty.callerinfo.model.db;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import org.xdty.callerinfo.utils.Config;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.Type;

public class Caller extends SugarRecord implements INumber {
    private String number;
    private String name;
    private String type;
    private int count;
    private String province;
    private String operators;
    private String city;
    private long lastUpdate;
    private boolean isOffline = true;

    @Ignore
    private String contactName;

    public Caller() {
    }

    public Caller(INumber number) {
        this.number = number.getNumber();
        this.name = number.getName();
        this.type = number.getType().getText();
        this.count = number.getCount();
        this.province = number.getProvince();
        this.city = number.getCity();
        this.operators = number.getProvider();
        lastUpdate = System.currentTimeMillis();
    }

    public Caller(INumber number, boolean isOffline) {
        this(number);
        this.isOffline = isOffline;
    }

    public String getNumber() {
        return number;
    }

    @Override
    public String getProvider() {
        return operators;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public int getApiId() {
        return 0;
    }

    public String getProvince() {
        return province;
    }

    @Override
    public Type getType() {
        return Type.fromString(type);
    }

    public String getOperators() {
        return operators;
    }

    public String getCity() {
        return city;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public String toString() {
        String s = "";

        s += number;
        if (name != null) {
            s += "-" + name;
        }

        if (count != 0) {
            s += "-" + count;
        }

        if (province != null) {
            s += "-" + province;
        }
        if (city != null) {
            s += "-" + city;
        }
        if (operators != null) {
            s += "-" + operators;
        }
        return s;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public void setOffline(boolean isOffline) {
        this.isOffline = isOffline;
    }

    public boolean isUpdated() {
        return !isOffline && lastUpdate - System.currentTimeMillis() < Config.MAX_UPDATE_CIRCLE;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }
}
