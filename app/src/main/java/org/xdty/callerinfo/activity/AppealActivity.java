package org.xdty.callerinfo.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.gson.JsonObject;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.xdty.callerinfo.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AppealActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = AppealActivity.class.getSimpleName();

    private static final int RESPONSE_CODE_AUTH = 1000;

    private static final String URL_OAUTH_AUTH = "https://id.xdty.org/auth/realms/xdty.org/protocol/openid-connect/auth";
    private static final String URL_OAUTH_TOKEN = "https://id.xdty.org/auth/realms/xdty.org/protocol/openid-connect/token";
    private static final String URL_API_APPEAL = "https://backend.xdty.org/api/v1/appeal";
    private static final String OAUTH_CLIENT_ID = "feedback";
    private static final String OAUTH_REDIRECT_URL = "org.xdty.callerinfo://oauth2redirect";
    private static final String PREFERENCE_AUTH = "auth";
    private static final String PREFERENCE_AUTH_KEY = "stateJson";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private FloatingActionButton mFab;
    private EditText mNumber;
    private EditText mDescription;
    private ProgressBar mProgress;

    private AuthorizationService mAuthService;

    private OkHttpClient mHttpClient;

    private AuthState mAuthState;

    private AuthorizationService.TokenResponseCallback mTokenCallback = new AuthorizationService.TokenResponseCallback() {
        @Override
        public void onTokenRequestCompleted(@Nullable TokenResponse response,
                @Nullable AuthorizationException ex) {

            mAuthState.update(response, ex);
            writeAuthState(mAuthState);

            if (response != null) {
                Log.d(TAG, "token refresh succeed.");
                submitNumber();
            } else {
                Log.e(TAG, "error token response: " + ex);
                showSubmitFailed();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appeal);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mFab = findViewById(R.id.add);
        mNumber = findViewById(R.id.number);
        mDescription = findViewById(R.id.description);
        mProgress = findViewById(R.id.progress);

        mFab.setOnClickListener(this);

        mAuthService = new AuthorizationService(this);

        mHttpClient = new OkHttpClient();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuthService.dispose();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add:
                if (validateInputs()) {
                    showProgress();
                    checkAuthState();
                }
                break;
            default:
                break;
        }
    }

    private void showProgress() {
        mProgress.setVisibility(View.VISIBLE);
        mFab.hide();
    }

    private void hideProgress() {
        mProgress.setVisibility(View.GONE);
        mFab.show();
    }

    private void checkAuthState() {
        mAuthState = readAuthState();
        if (mAuthState != null && mAuthState.isAuthorized()) {

            if (mAuthState.getNeedsTokenRefresh()) {
                Log.d(TAG, "token need refresh.");
                refreshToken();
            } else {
                Log.d(TAG, "isAuthorized, token is available");
                submitNumber();
            }

        } else {
            requestAuthorizationCode();
        }
    }

    private boolean validateInputs() {
        String number = mNumber.getText().toString().trim();
        String description = mDescription.getText().toString().trim();

        if (number.length() == 0) {
            mNumber.setError(getString(R.string.empty_input));
            return false;
        }

        if (description.length() == 0) {
            mDescription.setError(getString(R.string.empty_input));
            return false;
        }
        return true;
    }

    private void submitNumber() {

        String number = mNumber.getText().toString().trim();
        String description = mDescription.getText().toString().trim();

        // submit data to backend
        String token = mAuthState.getAccessToken();

        if (token == null) {
            Log.e(TAG, "token is null");
            showSubmitFailed();
            return;
        }

        String uid = getUidFromJwt(token);

        JsonObject json = new JsonObject();
        json.addProperty("number", number);
        json.addProperty("text", description);
        json.addProperty("user", uid);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(URL_API_APPEAL)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "onFailure: " + e);
                showSubmitFailed();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.code() == 201) {
                    showSubmitSucceed();
                    Log.d(TAG, "submit succeed");
                } else {
                    showSubmitFailed();
                    Log.e(TAG, "onResponse: " + response.code() + ", " + response.message());
                }
            }
        });

    }

    private void showSubmitSucceed() {
        mFab.post(new Runnable() {
            @Override
            public void run() {
                hideProgress();
                Snackbar.make(mFab, R.string.thanks_feedback, Snackbar.LENGTH_LONG)
                        .setAction(android.R.string.ok, null)
                        .show();
            }
        });
    }

    private void showSubmitFailed() {
        mFab.post(new Runnable() {
            @Override
            public void run() {
                hideProgress();
                Snackbar.make(mFab, R.string.auth_failed, Snackbar.LENGTH_LONG)
                        .setAction(android.R.string.ok, null)
                        .show();
            }
        });

    }

    private String getUidFromJwt(String token) {
        String uid = "";
        String[] sections = token.split("\\.");
        try {
            JSONObject claims = parseJwtSection(sections[1]);
            uid = claims.getString("sub");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return uid;
    }

    private void refreshToken() {
        mAuthService.performTokenRequest(mAuthState.createTokenRefreshRequest(), mTokenCallback);
    }

    private void requestAuthorizationCode() {
        AuthorizationServiceConfiguration configuration =
                new AuthorizationServiceConfiguration(Uri.parse(URL_OAUTH_AUTH),
                        Uri.parse(URL_OAUTH_TOKEN));

        mAuthState = new AuthState(configuration);

        Uri redirectUri = Uri.parse(OAUTH_REDIRECT_URL);

        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(configuration,
                OAUTH_CLIENT_ID, ResponseTypeValues.CODE, redirectUri);

        AuthorizationRequest authRequest = builder.build();

        Intent authIntent = mAuthService.getAuthorizationRequestIntent(authRequest);
        startActivityForResult(authIntent, RESPONSE_CODE_AUTH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESPONSE_CODE_AUTH) {
            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
            AuthorizationException ex = AuthorizationException.fromIntent(data);

            mAuthState.update(response, ex);

            if (response != null) {
                exchangeTokenFromAuthCode(response);
            } else {
                if (ex != null) {
                    Log.e(TAG, "error oauth code: " + ex);
                }
            }
        }
    }

    private void exchangeTokenFromAuthCode(AuthorizationResponse response) {
        mAuthService.performTokenRequest(response.createTokenExchangeRequest(), mTokenCallback);
    }

    @Nullable
    private AuthState readAuthState() {
        SharedPreferences authPrefs = getSharedPreferences(PREFERENCE_AUTH, MODE_PRIVATE);
        String stateJson = authPrefs.getString(PREFERENCE_AUTH_KEY, "");
        try {
            return AuthState.jsonDeserialize(stateJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeAuthState(@NonNull AuthState state) {
        SharedPreferences authPrefs = getSharedPreferences(PREFERENCE_AUTH, MODE_PRIVATE);
        authPrefs.edit().putString(PREFERENCE_AUTH_KEY, state.jsonSerializeString()).apply();
    }

    private static JSONObject parseJwtSection(String section) throws JSONException {
        byte[] decodedSection = Base64.decode(section, Base64.URL_SAFE);
        String jsonString = new String(decodedSection);
        return new JSONObject(jsonString);
    }
}
