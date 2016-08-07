package org.xdty.callerinfo.fragment;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.apmem.tools.layouts.FlowLayout;
import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.data.CallerDataSource;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.Utils;

import javax.inject.Inject;

public class MainBottomSheetFragment extends AppCompatDialogFragment
        implements View.OnClickListener {

    @Inject
    Setting mSetting;
    @Inject
    CallerDataSource mCallerDataSource;

    private InCall mInCall;
    private Caller mCaller;

    private View mBottomSheet;
    private TextView mNumber;
    private TextView mGeo;
    private TextView mTime;
    private TextView mRingTime;
    private TextView mDuration;
    private TextView mName;
    private TextView mSource;

    private FlowLayout mFlowLayout;
    private Button mHarassment;
    private Button mFraud;
    private Button mAdvertising;
    private Button mExpress;
    private Button mRestaurant;
    private Button mCustom;
    private EditText mCustomText;

    public MainBottomSheetFragment() {
        Application.getAppComponent().inject(this);
    }

    public static MainBottomSheetFragment newInstance(InCall inCall) {
        MainBottomSheetFragment fragment = new MainBottomSheetFragment();
        fragment.bindData(inCall);
        return fragment;
    }

    private void bindData(InCall inCall) {
        mInCall = inCall;
        mCaller = mCallerDataSource.getCallerFromCache(inCall.getNumber());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = new BottomSheetDialog(getContext());
        dialog.setContentView(R.layout.dialog_main_bottom_sheet);
        dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));

        FrameLayout frameLayout = (FrameLayout) dialog.findViewById(R.id.design_bottom_sheet);

        frameLayout.setBackground(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        mBottomSheet = dialog.findViewById(R.id.bottom_sheet);
        mNumber = (TextView) dialog.findViewById(R.id.number);
        mGeo = (TextView) dialog.findViewById(R.id.geo);
        mTime = (TextView) dialog.findViewById(R.id.time);
        mRingTime = (TextView) dialog.findViewById(R.id.ring_time);
        mDuration = (TextView) dialog.findViewById(R.id.duration);
        mName = (TextView) dialog.findViewById(R.id.name);
        mSource = (TextView) dialog.findViewById(R.id.source);

        mFlowLayout = (FlowLayout) dialog.findViewById(R.id.tags);
        mHarassment = (Button) dialog.findViewById(R.id.harassment);
        mFraud = (Button) dialog.findViewById(R.id.fraud);
        mAdvertising = (Button) dialog.findViewById(R.id.advertising);
        mExpress = (Button) dialog.findViewById(R.id.express);
        mRestaurant = (Button) dialog.findViewById(R.id.restaurant);
        mCustom = (Button) dialog.findViewById(R.id.custom);
        mCustomText = (EditText) dialog.findViewById(R.id.custom_text);

        mHarassment.setOnClickListener(this);
        mFraud.setOnClickListener(this);
        mAdvertising.setOnClickListener(this);
        mExpress.setOnClickListener(this);
        mRestaurant.setOnClickListener(this);
        mCustom.setOnClickListener(this);

        mNumber.setText(mInCall.getNumber());
        mGeo.setText(mCaller.getGeo());
        mTime.setText(mInCall.getReadableTime());
        mRingTime.setText(Utils.readableTime(mInCall.getRingTime()));
        mDuration.setText(Utils.readableTime(mInCall.getDuration()));
        mName.setText(mCaller.getName());
        mSource.setText(mCaller.getSource());

        // set bottom sheet background
        TextColorPair colorPair = TextColorPair.from(mCaller);
        //noinspection ResourceAsColor
        mBottomSheet.setBackgroundColor(colorPair.color);

        selectTag(mCaller.getName());

        return dialog;
    }

    private void selectTag(String name) {

        int type = Utils.typeFromString(name);
        switch (MarkedRecord.MarkType.fromInt(type)) {
            case HARASSMENT:
                mHarassment.setBackgroundResource(R.color.pressed);
                break;
            case FRAUD:
                mFraud.setBackgroundResource(R.color.pressed);
                break;
            case ADVERTISING:
                mAdvertising.setBackgroundResource(R.color.pressed);
                break;
            case EXPRESS_DELIVERY:
                mExpress.setBackgroundResource(R.color.pressed);
                break;
            case RESTAURANT_DELIVER:
                mRestaurant.setBackgroundResource(R.color.pressed);
                break;
            default:
                mCustom.setBackgroundResource(R.color.pressed);
                mCustomText.setVisibility(View.VISIBLE);
                mCustomText.setText(mCaller.getName() != null ? mCaller.getName() : "");
                break;
        }
    }

    @Override
    public void onClick(View view) {

        TypedValue normal = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.selectableItemBackground, normal, true);

        if (view == mCustom) {
            mCustomText.setVisibility(View.VISIBLE);
            mCustomText.setText(mCaller.getName() != null ? mCaller.getName() : "");
        } else {
            mCustomText.setVisibility(View.GONE);
        }

        mHarassment.setBackgroundResource(normal.resourceId);
        mFraud.setBackgroundResource(normal.resourceId);
        mAdvertising.setBackgroundResource(normal.resourceId);
        mExpress.setBackgroundResource(normal.resourceId);
        mRestaurant.setBackgroundResource(normal.resourceId);
        mCustom.setBackgroundResource(normal.resourceId);

        view.setBackgroundResource(R.color.pressed);
    }

    public class BottomSheetDialog extends android.support.design.widget.BottomSheetDialog {

        public BottomSheetDialog(@NonNull Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // fix dark status bar https://code.google.com/p/android/issues/detail?id=202691#c10
            int screenHeight = mSetting.getScreenHeight();
            int statusBarHeight = mSetting.getStatusBarHeight();
            int dialogHeight = screenHeight - statusBarHeight;
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    dialogHeight == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : dialogHeight);
        }
    }
}
