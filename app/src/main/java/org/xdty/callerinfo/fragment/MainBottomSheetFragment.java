package org.xdty.callerinfo.fragment;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.data.CallerDataSource;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.Utils;

import javax.inject.Inject;

public class MainBottomSheetFragment extends AppCompatDialogFragment {

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

        mNumber.setText(mInCall.getNumber());
        mGeo.setText(mCaller.getGeo());
        mTime.setText(mInCall.getReadableTime());
        mRingTime.setText(Utils.readableTime(mInCall.getRingTime()));
        mDuration.setText(Utils.readableTime(mInCall.getDuration()));
        mName.setText(mCaller.getName());

        // set bottom sheet background
        TextColorPair colorPair = TextColorPair.from(mCaller);
        //noinspection ResourceAsColor
        mBottomSheet.setBackgroundColor(colorPair.color);

        return dialog;
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
