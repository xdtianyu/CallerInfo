package org.xdty.callerinfo.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.ViewGroup;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.setting.Setting;

import javax.inject.Inject;

public class MainBottomSheetFragment extends AppCompatDialogFragment {

    @Inject
    Setting mSetting;

    public MainBottomSheetFragment() {
        Application.getAppComponent().inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = new BottomSheetDialog(getContext());
        dialog.setContentView(R.layout.dialog_main_bottom_sheet);

        return dialog;
    }

    public class BottomSheetDialog extends android.support.design.widget.BottomSheetDialog {

        public BottomSheetDialog(@NonNull Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            int screenHeight = mSetting.getScreenHeight();
            int statusBarHeight = mSetting.getStatusBarHeight();
            int dialogHeight = screenHeight - statusBarHeight;
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    dialogHeight == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : dialogHeight);
        }
    }
}
