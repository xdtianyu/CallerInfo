package org.xdty.callerinfo.model.db;

import com.orm.SugarRecord;

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

    public Caller() {
    }

    public Caller(Number number) {
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
}
