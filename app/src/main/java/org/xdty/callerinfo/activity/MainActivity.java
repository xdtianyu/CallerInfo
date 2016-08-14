package org.xdty.callerinfo.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.contract.MainContract;
import org.xdty.callerinfo.di.DaggerMainComponent;
import org.xdty.callerinfo.di.modules.AppModule;
import org.xdty.callerinfo.di.modules.MainModule;
import org.xdty.callerinfo.fragment.MainBottomSheetFragment;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.receiver.IncomingCall;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.utils.Window;
import org.xdty.callerinfo.view.CallerAdapter;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.caller.Status;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class MainActivity extends BaseActivity implements MainContract.View {

    public final static int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    public final static int REQUEST_CODE_ASK_PERMISSIONS = 1002;
    private final static String TAG = MainActivity.class.getSimpleName();

    @Inject
    MainContract.Presenter mPresenter;

    @Inject
    Permission mPermission;

    @Inject
    Setting mSetting;

    @Inject
    Window mWindow;

    private Toolbar mToolbar;
    private int mScreenWidth;
    private TextView mEmptyText;
    private RecyclerView mRecyclerView;
    private CallerAdapter mCallerAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FrameLayout mMainLayout;
    private long mLastSearchTime;
    private MaterialDialog mUpdateDataDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DaggerMainComponent.builder()
                .appModule(new AppModule(Application.getApplication()))
                .mainModule(new MainModule(this))
                .build()
                .inject(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mPresenter.checkEula();
        mScreenWidth = mSetting.getScreenWidth();

        mMainLayout = (FrameLayout) findViewById(R.id.main_layout);
        mEmptyText = (TextView) findViewById(R.id.empty_text);
        mRecyclerView = (RecyclerView) findViewById(R.id.history_list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        mCallerAdapter = new CallerAdapter(mPresenter);
        mRecyclerView.setAdapter(mCallerAdapter);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // invalidate data update if is scrolling
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        mPresenter.invalidateDataUpdate(false);
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        mPresenter.invalidateDataUpdate(true);
                        break;
                }
            }

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
                final InCall inCall = mCallerAdapter.getItem(viewHolder.getAdapterPosition());
                mPresenter.removeInCallFromList(inCall);
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
                            case DISMISS_EVENT_CONSECUTIVE:
                            case DISMISS_EVENT_SWIPE:
                            case DISMISS_EVENT_TIMEOUT:
                            default:
                                mPresenter.removeInCall(inCall);
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
                mPresenter.loadCallerMap();
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
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mPresenter.canDrawOverlays()) {
            mPermission.requestDrawOverlays(this, REQUEST_CODE_OVERLAY_PERMISSION);
        }

        int res = mPresenter.checkPermission(Manifest.permission.READ_PHONE_STATE);
        if (res != PackageManager.PERMISSION_GRANTED) {
            mPermission.requestPermissions(this,
                    new String[] { Manifest.permission.READ_PHONE_STATE },
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    protected void onStop() {
        if (FloatWindow.status() != FloatWindow.STATUS_CLOSE) {
            // FixME: window in other ui may close because async
            mWindow.closeWindow();
        }
        mPresenter.clearSearch();
        if (mUpdateDataDialog != null && mUpdateDataDialog.isShowing()) {
            mUpdateDataDialog.cancel();
        }
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
            mWindow.closeWindow();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults.length == 0) {
                    Log.e(TAG, "grantResults is empty!");
                    return;
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "READ_PHONE_STATE Denied", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    IncomingCall.IncomingCallListener.init(this);
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
                mWindow.closeWindow();
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
                    mWindow.showTextWindow(R.string.float_window_hint, Window.Type.POSITION);
                } else {
                    mWindow.closeWindow();
                }
                break;
            case R.id.action_clear_history:
                clearHistory();
                break;
            case R.id.action_clear_cache:
                clearCache();
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
                    }
                });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void clearCache() {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.action_clear_cache));
        builder.setMessage(getString(R.string.clear_cache_confirm_message));
        builder.setCancelable(true);
        builder.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.clearCache();
                        Snackbar.make(mToolbar, R.string.clear_cache_message, Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.ok), null)
                                .show();
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
        Log.d(TAG, "showSearchResult: " + number.getNumber());
        mWindow.showWindow(number, Window.Type.SEARCH);
    }

    @Override
    public void showSearching() {
        Log.d(TAG, "showSearching");
        mWindow.showTextWindow(R.string.searching, Window.Type.SEARCH);
    }

    @Override
    public void showSearchFailed(boolean isOnline) {
        Log.d(TAG, "showSearchFailed: isOnline=" + isOnline);
        if (isOnline) {
            mWindow.sendData(FloatWindow.WINDOW_ERROR, R.string.online_failed, Window.Type.SEARCH);
        } else {
            mWindow.showTextWindow(R.string.offline_failed, Window.Type.SEARCH);
        }
    }

    @Override
    public Context getContext() {
        return this.getApplicationContext();
    }

    @Override
    public void notifyUpdateData(final Status status) {
        final Snackbar snackbar = Snackbar.make(mToolbar, R.string.new_offline_data,
                Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction(getString(R.string.update), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
                mPresenter.dispatchUpdate(status);
            }
        });
        snackbar.show();
    }

    @Override
    public void showUpdateData(Status status) {

        if (mUpdateDataDialog != null && mUpdateDataDialog.isShowing()) {
            mUpdateDataDialog.dismiss();
            mUpdateDataDialog = null;
        }

        mUpdateDataDialog = new MaterialDialog.Builder(this)
                .cancelable(true)
                .theme(Theme.LIGHT)
                .title(R.string.offline_data_update)
                .content(getString(R.string.offline_data_status))
                .progress(true, 0).build();
        mUpdateDataDialog.show();
    }

    @Override
    public void updateDataFinished(boolean result) {
        if (mUpdateDataDialog != null && mUpdateDataDialog.isShowing()) {
            mUpdateDataDialog.dismiss();
        }
        int message = result ? R.string.offline_data_success : R.string.offline_data_failed;
        final Snackbar snackbar = Snackbar.make(mToolbar, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(getString(R.string.ok), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    @Override
    public void showBottomSheet(InCall inCall) {
        // show bottom sheet dialog
        MainBottomSheetFragment.newInstance(inCall).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void attachCallerMap(Map<String, Caller> callers) {
        mPresenter.loadInCallList();
    }

    @Override
    public void setPresenter(MainContract.Presenter presenter) {
        mPresenter = presenter;
    }
}
