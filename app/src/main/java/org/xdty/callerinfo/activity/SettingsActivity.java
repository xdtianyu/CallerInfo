package org.xdty.callerinfo.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.fragment.SettingsFragment;
import org.xdty.callerinfo.utils.Utils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.action_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }
        return true;
    }

}