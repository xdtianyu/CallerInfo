package org.xdty.callerinfo.activity

import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import org.xdty.callerinfo.R
import org.xdty.callerinfo.application.Application.Companion.appComponent
import org.xdty.callerinfo.model.database.Database
import org.xdty.callerinfo.model.db.MarkedRecord
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.utils.Alarm
import javax.inject.Inject

class MarkActivity : BaseActivity(), OnDismissListener {

    private var isPaused = false
    @Inject
    internal lateinit var mSetting: Setting
    @Inject
    internal lateinit var mDatabase: Database
    @Inject
    internal lateinit var mAlarm: Alarm

    private var mAlertDialog: AlertDialog? = null
    private lateinit var mNumberList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""
        appComponent.inject(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        isPaused = false
        val intent = intent
        val number = intent.getStringExtra(NUMBER)
        if (!TextUtils.isEmpty(number)) {
            showAlertDialog(number!!)
        } else {
            mNumberList = mSetting.paddingMarks
            if (mNumberList.size > 0) {
                showAlertDialog(mNumberList.get(0))
            } else {
                Log.e(TAG, "number is null or empty! $number")
                finish()
            }
        }
    }

    private fun showAlertDialog(number: String) {
        val title = getString(R.string.mark_number) + " (" + number + ")"
        val builder = Builder(this, R.style.MarkDialogStyle)
        builder.setTitle(title)
        builder.setOnDismissListener(this)
        builder.setNegativeButton(R.string.cancel, null)
        builder.setCancelable(false)
        builder.setSingleChoiceItems(R.array.mark_type, -1) { dialog, _ ->
            (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
        }
        builder.setNeutralButton(R.string.ignore) { _, _ ->
            // ignore number
            val markedRecord = MarkedRecord()
            markedRecord.uid = mSetting.uid
            markedRecord.number = number
            markedRecord.type = MarkedRecord.TYPE_IGNORE
            markedRecord.typeName = getString(R.string.ignore_number)
            markedRecord.isReported = true
            mDatabase.saveMarked(markedRecord)
            mDatabase.updateCaller(markedRecord)
        }
        builder.setPositiveButton(R.string.ok) { dialog, _ ->
            val lv = (dialog as AlertDialog).listView
            val type = lv.adapter.getItem(lv.checkedItemPosition) as String
            val markedRecord = MarkedRecord()
            markedRecord.uid = mSetting.uid
            markedRecord.number = number
            markedRecord.type = lv.checkedItemPosition
            markedRecord.typeName = type
            mDatabase.saveMarked(markedRecord)
            mDatabase.updateCaller(markedRecord)
            mAlarm.alarm()
        }
        mAlertDialog = builder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAlertDialog?.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            mAlertDialog?.window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        mAlertDialog?.show()
        mAlertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        if (mAlertDialog != null) {
            mAlertDialog?.cancel()
        }
        finish()
    }

    override val layoutId: Int
        get() = R.layout.activity_mark

    override val titleId: Int
        get() = R.string.app_name

    override fun onDismiss(dialog: DialogInterface) {
        if (mNumberList.size > 0) {
            mSetting.removePaddingMark(mNumberList[0])
            mNumberList.removeAt(0)
            if (mNumberList.size > 0) {
                showAlertDialog(mNumberList[0])
                return
            }
        }
        if (!isPaused && (mNumberList.size == 0)) {
            finish()
        }
    }

    companion object {
        const val NUMBER = "number"
        private val TAG = MarkActivity::class.java.simpleName
    }
}