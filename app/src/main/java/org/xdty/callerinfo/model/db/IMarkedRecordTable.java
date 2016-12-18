package org.xdty.callerinfo.model.db;

import android.os.Parcelable;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Table;

@Table(name = "MARKED_RECORD")
@Entity
public interface IMarkedRecordTable extends Parcelable{

    @Column(name = "NUMBER", unique = true)
    String getNumber();

    @Column(name = "UID")
    String getUid();

    @Column(name = "TYPE")
    int getType();

    @Column(name = "TIME")
    long getTime();

    @Column(name = "COUNT")
    int getCount();

    @Column(name = "SOURCE")
    int getSource();

    @Column(name = "REPORTED")
    boolean isReported();

    @Column(name = "TYPE_NAME")
    String getTypeName();

}
