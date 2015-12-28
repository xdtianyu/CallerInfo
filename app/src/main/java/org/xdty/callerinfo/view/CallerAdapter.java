package org.xdty.callerinfo.view;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.model.db.Caller;

import java.util.List;

public class CallerAdapter extends RecyclerView.Adapter<CallerAdapter.ViewHolder> {

    private Context mContext;
    private List<Caller> mList;

    private CardView cardView;

    public CallerAdapter(Context context, List<Caller> list) {
        mContext = context;
        mList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public ViewHolder(View view) {
            super(view);
            textView = (TextView) view.findViewById(R.id.text);
        }

        public void bind(Caller caller) {
            textView.setText(caller.toString());
        }
    }
}