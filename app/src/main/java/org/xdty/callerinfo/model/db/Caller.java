package org.xdty.callerinfo.model.db;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import org.xdty.callerinfo.utils.Config;
import org.xdty.phone.number.model.Location;
import org.xdty.phone.number.model.Number;

public class Caller extends SugarRecord {
    String number;
    String name;
    String type;
    int count;
    String province;
    String operators;
    String city;
    long lastUpdate;
    boolean isOffline = true;

    @Ignore
    Number phoneNumber;

    public Caller() {
    }

    public Caller(Number number) {
        this.phoneNumber = number;
        this.number = number.getNumber();
        this.name = number.getName();
        this.type = number.getType().getText();
        this.count = number.getCount();
        Location location = number.getLocation();
        if (location != null) {
            this.province = location.getProvince();
            this.city = location.getCity();
            this.operators = location.getOperators();
        }
        lastUpdate = System.currentTimeMillis();
    }

    public Caller(Number number, boolean isOffline) {
        this(number);
        this.isOffline = isOffline;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public String getProvince() {
        return province;
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

    public Number toNumber() {
        if (phoneNumber == null) {
            phoneNumber = new Number();
            phoneNumber.setNumber(number);
            phoneNumber.setCount(count);
            phoneNumber.setName(name);
            phoneNumber.setType(type);
            Location location = new Location();
            location.setCity(city);
            location.setOperators(operators);
            location.setProvince(province);
            phoneNumber.setLocation(location);
        }
        return phoneNumber;
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

    public boolean needUpdate() {
        return isOffline || lastUpdate - System.currentTimeMillis() >= Config.MAX_UPDATE_CIRCLE;
    }
}
