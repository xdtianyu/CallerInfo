package org.xdty.callerinfo.model.db;

import android.text.TextUtils;

import org.xdty.callerinfo.utils.Config;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.Type;

public class Caller extends CallerTable implements INumber {

    private final static int DEFAULT_SOURCE = -9999;
    private String contactName;

    public Caller() {
        setCallerSource(DEFAULT_SOURCE);
    }

    public Caller(INumber number) {
        setNumber(number.getNumber());
        setName(number.getName());
        setType(number.getType().getText());
        setCount(number.getCount());
        setProvince(number.getProvince());
        setCity(number.getCity());
        setOperators(number.getProvider());
        setCallerSource(number.getApiId());
        setLastUpdate(System.currentTimeMillis());
    }

    public Caller(INumber number, boolean isOffline) {
        this(number);
        setOffline(isOffline);
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
        return getNumber() == null;
    }

    @Override
    public String getProvider() {
        return getOperators();
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
        return super.getCallerSource();
    }

    @Override
    public void patch(INumber i) {
        setProvince(i.getProvince());
        setCity(i.getCity());
        setOperators(i.getProvider());
    }

    public String getSource() {
        return Utils.sourceFromId(super.getCallerSource());
    }

    public void setSource(int apiId) {
        setCallerSource(apiId);
    }

    public String getProvince() {
        String province = super.getProvince();
        return province != null ? province.replace("省", " ").replace("市", " ") : "";
    }

    @Override
    public Type getType() {

        if (getCallerSource() == DEFAULT_SOURCE) { // get type from type name
            return Utils.markTypeFromName(getName());
        }

        return Type.fromString(getCallerType());
    }

    public void setType(String type) {
        setCallerType(type);
    }

    public String getOperators() {
        return super.getOperators() != null ? super.getOperators() : "";
    }

    public String getCity() {
        return super.getCity() != null ? super.getCity().replace("市", " ") : "";
    }

    public boolean isUpdated() {
        return !isOffline() && getLastUpdate() - System.currentTimeMillis() <
                Config.MAX_UPDATE_CIRCLE;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getGeo() {
        if (getProvince().equals(super.getCity()) || getCity().isEmpty()) {
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
