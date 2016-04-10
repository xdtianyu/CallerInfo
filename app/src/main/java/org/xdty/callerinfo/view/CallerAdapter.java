package org.xdty.callerinfo.view;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.permission.PermissionImpl;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallerAdapter extends RecyclerView.Adapter<CallerAdapter.ViewHolder> {

    private static final Map<String, Caller> callerMap = new HashMap<>();
    private final Context mContext;
    private List<InCall> mList;
    private CardView cardView;
    private Permission mPermission;

    public CallerAdapter(Context context, List<InCall> list) {
        mContext = context;
        mPermission = new PermissionImpl(mContext.getApplicationContext());
        mList = list;
        updateCallerMap();
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
        Caller caller = callerMap.get(number);

        if (caller == null) {
            if (number.contains("+86")) {
                caller = callerMap.get(number.replace("+86", ""));
            }
        }
        return caller;
    }

    private void updateCallerMap() {
        callerMap.clear();
        boolean canReadContact = mPermission.canReadContact();
        List<Caller> callers = Caller.listAll(Caller.class);
        for (Caller caller : callers) {
            String number = caller.getNumber();
            if (number != null && !number.isEmpty()) {
                if (canReadContact) {
                    String name = Utils.getContactName(mContext, caller.getNumber());
                    caller.setContactName(name);
                }
                callerMap.put(caller.getNumber(), caller);
            }
        }
    }

    public void replaceData(List<InCall> inCalls) {
        mList = inCalls;
        notifyDataSetChanged();
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
        public void onResponseOffline (INumber number){
            new Caller(number, !number.isOnline()).save();
            callerMap.put(number.getNumber(), new Caller(number));
            notifyDataSetChanged();
        }

        @Override
        public void onResponse (INumber number){
            new Caller(number, !number.isOnline()).save();
            inCall.setFetched(true);
            updateCallerMap();
            notifyDataSetChanged();
        }

        @Override
        public void onResponseFailed (INumber number,boolean isOnline){
            inCall.setFetched(true);
            updateCallerMap();
            notifyDataSetChanged();
        }
    }
}