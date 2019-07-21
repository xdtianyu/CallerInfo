package org.xdty.callerinfo.presenter

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import android.util.Log
import androidx.work.ListenableWorker
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import org.xdty.callerinfo.application.Application
import org.xdty.callerinfo.contract.UpgradeContact
import org.xdty.callerinfo.model.Status
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.utils.Constants.DB_NAME
import org.xdty.config.Config
import org.xdty.phone.number.util.Utils
import java.io.File
import javax.inject.Inject

class UpgradePresenter(view: UpgradeContact.View) : UpgradeContact.Presenter {

    private val tag = UpgradePresenter::class.java.simpleName

    @Inject
    lateinit var mConfig: Config

    @Inject
    lateinit var mOkHttpClient: OkHttpClient

    @Inject
    lateinit var mSetting: Setting

    private var mView: UpgradeContact.View = view

    init {
        Application.getApplication().appComponent.inject(this)
    }

    override fun start() {

    }

    override fun upgradeOfflineData(context: Context): ListenableWorker.Result {
        var result = true
        try {
            val status = loadConfig()
            Log.d(tag, "$status")

            val dbStatus = getDBStatus(context)

            if (dbStatus == null || dbStatus.version < status.version) {
                result = upgradeData(context, status)

                if (result) {
                    mView.showSucceedNotification(status)
                }
            } else {
                Log.d(tag, "Offline data is already up to date")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            mView.showFailedNotification(e)
            result = false
        }

        return if (result) {
            ListenableWorker.Result.success()
        } else {
            ListenableWorker.Result.failure()
        }
    }

    private fun loadConfig(): Status {
        return mConfig.get(Status::class.java, "CallerInfo.json")
    }

    private fun upgradeData(context: Context, status: Status): Boolean {

        var url = status.url
        if (!TextUtils.isEmpty(url)) {
            val filename = "caller_" + status.version + ".db.zip"
            url += filename
            val request = Request.Builder().url(url)
            try {
                val response = mOkHttpClient.newCall(
                        request.build()).execute()
                val downloadedFile = File(context.cacheDir, filename)
                val sink = Okio.buffer(Okio.sink(downloadedFile))
                sink.writeAll(response.body()!!.source())
                sink.close()
                response.body()!!.close()

                // check md5
                if (!Utils.checkMD5(status.md5, downloadedFile)) {
                    Log.e(tag, "Offline file md5 not match!")
                    return false
                }

                Utils.unzip(downloadedFile.absolutePath,
                        context.cacheDir.absolutePath)
                if (!downloadedFile.delete()) {
                    Log.e(tag, "downloaded file delete failed.")
                }
                val dbNew = File(context.cacheDir, "caller_" + status.version + ".db")
                val db = File(context.cacheDir, DB_NAME)
                if (dbNew.exists() && dbNew.renameTo(db)) {
                    mSetting.status = status
                    return true
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        return false
    }


    private fun getDBStatus(context: Context): Status? {
        var status: Status? = null
        var db: SQLiteDatabase? = null
        var cur: Cursor? = null
        try {
            val dbFile = File(context.cacheDir, DB_NAME)

            if (!dbFile.exists()) {
                return null
            }

            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)

            cur = db!!.rawQuery("SELECT * FROM status where id = ?", arrayOf("1"))

            if (cur!!.count >= 1 && cur.moveToFirst()) {
                val count = cur.getInt(cur.getColumnIndex("count"))
                val newCount = cur.getInt(cur.getColumnIndex("new_count"))
                val timestamp = cur.getLong(cur.getColumnIndex("time"))
                val version = cur.getInt(cur.getColumnIndex("version"))
                status = Status(version, count, newCount, timestamp, "", "")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                db?.close()
                cur?.close()
            } catch (e: Exception) {
                // ignore
            }

        }
        return status
    }

}