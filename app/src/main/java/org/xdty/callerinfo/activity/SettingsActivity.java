package org.xdty.callerinfo.activity;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.fragment.SettingsFragment;
import org.xdty.callerinfo.utils.Utils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.action_settings);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, SettingsFragment.newInstance(getIntent()))
                    .commit();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = Utils.changeLang(newBase);
        super.attachBaseContext(context);
    }

}