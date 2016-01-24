package org.xdty.callerinfo.plugin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    public final static int REQUEST_CODE_CALL_PERMISSION = 2001;
    public final static int REQUEST_CODE_CALL_LOG_PERMISSION = 2002;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int type = getIntent().getIntExtra("type", 0);
            switch (type) {
                case REQUEST_CODE_CALL_PERMISSION:
                    requestPermissions(
                            new String[]{Manifest.permission.CALL_PHONE},
                            REQUEST_CODE_CALL_PERMISSION);
                    break;
                case REQUEST_CODE_CALL_LOG_PERMISSION:
                    requestPermissions(
                            new String[]{Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.WRITE_CALL_LOG},
                            REQUEST_CODE_CALL_LOG_PERMISSION);
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_CALL_PERMISSION:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: call " + false);
                }
                break;
            case REQUEST_CODE_CALL_LOG_PERMISSION:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: call log " + false);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        Intent intent = new Intent(this, PluginService.class);
        intent.putExtra("type", requestCode);
        intent.putExtra("result", grantResults[0] == PackageManager.PERMISSION_GRANTED);
        startService(intent);

        finish();
    }
}
