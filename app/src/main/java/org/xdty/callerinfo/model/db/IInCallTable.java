package org.xdty.callerinfo.model.db;

import android.os.Parcelable;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Table;

@Table(name = "IN_CALL")
@Entity
public interface IInCallTable extends Parcelable {

    @Column(name = "NUMBER")
    String getNumber();

    @Column(name = "TIME")
    long getTime();

    @Column(name = "RING_TIME")
    long getRingTime();

    @Column(name = "DURATION")
    long getDuration();
}
