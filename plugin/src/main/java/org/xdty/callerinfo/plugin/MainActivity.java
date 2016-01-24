package org.xdty.callerinfo.plugin;

import android.Manifest;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    public final static int REQUEST_CODE_CALL_PERMISSION = 2001;
    public final static int REQUEST_CODE_CALL_LOG_PERMISSION = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{Manifest.permission.CALL_PHONE},
                    REQUEST_CODE_CALL_PERMISSION);
        }
    }
}
