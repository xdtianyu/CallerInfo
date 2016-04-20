package org.xdty.callerinfo.activity;

import android.Manifest;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.contract.MainContract;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.permission.PermissionImpl;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.presenter.MainPresenter;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.service.MarkWindow;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.callerinfo.view.CallerAdapter;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements MainContract.View {

    public final static int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    public final static int REQUEST_CODE_ASK_PERMISSIONS = 1002;
    private final static String TAG = MainActivity.class.getSimpleName();
    private Toolbar mToolbar;
    private int mScreenWidth;
    private TextView mEmptyText;
    private RecyclerView mRecyclerView;
    private CallerAdapter mCallerAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FrameLayout mMainLayout;
    private long mLastSearchTime;
    private MainContract.Presenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Setting setting = new SettingImpl(getApplicationContext());
        Permission permission = new PermissionImpl(this);
        PhoneNumber phoneNumber = new PhoneNumber(this);

        mPresenter = new MainPresenter(this, setting, permission, phoneNumber);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mPresenter.checkEula();
        mScreenWidth = setting.getScreenWidth();

        mMainLayout = (FrameLayout) findViewById(R.id.main_layout);
        mEmptyText = (TextView) findViewById(R.id.empty_text);
        mRecyclerView = (RecyclerView) findViewById(R.id.history_list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        mCallerAdapter = new CallerAdapter(this, new ArrayList<InCall>());
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
                final int position = viewHolder.getAdapterPosition();
                mPresenter.removeInCallFromList(position);
                mCallerAdapter.notifyDataSetChanged();
                final Snackbar snackbar = Snackbar.make(mToolbar, R.string.deleted,
                        Snackbar.LENGTH_LONG);

                snackbar.setAction(getString(R.string.undo), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snackbar.dismiss();
                        mPresenter.loadInCallList();
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
                                mPresenter.removeInCall(position);
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
                vh.setAlpha(1 - Math.abs(dX) / mScreenWidth * 1.2f);
            }
        });

        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.loadInCallList();
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected int getTitleId() {
        return R.string.app_name;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPresenter.start();
        MarkWindow.show(this, MarkWindow.class, 111);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mPresenter.canDrawOverlays()) {
            mPresenter.requestDrawOverlays(REQUEST_CODE_OVERLAY_PERMISSION);
        }

        int res = mPresenter.checkPermission(Manifest.permission.READ_PHONE_STATE);
        if (res != PackageManager.PERMISSION_GRANTED) {
            mPresenter.requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    protected void onStop() {
        if (FloatWindow.status() != FloatWindow.STATUS_CLOSE) {
            Utils.closeWindow(this);
        }
        mPresenter.clearSearch();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (!mPresenter.canDrawOverlays()) {
                Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted...");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (FloatWindow.status() != FloatWindow.STATUS_CLOSE) {
            Utils.closeWindow(this);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem floatWindowMenu = menu.findItem(R.id.action_float_window);
        if (FloatWindow.status() != FloatWindow.STATUS_CLOSE) {
            floatWindowMenu.setTitle(R.string.close_window);
        } else {
            floatWindowMenu.setTitle(R.string.action_float_window);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(
                menu.findItem(R.id.action_search));
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(mScreenWidth);
        searchView.setInputType(InputType.TYPE_CLASS_PHONE);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: " + query);
                if (System.currentTimeMillis() - mLastSearchTime > 1000) {
                    mPresenter.search(query);
                    mRecyclerView.setVisibility(View.INVISIBLE);
                    mMainLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this,
                            R.color.dark));
                    mLastSearchTime = System.currentTimeMillis();
                }
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
                if (FloatWindow.status() == FloatWindow.STATUS_CLOSE) {
                    Utils.showTextWindow(this, R.string.float_window_hint,
                            FloatWindow.SET_POSITION_FRONT);
                } else {
                    Utils.closeWindow(this);
                }
                break;
            case R.id.action_clear_history:
                clearHistory();
                break;
        }

        return true;
    }

    private void clearHistory() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.action_clear_history));
        builder.setMessage(getString(R.string.clear_history_message));
        builder.setCancelable(true);
        builder.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.clearAll();
                        mPresenter.loadInCallList();
                    }
                });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    @Override
    public void showNoCallLog(boolean show) {
        mEmptyText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showLoading(boolean active) {
        mSwipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void showCallLogs(List<InCall> inCalls) {
        mCallerAdapter.replaceData(inCalls);
    }

    @Override
    public void showEula() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.eula_title));
        builder.setMessage(getString(R.string.eula_message));
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.agree),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.setEula();
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

    @Override
    public void showSearchResult(INumber number) {
        Utils.showWindow(MainActivity.this, number, FloatWindow.SEARCH_FRONT);
    }

    @Override
    public void showSearching() {
        Utils.showTextWindow(MainActivity.this, R.string.searching,
                FloatWindow.SEARCH_FRONT);
    }

    @Override
    public void showSearchFailed(boolean isOnline) {
        if (isOnline) {
            Utils.sendData(MainActivity.this, FloatWindow.WINDOW_ERROR,
                    R.string.online_failed, FloatWindow.SEARCH_FRONT);
        } else {
            Utils.showTextWindow(MainActivity.this, R.string.offline_failed,
                    FloatWindow.SEARCH_FRONT);
        }
    }

    @Override
    public void setPresenter(MainContract.Presenter presenter) {
        mPresenter = presenter;
    }
}
