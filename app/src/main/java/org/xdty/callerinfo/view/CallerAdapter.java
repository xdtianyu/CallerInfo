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

    private Context mContext;
    private List<InCall> mList;
    private static Map<String, Caller> callerMap = new HashMap<>();

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
        Caller caller = callerMap.get(inCall.getNumber());
        holder.bind(inCall, caller);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private static void updateCallerMap() {
        callerMap.clear();
        List<Caller> callers = Caller.listAll(Caller.class);
        for (Caller caller : callers) {
            String number = caller.getNumber();
            if (number != null && !number.isEmpty()) {
                callerMap.put(caller.getNumber(), caller);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

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

        public void bind(InCall inCall, Caller caller) {
            if (caller != null) {
                TextColorPair t = Utils.getTextColorPair(context, caller.toNumber());
                text.setText(t.text);
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, t.color));
                number.setText(caller.getNumber());
            } else {
                new PhoneNumber(context, new PhoneNumber.Callback() {
                    @Override
                    public void onResponse(NumberInfo numberInfo) {

                        for (org.xdty.phone.number.model.Number number : numberInfo.getNumbers()) {
                            new Caller(number).save();
                            updateCallerMap();
                        }
                    }

                    @Override
                    public void onResponseFailed(NumberInfo numberInfo) {

                    }
                }).fetch(inCall.getNumber());
            }
        }
    }
}