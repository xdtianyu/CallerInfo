package org.xdty.callerinfo.model.db;

import android.text.TextUtils;

import org.xdty.callerinfo.utils.Config;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.Type;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.Table;
import io.requery.Transient;

@Table(name = "CALLER")
@Entity

public abstract class BaseCaller implements INumber {

    private final static int DEFAULT_SOURCE = -9999;

    @Key
    @Generated
    @Column(name = "ID")
    int id;

    @Column(name = "NUMBER", unique = true)
    String number;

    @Column(name = "NAME")
    String name;

    @Column(name = "TYPE")
    String callerType;

    @Column(name = "COUNT")
    int count;

    @Column(name = "PROVINCE")
    String province = "";

    @Column(name = "OPERATORS")
    String operators = "";

    @Column(name = "CITY")
    String city = "";

    @Column(name = "LAST_UPDATE")
    long lastUpdate;

    @Column(name = "IS_OFFLINE")
    boolean offline;

    @Column(name = "SOURCE")
    int callerSource;

    @Transient
    String contactName;

    public BaseCaller() {
        callerSource = DEFAULT_SOURCE;
    }

    public BaseCaller(INumber number) {
        this.number = number.getNumber();
        this.name = number.getName();
        setType(number.getType().getText());
        this.count = number.getCount();
        this.province = fixedProvince(number.getProvince());
        this.city = fixedCity(number.getCity());
        this.operators = fixedOperators(number.getProvider());
        this.callerSource = number.getApiId();
        this.lastUpdate = System.currentTimeMillis();
    }

    public BaseCaller(INumber number, boolean isOffline) {
        this(number);
        this.offline = isOffline;
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

    private String fixedProvince(String province) {
        return province != null ? province.replace("省", " ").replace("市", " ") : "";
    }

    private String fixedOperators(String operators) {
        return operators != null ? operators : "";
    }

    private String fixedCity(String city) {
        return city != null ? city.replace("市", " ") : "";
    }

    public boolean isEmpty() {
        return getNumber() == null;
    }

    @Override
    public String getProvider() {
        return this.operators;
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
        return callerSource;
    }

    @Override
    public void patch(INumber i) {
        this.province = fixedProvince(i.getProvince());
        this.city = fixedCity(i.getCity());
        this.operators = fixedOperators(i.getProvider());
    }

    public String getSource() {
        return Utils.sourceFromId(callerSource);
    }

    public void setSource(int apiId) {
        this.callerSource = apiId;
    }

    @Override
    public Type getType() {

        if (callerSource == DEFAULT_SOURCE) { // get type from type name
            return Utils.markTypeFromName(getName());
        }

        return Type.fromString(callerType);
    }

    public void setType(String type) {
        this.callerType = type;
    }

    public boolean isUpdated() {
        return !offline && lastUpdate - System.currentTimeMillis() <
                Config.MAX_UPDATE_CIRCLE;
    }

    public String getGeo() {
        if (getProvince().equals(this.city) || getCity().isEmpty()) {
            return getProvince() + " " + getProvider();
        } else {
            return getProvince() + " " + getCity() + " " + getProvider();
        }
    }

    public boolean canMark() {
        return (getName() == null || getName().isEmpty()) && offline ||
                getApiId() < 0 && getApiId() != INumber.API_ID_CALLER;
    }

    public boolean isMark() {
        return getApiId() == -9999;
    }
}
