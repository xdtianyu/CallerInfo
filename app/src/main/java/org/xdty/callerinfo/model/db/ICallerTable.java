package org.xdty.callerinfo.model.db;

import android.os.Parcelable;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Table;

@Table(name = "CALLER")
@Entity
public interface ICallerTable extends Parcelable {

    @Column(name = "NUMBER", unique = true)
    String getNumber();

    @Column(name = "NAME")
    String getName();

    @Column(name = "TYPE")
    String getCallerType();

    @Column(name = "COUNT")
    int getCount();

    @Column(name = "PROVINCE")
    String getProvince();

    @Column(name = "OPERATORS")
    String getOperators();

    @Column(name = "CITY")
    String getCity();

    @Column(name = "LAST_UPDATE")
    long getLastUpdate();

    @Column(name = "IS_OFFLINE")
    boolean isOffline();

    @Column(name = "SOURCE")
    int getCallerSource();
}
