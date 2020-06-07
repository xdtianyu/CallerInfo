package org.xdty.callerinfo.view

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.xdty.callerinfo.R
import org.xdty.callerinfo.contract.MainContract.Presenter
import org.xdty.callerinfo.model.TextColorPair
import org.xdty.callerinfo.model.db.InCall
import org.xdty.callerinfo.utils.Utils
import org.xdty.callerinfo.view.CallerAdapter.ViewHolder

class CallerAdapter(internal var mPresenter: Presenter) : Adapter<ViewHolder>() {
    private var mList: List<InCall> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val inCall = mList[position]
        holder.bind(inCall)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
    }

    fun replaceData(inCalls: List<InCall>) {
        mList = inCalls
        notifyDataSetChanged()
    }

    fun getItem(position: Int): InCall {
        return mList[position]
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener, OnLongClickListener {

        private val cardView: CardView = view.findViewById(R.id.card_view)
        private val text: TextView = view.findViewById(R.id.text)
        private val number: TextView = view.findViewById(R.id.number)
        private val detail: LinearLayout = view.findViewById(R.id.detail)
        private val time: TextView = view.findViewById(R.id.time)
        private val ringTime: TextView = view.findViewById(R.id.ring_time)
        private val duration: TextView = view.findViewById(R.id.duration)

        internal var inCall: InCall? = null
        fun setAlpha(alpha: Float) {
            cardView.alpha = alpha
        }

        fun bind(inCall: InCall) {
            val caller = mPresenter.getCaller(inCall.number)
            if (caller.isEmpty) {
                if (caller.isOffline) {
                    if (caller.hasGeo()) {
                        text.text = caller.geo
                    } else {
                        text.setText(R.string.loading)
                    }
                    number.text = inCall.number
                    cardView.setCardBackgroundColor(
                            ContextCompat.getColor(cardView.context, R.color.blue_light))
                } else {
                    text.setText(R.string.loading_error)
                    number.text = inCall.number
                    cardView.setCardBackgroundColor(
                            ContextCompat.getColor(cardView.context, R.color.graphite))
                }
            } else {
                val t = TextColorPair.from(caller)
                text.text = t.text
                cardView.setCardBackgroundColor(t.color)
                number.text = if (TextUtils.isEmpty(
                                caller.contactName)) caller.number else caller.contactName
            }
            cardView.alpha = 1f
            if (inCall.isExpanded) {
                detail.visibility = View.VISIBLE
            } else {
                detail.visibility = View.GONE
            }
            this.inCall = inCall
            time.text = Utils.readableDate(inCall.time)
            ringTime.text = Utils.readableTime(inCall.ringTime)
            duration.text = Utils.readableTime(inCall.duration)
        }

        override fun onClick(v: View) {
            if (detail.visibility == View.VISIBLE) {
                detail.visibility = View.GONE
                inCall!!.isExpanded = false
            } else {
                detail.visibility = View.VISIBLE
                inCall!!.isExpanded = true
            }
        }

        override fun onLongClick(view: View): Boolean {
            mPresenter.itemOnLongClicked(inCall!!)
            return true
        }

        init {
            cardView.setOnClickListener(this)
            cardView.setOnLongClickListener(this)
        }
    }

    companion object {
        private val TAG = CallerAdapter::class.java.simpleName
    }

}