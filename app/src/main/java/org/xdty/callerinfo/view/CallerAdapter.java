package org.xdty.callerinfo.view;

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
import org.xdty.callerinfo.contract.MainContract;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class CallerAdapter extends RecyclerView.Adapter<CallerAdapter.ViewHolder> {

    private static final String TAG = CallerAdapter.class.getSimpleName();

    MainContract.Presenter mPresenter;

    private List<InCall> mList;

    public CallerAdapter(MainContract.Presenter presenter) {
        mPresenter = presenter;
        mList = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        InCall inCall = mList.get(position);
        holder.bind(inCall);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    public void replaceData(List<InCall> inCalls) {
        mList = inCalls;
        notifyDataSetChanged();
    }

    public InCall getItem(int position) {
        return mList.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {

        final CardView cardView;
        final TextView text;
        final TextView number;
        final LinearLayout detail;
        final TextView time;
        final TextView ringTime;
        final TextView duration;
        InCall inCall;

        public ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.card_view);
            text = (TextView) view.findViewById(R.id.text);
            number = (TextView) view.findViewById(R.id.number);
            detail = (LinearLayout) view.findViewById(R.id.detail);
            time = (TextView) view.findViewById(R.id.time);
            ringTime = (TextView) view.findViewById(R.id.ring_time);
            duration = (TextView) view.findViewById(R.id.duration);

            cardView.setOnClickListener(this);
            cardView.setOnLongClickListener(this);
        }

        public void setAlpha(float alpha) {
            cardView.setAlpha(alpha);
        }

        public void bind(InCall inCall) {

            Caller caller = mPresenter.getCaller(inCall.getNumber());

            if (caller.isEmpty()) {
                if (caller.isOffline()) {
                    if (caller.hasGeo()) {
                        text.setText(caller.getGeo());
                    } else {
                        text.setText(R.string.loading);
                    }
                    number.setText(inCall.getNumber());
                    cardView.setCardBackgroundColor(
                            ContextCompat.getColor(cardView.getContext(), R.color.blue_light));
                } else {
                    text.setText(R.string.loading_error);
                    number.setText(inCall.getNumber());
                    cardView.setCardBackgroundColor(
                            ContextCompat.getColor(cardView.getContext(), R.color.graphite));
                }
            } else {
                TextColorPair t = TextColorPair.from(caller);
                text.setText(t.text);
                //noinspection ResourceAsColor
                cardView.setCardBackgroundColor(t.color);
                number.setText(TextUtils.isEmpty(
                        caller.getContactName()) ? caller.getNumber() : caller.getContactName());
            }
            cardView.setAlpha(1f);
            if (inCall.isExpanded()) {
                detail.setVisibility(View.VISIBLE);
            } else {
                detail.setVisibility(View.GONE);
            }
            this.inCall = inCall;

            time.setText(Utils.readableDate(inCall.getTime()));
            ringTime.setText(Utils.readableTime(inCall.getRingTime()));
            duration.setText(Utils.readableTime(inCall.getDuration()));
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
        public boolean onLongClick(View view) {
            mPresenter.itemOnLongClicked(inCall);
            return true;
        }
    }
}