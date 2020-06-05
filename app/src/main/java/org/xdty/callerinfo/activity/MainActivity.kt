package org.xdty.callerinfo.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog.Builder
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.Callback
import org.xdty.callerinfo.R
import org.xdty.callerinfo.application.Application
import org.xdty.callerinfo.contract.MainContract
import org.xdty.callerinfo.di.DaggerMainComponent
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.di.modules.MainModule
import org.xdty.callerinfo.fragment.MainBottomSheetFragment
import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.InCall
import org.xdty.callerinfo.model.permission.Permission
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.service.FloatWindow
import org.xdty.callerinfo.utils.Window
import org.xdty.callerinfo.view.CallerAdapter
import org.xdty.phone.number.model.INumber
import org.xdty.phone.number.model.caller.Status
import javax.inject.Inject

class MainActivity : BaseActivity(), MainContract.View {
    @Inject
    internal lateinit var mPresenter: MainContract.Presenter
    @Inject
    internal lateinit var mPermission: Permission
    @Inject
    internal lateinit var mSetting: Setting
    @Inject
    internal lateinit var mWindow: Window

    private lateinit var mToolbar: Toolbar

    private var mScreenWidth = 0
    private lateinit var mEmptyText: TextView
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mCallerAdapter: CallerAdapter
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mMainLayout: FrameLayout
    private var mLastSearchTime: Long = 0
    private var mUpdateDataDialog: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerMainComponent.builder()
                .appModule(AppModule(Application.application))
                .mainModule(MainModule(this))
                .build()
                .inject(this)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        mPresenter.checkEula()
        mScreenWidth = mSetting.screenWidth
        mMainLayout = findViewById(R.id.main_layout)
        mEmptyText = findViewById(R.id.empty_text)
        mRecyclerView = findViewById(R.id.history_list)
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mRecyclerView.layoutManager = layoutManager
        mCallerAdapter = CallerAdapter(mPresenter)
        mRecyclerView.adapter = mCallerAdapter
        mRecyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> mPresenter.invalidateDataUpdate(false)
                    RecyclerView.SCROLL_STATE_DRAGGING,
                    RecyclerView.SCROLL_STATE_SETTLING -> mPresenter.invalidateDataUpdate(true)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // can scroll up and disable refresh
                mSwipeRefreshLayout.isEnabled = !recyclerView.canScrollVertically(-1)
            }
        })
        val itemTouchHelper = ItemTouchHelper(object : SimpleCallback(0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder,
                                target: ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
                val inCall = mCallerAdapter.getItem(viewHolder.adapterPosition)
                mPresenter.removeInCallFromList(inCall)
                mCallerAdapter.notifyDataSetChanged()
                val snackbar = Snackbar.make(mToolbar, R.string.deleted,
                        Snackbar.LENGTH_LONG)
                snackbar.setAction(getString(R.string.undo)) {
                    snackbar.dismiss()
                    mPresenter.loadInCallList()
                }
                snackbar.setCallback(object : Callback() {
                    @SuppressLint("SwitchIntDef")
                    override fun onDismissed(snackbar: Snackbar, event: Int) {
                        when (event) {
                            DISMISS_EVENT_MANUAL,
                            DISMISS_EVENT_ACTION -> {
                            }
                            DISMISS_EVENT_CONSECUTIVE,
                            DISMISS_EVENT_SWIPE,
                            DISMISS_EVENT_TIMEOUT -> mPresenter.removeInCall(inCall)
                            else -> mPresenter.removeInCall(inCall)
                        }
                        super.onDismissed(snackbar, event)
                    }
                })
                snackbar.show()
            }

            override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                val swiping = actionState == ItemTouchHelper.ACTION_STATE_SWIPE
                mSwipeRefreshLayout.isEnabled = !swiping
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView,
                                     viewHolder: ViewHolder,
                                     dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState,
                        isCurrentlyActive)
                (viewHolder as CallerAdapter.ViewHolder).setAlpha(1 - Math.abs(dX) / mScreenWidth * 1.2f)
            }
        })
        itemTouchHelper.attachToRecyclerView(mRecyclerView)
        mSwipeRefreshLayout.setOnRefreshListener { mPresenter.loadCallerMap() }
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun getTitleId(): Int {
        return R.string.app_name
    }

    override fun onResume() {
        super.onResume()
        mPresenter.start()
    }

    @SuppressLint("InlinedApi")
    override fun onStart() {
        super.onStart()
        if (!mPresenter.canDrawOverlays()) {
            mPermission.requestDrawOverlays(this, REQUEST_CODE_OVERLAY_PERMISSION)
        }
        var res = mPresenter.checkPermission(Manifest.permission.READ_PHONE_STATE)
        if (res != PackageManager.PERMISSION_GRANTED) {
            mPermission.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE),
                    REQUEST_CODE_ASK_PERMISSIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            res = mPresenter.checkPermission(Manifest.permission.READ_CALL_LOG)
            if (res != PackageManager.PERMISSION_GRANTED) {
                mPermission.requestPermissions(this, arrayOf(Manifest.permission.READ_CALL_LOG),
                        REQUEST_CODE_ASK_CALL_LOG_PERMISSIONS)
            }
        }
    }

    override fun onStop() {
        if (FloatWindow.status() != FloatWindow.STATUS_CLOSE) { // FixME: window in other ui may close because async
            mWindow.closeWindow()
        }
        mPresenter.clearSearch()
        if (mUpdateDataDialog != null && mUpdateDataDialog?.isShowing!!) {
            mUpdateDataDialog?.cancel()
        }
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (!mPresenter.canDrawOverlays()) {
                Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted...")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (FloatWindow.status() != FloatWindow.STATUS_CLOSE) {
            mWindow.closeWindow()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS, REQUEST_CODE_ASK_CALL_LOG_PERMISSIONS -> {
                if (grantResults.isEmpty()) {
                    Log.e(TAG, "grantResults is empty!")
                    return
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT)
                            .show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val floatWindowMenu = menu.findItem(R.id.action_float_window)
        if (FloatWindow.status() != FloatWindow.STATUS_CLOSE) {
            floatWindowMenu.setTitle(R.string.close_window)
        } else {
            floatWindowMenu.setTitle(R.string.action_float_window)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchView = MenuItemCompat.getActionView(
                menu.findItem(R.id.action_search)) as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.maxWidth = mScreenWidth
        searchView.inputType = InputType.TYPE_CLASS_PHONE
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                Log.d(TAG, "onQueryTextSubmit: $query")
                if (System.currentTimeMillis() - mLastSearchTime > 1000) {
                    mPresenter.search(query)
                    mRecyclerView.visibility = View.INVISIBLE
                    mMainLayout.setBackgroundColor(ContextCompat.getColor(this@MainActivity,
                            R.color.dark))
                    mLastSearchTime = System.currentTimeMillis()
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                mWindow.closeWindow()
                mRecyclerView.visibility = View.VISIBLE
                mMainLayout.setBackgroundColor(ContextCompat.getColor(this@MainActivity,
                        R.color.transparent))
                return false
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_float_window -> if (FloatWindow.status() == FloatWindow.STATUS_CLOSE) {
                mWindow.showTextWindow(R.string.float_window_hint, Window.Type.POSITION)
            } else {
                mWindow.closeWindow()
            }
            R.id.action_clear_history -> clearHistory()
            R.id.action_clear_cache -> clearCache()
        }
        return true
    }

    private fun clearHistory() {
        val builder = Builder(this)
        builder.setTitle(getString(R.string.action_clear_history))
        builder.setMessage(getString(R.string.clear_history_message))
        builder.setCancelable(true)
        builder.setPositiveButton(getString(R.string.ok)) { _, _ -> mPresenter.clearAll() }
        builder.setNegativeButton(getString(R.string.cancel), null)
        builder.show()
    }

    private fun clearCache() {
        val builder = Builder(this)
        builder.setTitle(getString(R.string.action_clear_cache))
        builder.setMessage(getString(R.string.clear_cache_confirm_message))
        builder.setCancelable(true)
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
            mPresenter.clearCache()
            Snackbar.make(mToolbar, R.string.clear_cache_message, Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.ok), null)
                    .show()
        }
        builder.setNegativeButton(getString(R.string.cancel), null)
        builder.show()
    }

    override fun showNoCallLog(show: Boolean) {
        mEmptyText.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showLoading(active: Boolean) {
        mSwipeRefreshLayout.isRefreshing = active
    }

    override fun showCallLogs(inCalls: List<InCall>) {
        mCallerAdapter.replaceData(inCalls)
    }

    override fun showEula() {
        val builder = Builder(this)
        builder.setTitle(getString(R.string.eula_title))
        builder.setMessage(getString(R.string.eula_message))
        builder.setCancelable(false)

        builder.setPositiveButton(getString(R.string.agree)) { _, _ -> mPresenter.setEula() }
        builder.setNegativeButton(getString(R.string.disagree)) { _, _ -> finish() }
        builder.show()
    }

    override fun showSearchResult(number: INumber) {
        Log.d(TAG, "showSearchResult: " + number.number)
        mWindow.showWindow(number, Window.Type.SEARCH)
    }

    override fun showSearching() {
        Log.d(TAG, "showSearching")
        mWindow.showTextWindow(R.string.searching, Window.Type.SEARCH)
    }

    override fun showSearchFailed(isOnline: Boolean) {
        Log.d(TAG, "showSearchFailed: isOnline=$isOnline")
        if (isOnline) {
            mWindow.sendData(FloatWindow.WINDOW_ERROR, R.string.online_failed, Window.Type.SEARCH)
        } else {
            mWindow.showTextWindow(R.string.offline_failed, Window.Type.SEARCH)
        }
    }

    override val context: Context
        get() = this.applicationContext

    override fun notifyUpdateData(status: Status) {
        val snackbar = Snackbar.make(mToolbar, R.string.new_offline_data,
                Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(getString(R.string.update), object : OnClickListener {
            override fun onClick(v: View) {
                snackbar.dismiss()
                mPresenter.dispatchUpdate(status)
            }
        })
        snackbar.show()
    }

    override fun showUpdateData(status: Status) {
        if (mUpdateDataDialog != null && mUpdateDataDialog?.isShowing!!) {
            mUpdateDataDialog?.dismiss()
            mUpdateDataDialog = null
        }
        mUpdateDataDialog = MaterialDialog.Builder(this)
                .cancelable(true)
                .theme(Theme.LIGHT)
                .title(R.string.offline_data_update)
                .content(getString(R.string.offline_data_status))
                .progress(true, 0).build()
        mUpdateDataDialog?.show()
    }

    override fun updateDataFinished(result: Boolean) {
        if (mUpdateDataDialog != null && mUpdateDataDialog?.isShowing!!) {
            mUpdateDataDialog?.dismiss()
        }
        val message = if (result) R.string.offline_data_success else R.string.offline_data_failed
        val snackbar = Snackbar.make(mToolbar, message, Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.ok), object : OnClickListener {
            override fun onClick(v: View) {
                snackbar.dismiss()
            }
        })
        snackbar.show()
    }

    override fun showBottomSheet(inCall: InCall) { // show bottom sheet dialog
        MainBottomSheetFragment.newInstance(inCall).show(supportFragmentManager, "dialog")
    }

    override fun attachCallerMap(callerMap: Map<String, Caller>) {
        mPresenter.loadInCallList()
    }

    override fun setPresenter(presenter: MainContract.Presenter) {
        mPresenter = presenter
    }

    companion object {
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
        const val REQUEST_CODE_ASK_PERMISSIONS = 1002
        const val REQUEST_CODE_ASK_CALL_LOG_PERMISSIONS = 1003
        private val TAG = MainActivity::class.java.simpleName
    }
}