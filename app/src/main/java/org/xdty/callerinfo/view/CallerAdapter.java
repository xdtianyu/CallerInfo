package org.xdty.callerinfo.view;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.NumberInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallerAdapter extends RecyclerView.Adapter<CallerAdapter.ViewHolder> {

    private static Map<String, Caller> callerMap = new HashMap<>();
    private Context mContext;
    private List<InCall> mList;
    private CardView cardView;

    public CallerAdapter(Context context, List<InCall> list) {
        mContext = context;
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
        List<Caller> callers = Caller.listAll(Caller.class);
        for (Caller caller : callers) {
            String number = caller.getNumber();
            if (number != null && !number.isEmpty()) {
                callerMap.put(caller.getNumber(), caller);
            }
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        Context context;
        CardView cardView;
        TextView text;
        TextView number;

        public ViewHolder(Context context, View view) {
            super(view);
            this.context = context;
            cardView = (CardView) view.findViewById(R.id.card_view);
            text = (TextView) view.findViewById(R.id.text);
            number = (TextView) view.findViewById(R.id.number);
        }

        public void setAlpha(float alpha) {
            cardView.setAlpha(alpha);
        }

        public void bind(final InCall inCall, Caller caller) {
            if (caller != null) {
                TextColorPair t = Utils.getTextColorPair(context, caller.toNumber());
                text.setText(t.text);
                cardView.setCardBackgroundColor(t.color);
                number.setText(caller.getNumber());
            } else {
                if (inCall.isFetched()) {
                    text.setText(inCall.getNumber());
                    number.setText(R.string.loading_error);
                    cardView.setCardBackgroundColor(
                            ContextCompat.getColor(context, R.color.graphite));
                } else {
                    new PhoneNumber(context, new PhoneNumber.Callback() {
                        @Override
                        public void onResponse(NumberInfo numberInfo) {

                            for (org.xdty.phone.number.model.Number number : numberInfo.getNumbers()) {
                                new Caller(number).save();
                                inCall.setFetched(true);
                                updateCallerMap();
                                notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onResponseFailed(NumberInfo numberInfo) {

                        }
                    }).fetch(inCall.getNumber());

                    text.setText(inCall.getNumber());
                    number.setText(R.string.loading);
                    cardView.setCardBackgroundColor(
                            ContextCompat.getColor(context, R.color.blue_light));
                }
            }
            cardView.setAlpha(1f);
        }
    }
}