package org.xdty.callerinfo.plugin;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.webkit.WebView;

public class LicensesActivity extends AppCompatActivity {

    public final static String ACTION_LICENSE = "org.xdty.callerinfo.plugin.action.VIEW_LICENSES";
    public final static String ACTION_PRIVACY = "org.xdty.callerinfo.plugin.action.VIEW_PRIVACY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_licenses);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        WebView webView = (WebView) findViewById(R.id.webView);

        String action = getIntent().getAction();
        String url = "file:///android_res/raw/licenses.html";

        switch (action) {
            case ACTION_LICENSE:
                setTitle(R.string.license);
                break;
            case ACTION_PRIVACY:
                setTitle(R.string.plugin_privacy);
                url = "file:///android_res/raw/privacy_notice.html";
                break;
            default:
                break;
        }
        if (webView != null) {
            webView.loadUrl(url);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
