package org.xdty.callerinfo;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.NumberInfo;

import wei.mark.standout.StandOutWindow;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = MainActivity.class.getSimpleName();
    public final static int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    public final static int REQUEST_CODE_ASK_PERMISSIONS = 1002;

    Toolbar toolbar;

    private int mScreenWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = mWindowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        mScreenWidth = point.x;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            }

            int res = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
            if (res != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                        REQUEST_CODE_OVERLAY_PERMISSION);
            }

        }
    }

    @Override
    protected void onStop() {
        StandOutWindow.closeAll(this, FloatWindow.class);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted...");
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "READ_PHONE_STATE Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(
                menu.findItem(R.id.action_search));
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(mScreenWidth);
        searchView.setInputType(InputType.TYPE_CLASS_NUMBER);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: " + query);
                showFloatWindow(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                StandOutWindow.closeAll(MainActivity.this, FloatWindow.class);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                break;
            case R.id.action_float_window:
                showFloatWindow("10086");
                Snackbar.make(toolbar, R.string.float_window_hint, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                break;
        }

        return true;
    }

    private void showFloatWindow(String phoneNumber) {
        StandOutWindow.closeAll(this, FloatWindow.class);
        new PhoneNumber(this, new PhoneNumber.Callback() {
            @Override
            public void onResponse(NumberInfo numberInfo) {
                String text = numberInfo.toString();
                Bundle bundle = new Bundle();
                bundle.putString(FloatWindow.NUMBER_INFO, text);
                bundle.putInt(FloatWindow.WINDOW_COLOR, R.color.orange_dark);
                StandOutWindow.show(MainActivity.this, FloatWindow.class, FloatWindow.VIEWER_FRONT);
                StandOutWindow.sendData(MainActivity.this, FloatWindow.class,
                        FloatWindow.VIEWER_FRONT, 0, bundle, FloatWindow.class, 0);
            }

            @Override
            public void onResponseFailed(NumberInfo numberInfo) {

            }
        }).fetch(phoneNumber);
    }
}
