package org.xdty.callerinfo.view;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class CallerAdapter extends RecyclerView.Adapter<CallerAdapter.ViewHolder> {

    private static final String TAG = CallerAdapter.class.getSimpleName();
    private final Context mContext;

    @Inject
    Database mDatabase;

    @Inject
    Setting mSetting;

    private Map<String, Caller> mCallerMap;
    private List<InCall> mList;

    public CallerAdapter(Context context) {
        mContext = context;
        mCallerMap = new HashMap<>();
        mList = new ArrayList<>();

        Application.getAppComponent().inject(this);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);
        return new ViewHolder(mContext, view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        InCall inCall = mList.get(position);
        Caller caller = getCaller(inCall.getNumber());
        holder.bind(inCall, caller);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private Caller getCaller(String number) {
        Caller caller = mCallerMap.get(number);

        if (caller == null) {
            if (number.contains("+86")) {
                caller = mCallerMap.get(number.replace("+86", ""));
            }
        }
        return caller;
    }

    public void attachCallerMap(Map<String, Caller> callerMap) {
        mCallerMap = callerMap;
    }

    public void replaceData(List<InCall> inCalls) {
        mList = inCalls;
        notifyDataSetChanged();
    }

    public InCall getItem(int position) {
        return mList.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, PhoneNumber.Callback {

        final Context context;
        final CardView cardView;
        final TextView text;
        final TextView number;
        final LinearLayout detail;
        final TextView time;
        final TextView ringTime;
        final TextView duration;
        InCall inCall;

        // FIXME: remove phone number callback
        PhoneNumber phoneNumber;

        public ViewHolder(Context context, View view) {
            super(view);
            this.context = context;
            cardView = (CardView) view.findViewById(R.id.card_view);
            text = (TextView) view.findViewById(R.id.text);
            number = (TextView) view.findViewById(R.id.number);
            detail = (LinearLayout) view.findViewById(R.id.detail);
            cardView.setOnClickListener(this);
            time = (TextView) view.findViewById(R.id.time);
            ringTime = (TextView) view.findViewById(R.id.ring_time);
            duration = (TextView) view.findViewById(R.id.duration);

            phoneNumber = new PhoneNumber(context, this);
        }

        public void setAlpha(float alpha) {
            cardView.setAlpha(alpha);
        }

        public void bind(InCall inCall, Caller caller) {
            if (caller != null) {
                TextColorPair t = Utils.getTextColorPair(context, caller);
                text.setText(t.text);
                //noinspection ResourceAsColor
                cardView.setCardBackgroundColor(t.color);
                number.setText(TextUtils.isEmpty(
                        caller.getContactName()) ? caller.getNumber() : caller.getContactName());
            } else {
                if (inCall.isFetched() || TextUtils.isEmpty(inCall.getNumber())) {
                    text.setText(R.string.loading_error);
                    number.setText(inCall.getNumber());
                    cardView.setCardBackgroundColor(
                            ContextCompat.getColor(context, R.color.graphite));
                } else {
                    // FIXME: fetched several times on same number
                    phoneNumber.fetch(inCall.getNumber());

                    text.setText(R.string.loading);
                    number.setText(inCall.getNumber());
                    cardView.setCardBackgroundColor(
                            ContextCompat.getColor(context, R.color.blue_light));
                }
            }
            cardView.setAlpha(1f);
            if (inCall.isExpanded()) {
                detail.setVisibility(View.VISIBLE);
            } else {
                detail.setVisibility(View.GONE);
            }
            this.inCall = inCall;

            time.setText(Utils.readableDate(context, inCall.getTime()));
            ringTime.setText(Utils.readableTime(context, inCall.getRingTime()));
            duration.setText(Utils.readableTime(context, inCall.getDuration()));
        }

        @Override
        public void onClick(View v) {
            if (detail.getVisibility() == View.VISIBLE) {
                detail.setVisibility(View.GONE);
                inCall.setExpanded(false);
            } else {
                detail.setVisibility(View.VISIBLE);
                inCall.setExpanded(true);
            }
        }

        @Override
        public void onResponseOffline(INumber number) {
            Log.v(TAG, "onResponseOffline: " + number);
            mDatabase.updateCaller(new Caller(number, !number.isOnline()));
            mCallerMap.put(number.getNumber(), new Caller(number));
            notifyDataSetChanged();
        }

        @Override
        public void onResponse(INumber number) {
            Log.v(TAG, "onResponse: " + number);
            Caller caller = new Caller(number, !number.isOnline());
            mDatabase.updateCaller(caller);
            mCallerMap.put(number.getNumber(), caller);
            if (mSetting.isAutoReportEnabled()) {
                mDatabase.saveMarkedRecord(number, mSetting.getUid());
            }
            inCall.setFetched(true);
            notifyDataSetChanged();
        }

        @Override
        public void onResponseFailed(INumber number, boolean isOnline) {
            Log.v(TAG, "onResponse: " + number + ", " + isOnline);
            inCall.setFetched(true);
            notifyDataSetChanged();
        }
    }
}