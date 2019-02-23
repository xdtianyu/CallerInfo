package org.xdty.callerinfo.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;

import org.xdty.callerinfo.R;

public class AppealActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = AppealActivity.class.getSimpleName();

    private static final int RESPONSE_CODE_AUTH = 1000;

    AuthorizationService authService;

    AuthState authState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appeal);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        FloatingActionButton addButton = findViewById(R.id.add);

        addButton.setOnClickListener(this);
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
                sendNumberRequest();
                break;
            default:
                break;
        }
    }

    private void sendNumberRequest() {

        AuthorizationServiceConfiguration configuration = new AuthorizationServiceConfiguration(
                Uri.parse("https:/id.xdty.org/auth/realms/xdty.org/protocol/openid-connect/auth"),
                Uri.parse("https:/id.xdty.org/auth/realms/xdty.org/protocol/openid-connect/token")
        );

        authState = readAuthState(configuration);

        if (authState.isAuthorized() && !authState.getNeedsTokenRefresh()) {
            Log.d(TAG, "isAuthorized");
        } else {
            auth(configuration);
        }
    }

    private void auth(AuthorizationServiceConfiguration configuration) {
        String clientId = "feedback";
        Uri redirectUri = Uri.parse("org.xdty.callerinfo:/oauth2redirect");

        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(configuration,
                clientId, ResponseTypeValues.CODE, redirectUri);

        AuthorizationRequest authRequest = builder.build();

        authService = new AuthorizationService(this);
        Intent authIntent = authService.getAuthorizationRequestIntent(authRequest);
        startActivityForResult(authIntent, RESPONSE_CODE_AUTH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESPONSE_CODE_AUTH) {
            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);

            if (response != null) {
                authService.performTokenRequest(response.createTokenExchangeRequest(),
                        new AuthorizationService.TokenResponseCallback() {
                            @Override
                            public void onTokenRequestCompleted(TokenResponse response,
                                    AuthorizationException ex) {
                                if (response != null) {
                                    authState.update(response, ex);
                                    writeAuthState(authState);
                                } else {
                                    Log.e(TAG, "error token response: " + ex);
                                }
                            }
                        });
            } else {
                AuthorizationException ex = AuthorizationException.fromIntent(data);
                if (ex != null) {
                    Log.e(TAG, "error oauth response: " + ex);
                }
            }
        }
    }

    @NonNull
    public AuthState readAuthState(
            AuthorizationServiceConfiguration configuration) {
        SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
        String stateJson = authPrefs.getString("stateJson", "");
        try {
            return AuthState.jsonDeserialize(stateJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new AuthState(configuration);
    }

    public void writeAuthState(@NonNull AuthState state) {
        SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
        authPrefs.edit().putString("stateJson", state.jsonSerializeString()).apply();
    }
}
