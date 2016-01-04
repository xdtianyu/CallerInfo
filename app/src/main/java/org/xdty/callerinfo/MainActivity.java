package org.xdty.callerinfo;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.callerinfo.view.CallerAdapter;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.Number;
import org.xdty.phone.number.model.NumberInfo;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = MainActivity.class.getSimpleName();
    public final static int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    public final static int REQUEST_CODE_ASK_PERMISSIONS = 1002;

    Toolbar toolbar;
    List<InCall> inCallList = new ArrayList<>();
    SharedPreferences sharedPreferences;
    private int mScreenWidth;
    private TextView mEmptyText;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private CallerAdapter mCallerAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FrameLayout mMainLayout;
    private Menu mMenu;
    private boolean isFloating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkEula();

        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = mWindowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        mScreenWidth = point.x;

        mMainLayout = (FrameLayout) findViewById(R.id.main_layout);
        mEmptyText = (TextView) findViewById(R.id.empty_text);
        mRecyclerView = (RecyclerView) findViewById(R.id.history_list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        loadInCallList();

        mCallerAdapter = new CallerAdapter(this, inCallList);
        mRecyclerView.setAdapter(mCallerAdapter);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // can scroll up and disable refresh
                if (recyclerView.canScrollVertically(-1)) {
                    mSwipeRefreshLayout.setEnabled(false);
                } else {
                    mSwipeRefreshLayout.setEnabled(true);
                }
            }
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final InCall inCall = inCallList.get(viewHolder.getAdapterPosition());
                inCallList.remove(viewHolder.getAdapterPosition());
                mCallerAdapter.notifyDataSetChanged();
                final Snackbar snackbar = Snackbar.make(toolbar, R.string.deleted,
                        Snackbar.LENGTH_LONG);

                snackbar.setAction(getString(R.string.undo), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snackbar.dismiss();
                        loadInCallList();
                        mCallerAdapter.notifyDataSetChanged();
                    }
                });
                snackbar.setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        switch (event) {
                            case DISMISS_EVENT_MANUAL:
                            case DISMISS_EVENT_ACTION:
                                break;
                            default:
                                inCall.delete();
                                break;
                        }
                        super.onDismissed(snackbar, event);
                    }
                });
                snackbar.show();
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                boolean swiping = actionState == ItemTouchHelper.ACTION_STATE_SWIPE;
                mSwipeRefreshLayout.setEnabled(!swiping);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder,
                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState,
                        isCurrentlyActive);
                CallerAdapter.ViewHolder vh = (CallerAdapter.ViewHolder) viewHolder;
                vh.setAlpha(1 - Math.abs(dX) / mScreenWidth * 2);
            }
        });

        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadInCallList();
                mCallerAdapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInCallList();
        mCallerAdapter.notifyDataSetChanged();
    }

    private void loadInCallList() {
        inCallList.clear();
        inCallList.addAll(InCall.listAll(InCall.class, "time DESC"));

        if (inCallList.size() == 0) {
            mEmptyText.setVisibility(View.VISIBLE);
        } else {
            mEmptyText.setVisibility(View.GONE);
        }
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
        Utils.closeWindow(this);
        isFloating = false;
        updateMenuTitles();
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
    public void onBackPressed() {
        if (isFloating) {
            Utils.closeWindow(this);
            isFloating = false;
            updateMenuTitles();
        } else {
            super.onBackPressed();
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
        mMenu = menu;

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
                showNumberInfo(query);
                mRecyclerView.setVisibility(View.INVISIBLE);
                mMainLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this,
                        R.color.dark));
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Utils.closeWindow(MainActivity.this);
                mRecyclerView.setVisibility(View.VISIBLE);
                mMainLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this,
                        R.color.transparent));
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
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_float_window:
                if (!isFloating) {
                    Utils.showTextWindow(this, R.string.float_window_hint);
                    isFloating = true;
                } else {
                    Utils.closeWindow(this);
                    isFloating = false;
                }
                updateMenuTitles();

                break;
        }

        return true;
    }

    private void updateMenuTitles() {
        if (mMenu != null) {
            MenuItem floatWindowMenu = mMenu.findItem(R.id.action_float_window);
            if (isFloating) {
                floatWindowMenu.setTitle(R.string.close_window);
            } else {
                floatWindowMenu.setTitle(R.string.action_float_window);
            }
        }
    }

    private void showNumberInfo(String phoneNumber) {
        Utils.closeWindow(this);

        if (phoneNumber.isEmpty()) {
            return;
        }

        List<Caller> callers = Caller.find(Caller.class, "number=?", phoneNumber);

        if (callers.size() > 0) {
            Caller caller = callers.get(0);
            if (caller.getLastUpdate() - System.currentTimeMillis() < 7 * 24 * 3600 * 1000) {
                Utils.showMovableWindow(MainActivity.this, caller.toNumber());
                return;
            } else {
                caller.delete();
            }
        }

        new PhoneNumber(this, new PhoneNumber.Callback() {
            @Override
            public void onResponse(NumberInfo numberInfo) {

                for (Number number : numberInfo.getNumbers()) {
                    new Caller(number).save();
                    Utils.showMovableWindow(MainActivity.this, number);
                }
            }

            @Override
            public void onResponseFailed(NumberInfo numberInfo) {

            }
        }).fetch(phoneNumber);
    }

    private void checkEula() {

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean eula = sharedPreferences.getBoolean("eula", false);

        if (!eula) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.eula_title));
            builder.setMessage(getString(R.string.eula_message));
            builder.setCancelable(false);
            builder.setPositiveButton(getString(R.string.agree),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("eula", true);
                            editor.putInt("eula_version", 1);
                            editor.putInt("version", BuildConfig.VERSION_CODE);
                            editor.apply();
                        }
                    });
            builder.setNegativeButton(getString(R.string.disagree),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            builder.show();
        }
    }
}
