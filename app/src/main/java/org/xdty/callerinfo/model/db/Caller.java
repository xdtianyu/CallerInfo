package org.xdty.callerinfo.model.db;

import android.text.TextUtils;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.dsl.Unique;

import org.xdty.callerinfo.utils.Config;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.Type;

public class Caller extends SugarRecord implements INumber {
    @Unique
    private String number;
    private String name;
    private String type;
    private int count;
    private String province;
    private String operators;
    private String city;
    private long lastUpdate;
    private boolean isOffline = true;
    private int source = -9999;

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
        this.source = number.getApiId();
        lastUpdate = System.currentTimeMillis();
    }

    public Caller(INumber number, boolean isOffline) {
        this(number);
        this.isOffline = isOffline;
    }

    public static Caller empty(boolean isOnline) {
        return empty(isOnline, null);
    }

    public static Caller empty(boolean isOnline, INumber iNumber) {
        Caller caller = new Caller();
        caller.setOffline(!isOnline);
        if (iNumber != null) {
            caller.patch(iNumber);
        }
        return caller;
    }

    public boolean isEmpty() {
        return number == null;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public String getProvider() {
        return operators;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public boolean hasGeo() {
        return !TextUtils.isEmpty(getProvince()) || !TextUtils.isEmpty(getCity())
                || !TextUtils.isEmpty(getProvider());
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public int getApiId() {
        return source;
    }

    @Override
    public void patch(INumber i) {
        province = i.getProvince();
        city = i.getCity();
        operators = i.getProvider();
    }

    public String getSource() {
        return Utils.sourceFromId(source);
    }

    public void setSource(int source) {
        this.source = source;
    }

    public String getProvince() {
        return province != null ? province : "";
    }

    @Override
    public Type getType() {
        return Type.fromString(type);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperators() {
        return operators != null ? operators : "";
    }

    public String getCity() {
        return city != null ? city : "";
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
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

    public String getGeo() {
        if (getProvince().equals(city) || getCity().isEmpty()) {
            return getProvince() + " " + getOperators();
        } else {
            return getProvince() + " " + getCity() + " " + getOperators();
        }
    }

    public boolean canMark() {
        return (getName() == null || getName().isEmpty()) && isOffline() ||
                getApiId() < 0 && getApiId() != INumber.API_ID_CALLER;
    }

    public boolean isMark() {
        return getApiId() == -9999;
    }
}
