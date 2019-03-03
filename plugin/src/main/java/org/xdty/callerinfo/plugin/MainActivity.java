package org.xdty.callerinfo.plugin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public final static int REQUEST_CODE_CALL_PERMISSION = 2001;
    public final static int REQUEST_CODE_CALL_LOG_PERMISSION = 2002;
    public final static int REQUEST_CODE_STORAGE_PERMISSION = 2003;

    private final static String MAIN_PACKAGE_NAME = "org.xdty.callerinfo";

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int type = getIntent().getIntExtra("type", 0);
            switch (type) {
                case REQUEST_CODE_CALL_PERMISSION:
                    String[] permissions = new String[] { Manifest.permission.CALL_PHONE };
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        permissions = new String[] { Manifest.permission.CALL_PHONE,
                                Manifest.permission.ANSWER_PHONE_CALLS };
                    }
                    requestPermissions(permissions, REQUEST_CODE_CALL_PERMISSION);
                    break;
                case REQUEST_CODE_CALL_LOG_PERMISSION:
                    requestPermissions(
                            new String[] { Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.WRITE_CALL_LOG },
                            REQUEST_CODE_CALL_LOG_PERMISSION);
                    break;
                case REQUEST_CODE_STORAGE_PERMISSION:
                    requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE },
                            REQUEST_CODE_STORAGE_PERMISSION);
                    break;
                default:
                    break;
            }
        }

        TextView version = findViewById(R.id.version);
        TextView versionCode = findViewById(R.id.version_code);

        version.setText(getString(R.string.version, BuildConfig.VERSION_NAME));
        versionCode.setText(getString(R.string.version_code, BuildConfig.VERSION_CODE));
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView installText = findViewById(R.id.install_main_app);
        Button installButton = findViewById(R.id.install);

        if (Utils.isAppInstalled(this, MAIN_PACKAGE_NAME)) {
            installText.setVisibility(View.GONE);
            installButton.setVisibility(View.GONE);
        } else {
            installText.setVisibility(View.VISIBLE);
            installButton.setVisibility(View.VISIBLE);
            installButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=" + MAIN_PACKAGE_NAME));
                    startActivity(intent);
                }
            });
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
            case REQUEST_CODE_STORAGE_PERMISSION:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: storage " + false);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.plugin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.privacy:
                startActivity(new Intent(LicensesActivity.ACTION_PRIVACY));
                break;
            case R.id.license:
                startActivity(new Intent(LicensesActivity.ACTION_LICENSE));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
