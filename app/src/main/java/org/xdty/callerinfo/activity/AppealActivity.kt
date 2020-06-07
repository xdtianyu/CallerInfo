package org.xdty.callerinfo.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import net.openid.appauth.*
import net.openid.appauth.AuthorizationService.TokenResponseCallback
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request.Builder
import org.json.JSONException
import org.json.JSONObject
import org.xdty.callerinfo.R
import java.io.IOException

class AppealActivity : AppCompatActivity(), OnClickListener {

    private lateinit var mFab: FloatingActionButton
    private lateinit var mNumber: EditText
    private lateinit var mDescription: EditText
    private lateinit var mProgress: ProgressBar

    private lateinit var mAuthService: AuthorizationService
    private lateinit var mHttpClient: OkHttpClient
    private var mAuthState: AuthState? = null

    private val mTokenCallback = TokenResponseCallback { response, ex ->
        mAuthState?.update(response, ex)
        writeAuthState(mAuthState!!)
        if (response != null) {
            Log.d(TAG, "token refresh succeed.")
            submitNumber()
        } else {
            Log.e(TAG, "error token response: $ex")
            showSubmitFailed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appeal)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        mFab = findViewById(R.id.add)
        mNumber = findViewById(R.id.number)
        mDescription = findViewById(R.id.description)
        mProgress = findViewById(R.id.progress)
        mFab.setOnClickListener(this)
        mAuthService = AuthorizationService(this)
        mHttpClient = OkHttpClient()
    }

    override fun onDestroy() {
        super.onDestroy()
        mAuthService.dispose()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.add -> if (validateInputs()) {
                showProgress()
                checkAuthState()
            }
            else -> {
            }
        }
    }

    private fun showProgress() {
        mProgress.visibility = View.VISIBLE
        mFab.hide()
    }

    private fun hideProgress() {
        mProgress.visibility = View.GONE
        mFab.show()
    }

    private fun checkAuthState() {
        mAuthState = readAuthState()
        if (mAuthState != null && mAuthState?.isAuthorized!!) {
            if (mAuthState?.needsTokenRefresh!!) {
                Log.d(TAG, "token need refresh.")
                refreshToken()
            } else {
                Log.d(TAG, "isAuthorized, token is available")
                submitNumber()
            }
        } else {
            requestAuthorizationCode()
        }
    }

    private fun validateInputs(): Boolean {
        val number = mNumber.text.toString().trim { it <= ' ' }
        val description = mDescription.text.toString().trim { it <= ' ' }
        if (number.length == 0) {
            mNumber.error = getString(R.string.empty_input)
            return false
        }
        if (description.length == 0) {
            mDescription.error = getString(R.string.empty_input)
            return false
        }
        return true
    }

    private fun submitNumber() {
        val number = mNumber.text.toString().trim { it <= ' ' }
        val description = mDescription.text.toString().trim { it <= ' ' }
        // submit data to backend
        val token = mAuthState?.accessToken
        if (token == null) {
            Log.e(TAG, "token is null")
            showSubmitFailed()
            return
        }
        val uid = getUidFromJwt(token)
        val json = JsonObject()
        json.addProperty("number", number)
        json.addProperty("text", description)
        json.addProperty("user", uid)
        val body = RequestBody.create(JSON, json.toString())
        val request = Builder()
                .url(URL_API_APPEAL)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()
        mHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "onFailure: $e")
                showSubmitFailed()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 201) {
                    showSubmitSucceed()
                    Log.d(TAG, "submit succeed")
                } else {
                    showSubmitFailed()
                    Log.e(TAG, "onResponse: " + response.code + ", " + response.message)
                }
            }
        })
    }

    private fun showSubmitSucceed() {
        mFab.post {
            hideProgress()
            Snackbar.make(mFab, R.string.thanks_feedback, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, null)
                    .show()
        }
    }

    private fun showSubmitFailed() {
        mFab.post {
            hideProgress()
            Snackbar.make(mFab, R.string.auth_failed, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, null)
                    .show()
        }
    }

    private fun getUidFromJwt(token: String): String {
        var uid = ""
        val sections = token.split("\\.").toTypedArray()
        try {
            val claims = parseJwtSection(sections[1])
            uid = claims.getString("sub")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return uid
    }

    private fun refreshToken() {
        mAuthService.performTokenRequest(mAuthState?.createTokenRefreshRequest()!!, mTokenCallback)
    }

    private fun requestAuthorizationCode() {
        val configuration = AuthorizationServiceConfiguration(Uri.parse(URL_OAUTH_AUTH),
                Uri.parse(URL_OAUTH_TOKEN))
        mAuthState = AuthState(configuration)
        val redirectUri = Uri.parse(OAUTH_REDIRECT_URL)
        val builder = AuthorizationRequest.Builder(configuration,
                OAUTH_CLIENT_ID, ResponseTypeValues.CODE, redirectUri)
        val authRequest: AuthorizationRequest = builder.build()
        val authIntent = mAuthService.getAuthorizationRequestIntent(authRequest)
        startActivityForResult(authIntent, RESPONSE_CODE_AUTH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RESPONSE_CODE_AUTH) {
            val response = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)
            mAuthState?.update(response, ex)
            if (response != null) {
                exchangeTokenFromAuthCode(response)
            } else {
                if (ex != null) {
                    Log.e(TAG, "error oauth code: $ex")
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun exchangeTokenFromAuthCode(response: AuthorizationResponse) {
        mAuthService.performTokenRequest(response.createTokenExchangeRequest(), mTokenCallback)
    }

    private fun readAuthState(): AuthState? {
        val authPrefs = getSharedPreferences(PREFERENCE_AUTH, Context.MODE_PRIVATE)
        val stateJson = authPrefs.getString(PREFERENCE_AUTH_KEY, "")
        try {
            return AuthState.jsonDeserialize(stateJson!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun writeAuthState(state: AuthState) {
        val authPrefs = getSharedPreferences(PREFERENCE_AUTH, Context.MODE_PRIVATE)
        authPrefs.edit().putString(PREFERENCE_AUTH_KEY, state.jsonSerializeString()).apply()
    }

    companion object {
        private val TAG = AppealActivity::class.java.simpleName
        private const val RESPONSE_CODE_AUTH = 1000
        private const val URL_OAUTH_AUTH = "https://id.xdty.org/auth/realms/xdty.org/protocol/openid-connect/auth"
        private const val URL_OAUTH_TOKEN = "https://id.xdty.org/auth/realms/xdty.org/protocol/openid-connect/token"
        private const val URL_API_APPEAL = "https://backend.xdty.org/api/v1/appeal"
        private const val OAUTH_CLIENT_ID = "feedback"
        private const val OAUTH_REDIRECT_URL = "org.xdty.callerinfo://oauth2redirect"
        private const val PREFERENCE_AUTH = "auth"
        private const val PREFERENCE_AUTH_KEY = "stateJson"
        private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
        @Throws(JSONException::class)
        private fun parseJwtSection(section: String): JSONObject {
            val decodedSection = Base64.decode(section, Base64.URL_SAFE)
            val jsonString = String(decodedSection)
            return JSONObject(jsonString)
        }
    }
}