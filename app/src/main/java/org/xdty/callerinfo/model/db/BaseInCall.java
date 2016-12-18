package org.xdty.callerinfo.model.db;

import android.text.TextUtils;

import org.xdty.callerinfo.utils.Utils;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.Table;
import io.requery.Transient;

@Table(name = "IN_CALL")
@Entity
public abstract class BaseInCall {

    @Key
    @Generated
    @Column(name = "ID")
    int id;

    @Column(name = "NUMBER")
    String number;

    @Column(name = "TIME")
    long time;

    @Column(name = "RING_TIME")
    long ringTime;

    @Column(name = "DURATION")
    long duration;

    @Transient
    boolean isExpanded = false;

    public BaseInCall() {
    }

    public BaseInCall(String number, long time, long ringTime, long duration) {

        if (!TextUtils.isEmpty(number)) {
            number = number.replaceAll(" ", "");
        }

        this.number = number;
        this.time = time;
        this.ringTime = ringTime;
        this.duration = duration;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public String getReadableTime() {
        return Utils.readableDate(time) + " " + Utils.getTime(time);
    }
}
