package org.xdty.callerinfo.fragment

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.apmem.tools.layouts.FlowLayout
import org.xdty.callerinfo.R
import org.xdty.callerinfo.application.Application.Companion.application
import org.xdty.callerinfo.contract.MainBottomContact
import org.xdty.callerinfo.contract.MainBottomContact.Presenter
import org.xdty.callerinfo.di.DaggerMainBottomComponent
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.di.modules.MainBottomModule
import org.xdty.callerinfo.model.MarkType
import org.xdty.callerinfo.model.TextColorPair
import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.InCall
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.utils.Utils
import javax.inject.Inject

class MainBottomSheetFragment : AppCompatDialogFragment(), OnClickListener, MainBottomContact.View {
    @Inject
    internal lateinit var mSetting: Setting
    @Inject
    internal lateinit var mPresenter: Presenter

    private lateinit var mFrameLayout: FrameLayout
    private lateinit var mBottomSheet: View
    private lateinit var mNumber: TextView
    private lateinit var mGeo: TextView
    private lateinit var mTime: TextView
    private lateinit var mRingTime: TextView
    private lateinit var mDuration: TextView
    private lateinit var mName: TextView
    private lateinit var mSource: TextView
    private lateinit var mFlowLayout: FlowLayout
    private lateinit var mHarassment: Button
    private lateinit var mFraud: Button
    private lateinit var mAdvertising: Button
    private lateinit var mExpress: Button
    private lateinit var mRestaurant: Button
    private lateinit var mCustom: Button
    private lateinit var mCustomText: EditText
    private lateinit var mDivider: View
    private lateinit var mEdit: TextView
    private lateinit var mFab: FloatingActionButton
    private fun bindData(inCall: InCall) {
        mPresenter.bindData(inCall)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = BottomSheetDialog(context!!)
        dialog.setContentView(R.layout.dialog_main_bottom_sheet)
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(
                    ColorDrawable(Color.TRANSPARENT))
        }
        mFrameLayout = dialog.findViewById(R.id.design_bottom_sheet)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mFrameLayout.background = ColorDrawable(Color.TRANSPARENT)
        } else {
            mFrameLayout.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        mBottomSheet = dialog.findViewById(R.id.bottom_sheet)
        mNumber = dialog.findViewById(R.id.number) as TextView
        mGeo = dialog.findViewById(R.id.geo) as TextView
        mTime = dialog.findViewById(R.id.time) as TextView
        mRingTime = dialog.findViewById(R.id.ring_time) as TextView
        mDuration = dialog.findViewById(R.id.duration) as TextView
        mName = dialog.findViewById(R.id.name) as TextView
        mSource = dialog.findViewById(R.id.source) as TextView
        mFlowLayout = dialog.findViewById(R.id.tags) as FlowLayout
        mHarassment = dialog.findViewById(R.id.harassment) as Button
        mFraud = dialog.findViewById(R.id.fraud) as Button
        mAdvertising = dialog.findViewById(R.id.advertising) as Button
        mExpress = dialog.findViewById(R.id.express) as Button
        mRestaurant = dialog.findViewById(R.id.restaurant) as Button
        mCustom = dialog.findViewById(R.id.custom) as Button
        mCustomText = dialog.findViewById(R.id.custom_text) as EditText
        mDivider = dialog.findViewById(R.id.divider)
        mEdit = dialog.findViewById(R.id.edit) as TextView
        mFab = dialog.findViewById(R.id.fab) as FloatingActionButton
        mHarassment.setOnClickListener(this)
        mFraud.setOnClickListener(this)
        mAdvertising.setOnClickListener(this)
        mExpress.setOnClickListener(this)
        mRestaurant.setOnClickListener(this)
        mCustom.setOnClickListener(this)
        mCustomText.setOnEditorActionListener(OnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mPresenter.markCustom(textView.text.toString())
                return@OnEditorActionListener true
            }
            false
        })
        mPresenter.start()
        return dialog
    }

    private fun selectTag(name: String) {
        val type = Utils.typeFromString(name)
        when (MarkType.fromInt(type)) {
            MarkType.HARASSMENT -> mHarassment.setBackgroundResource(R.color.pressed)
            MarkType.FRAUD -> mFraud.setBackgroundResource(R.color.pressed)
            MarkType.ADVERTISING -> mAdvertising.setBackgroundResource(R.color.pressed)
            MarkType.EXPRESS_DELIVERY -> mExpress.setBackgroundResource(R.color.pressed)
            MarkType.RESTAURANT_DELIVER -> mRestaurant.setBackgroundResource(R.color.pressed)
            else -> {
                mCustom.setBackgroundResource(R.color.pressed)
                mCustomText.visibility = View.VISIBLE
                mCustomText.setText(name)
            }
        }
    }

    override fun onClick(view: View) {
        val normal = TypedValue()
        context!!.theme.resolveAttribute(R.attr.selectableItemBackground, normal, true)
        mHarassment.setBackgroundResource(normal.resourceId)
        mFraud.setBackgroundResource(normal.resourceId)
        mAdvertising.setBackgroundResource(normal.resourceId)
        mExpress.setBackgroundResource(normal.resourceId)
        mRestaurant.setBackgroundResource(normal.resourceId)
        mCustom.setBackgroundResource(normal.resourceId)
        view.setBackgroundResource(R.color.pressed)
        mPresenter.markClicked(view.id)
    }

    override fun init(inCall: InCall, caller: Caller) {
        mNumber.text = inCall.number
        var geo = caller.geo.trim { it <= ' ' }
        if (geo.isEmpty()) {
            geo = resources.getString(R.string.no_geo)
        }
        mGeo.text = geo
        mTime.text = inCall.readableTime
        mRingTime.text = Utils.readableTime(inCall.ringTime)
        mDuration.text = Utils.readableTime(inCall.duration)
        val name = caller.name
        updateMarkName(name)
        mSource.text = caller.source
        // set bottom sheet background
        updateBackgroundColor(caller.name)
        if (mPresenter.canMark()) {
            mDivider.visibility = View.VISIBLE
            mEdit.visibility = View.VISIBLE
            mFlowLayout.visibility = View.VISIBLE
            mFab.visibility = View.VISIBLE
            mFab.setOnClickListener(object : OnClickListener {
                override fun onClick(view: View) {
                    BottomSheetBehavior.from<FrameLayout?>(mFrameLayout).state = BottomSheetBehavior.STATE_EXPANDED
                }
            })
            selectTag(name)
        } else {
            BottomSheetBehavior.from<FrameLayout?>(mFrameLayout).setState(BottomSheetBehavior.STATE_EXPANDED)
        }
    }

    override fun updateMark(viewId: Int, caller: Caller) {
        if (viewId == mCustom.id) {
            mCustomText.visibility = View.VISIBLE
            mCustomText.setText(if (caller.name != null) caller.name else "")
        } else {
            mCustomText.visibility = View.GONE
        }
    }

    override fun updateMarkName(name: String?) {
        var name = name
        if (name == null || name.isEmpty()) {
            name = resources.getString(R.string.no_marked_name)
        }
        mName.text = name
        updateBackgroundColor(name)
    }

    override fun setPresenter(presenter: Presenter) {}

    private fun updateBackgroundColor(name: String) {
        val colorPair = TextColorPair.from(name)
        mBottomSheet.setBackgroundColor(colorPair.color)
    }

    inner class BottomSheetDialog(context: Context) : com.google.android.material.bottomsheet.BottomSheetDialog(context) {
        override fun onCreate(savedInstanceState: Bundle) {
            super.onCreate(savedInstanceState)
            // fix dark status bar https://code.google.com/p/android/issues/detail?id=202691#c10
            val screenHeight = mSetting.screenHeight
            val statusBarHeight = mSetting.statusBarHeight
            val dialogHeight = screenHeight - statusBarHeight
            if (window != null) {
                window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                        if (dialogHeight == 0) ViewGroup.LayoutParams.MATCH_PARENT else dialogHeight)
            }
        }
    }

    companion object {
        fun newInstance(inCall: InCall): MainBottomSheetFragment {
            val fragment = MainBottomSheetFragment()
            fragment.bindData(inCall)
            return fragment
        }
    }

    init {
        DaggerMainBottomComponent.builder()
                .appModule(AppModule(application))
                .mainBottomModule(MainBottomModule(this))
                .build()
                .inject(this)
    }
}