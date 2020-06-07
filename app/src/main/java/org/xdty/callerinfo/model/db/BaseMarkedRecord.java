package org.xdty.callerinfo.model.db;

import org.xdty.phone.number.model.cloud.CloudNumber;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.Table;

@Table(name = "MARKED_RECORD")
@Entity
public abstract class BaseMarkedRecord {
    public final static int API_ID_USER_MARKED = 8;
    public final static int TYPE_IGNORE = 32;

    @Key
    @Generated
    @Column(name = "ID")
    int id;

    @Column(name = "NUMBER", unique = true)
    String number;

    @Column(name = "UID")
    String uid;

    @Column(name = "TYPE")
    int type;

    @Column(name = "TIME")
    long time;

    @Column(name = "COUNT")
    int count;

    @Column(name = "SOURCE")
    int source;

    @Column(name = "IS_REPORTED")
    boolean reported;

    @Column(name = "TYPE_NAME")
    String typeName;

    public BaseMarkedRecord() {
        this.source = API_ID_USER_MARKED;
        this.time = System.currentTimeMillis();
        this.count = 0;
        this.reported = false;
    }

    public CloudNumber toNumber() {
        CloudNumber number = new CloudNumber();
        number.setNumber(this.number);
        number.setCount(this.count);
        number.setType(this.type);
        number.setFrom(this.source);
        number.setName(this.typeName);
        number.setUid(this.uid);
        return number;
    }

    public boolean isIgnore() {
        return this.type == TYPE_IGNORE;
    }
}
