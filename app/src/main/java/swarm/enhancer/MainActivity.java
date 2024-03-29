package swarm.enhancer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.foursquare.android.nativeoauth.FoursquareCancelException;
import com.foursquare.android.nativeoauth.FoursquareDenyException;
import com.foursquare.android.nativeoauth.FoursquareInvalidRequestException;
import com.foursquare.android.nativeoauth.FoursquareOAuth;
import com.foursquare.android.nativeoauth.FoursquareOAuthException;
import com.foursquare.android.nativeoauth.FoursquareUnsupportedVersionException;
import com.foursquare.android.nativeoauth.model.AccessTokenResponse;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;

import java.util.Locale;

import swarm.enhancer.foursquare.FileTokenStore;
import swarm.enhancer.utils.Log;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_FSQ_CONNECT = 200;
    private static final int REQUEST_CODE_FSQ_TOKEN_EXCHANGE = 201;

    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 300;

    private final String clientId;
    private final String clientSecret;

    public MainActivity() {
        clientId = BuildConfig.CLIENT_ID;
        clientSecret = BuildConfig.CLIENT_SECRET;
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateActivityLog();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("activity-updated"));

        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        } else {
            ensureUi();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                Log.i(MainActivity.class, this, "Location permission granted", true);

                ensureUi();
            } else {
                // Permission denied
                Log.w(MainActivity.class, this, "Location permission was denied, app won't work", true);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_FSQ_CONNECT:
                onCompleteConnect(resultCode, data);
                break;

            case REQUEST_CODE_FSQ_TOKEN_EXCHANGE:
                onCompleteTokenExchange(resultCode, data);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void ensureUi() {
        final String token = FileTokenStore.get(getApplicationContext()).getToken();
        final boolean isAuthorized = !TextUtils.isEmpty(token);

        // Check log-in status
        if (isAuthorized) {
            Log.d(MainActivity.class, this, "User is authenticated", false);

            // Start or stop the check-in service
            startStopCheckInService(token);
        }

        TextView tvMessage = findViewById(R.id.tvMessage);
        tvMessage.setVisibility(isAuthorized ? View.VISIBLE : View.GONE);

        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setVisibility(isAuthorized ? View.GONE : View.VISIBLE);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the native auth flow.
                Intent intent = FoursquareOAuth.getConnectIntent(MainActivity.this, clientId);

                // If the device does not have the Foursquare app installed, we'd
                // get an intent back that would open the Play Store for download.
                // Otherwise we start the auth flow.
                if (FoursquareOAuth.isPlayStoreIntent(intent)) {
                    Log.i(MainActivity.class, MainActivity.this, getString(R.string.app_not_installed_message), true);
                    startActivity(intent);
                } else {
                    startActivityForResult(intent, REQUEST_CODE_FSQ_CONNECT);
                }
            }
        });

        // Handle button pause/resume updates
        final Button btnPauseResumeUpdates = findViewById(R.id.btnPauseResumeUpdates);
        btnPauseResumeUpdates.setVisibility(isAuthorized ? View.VISIBLE : View.GONE);
        // Set button text according to current state
        setPauseResumeUpdatesButtonText(btnPauseResumeUpdates);
        btnPauseResumeUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save inverted value to preferences
                Preferences.setUpdatesPaused(MainActivity.this, !Preferences.areUpdatesPaused(MainActivity.this));

                // Start or stop the check-in service
                startStopCheckInService(token);

                // Set button text according to current state
                setPauseResumeUpdatesButtonText(btnPauseResumeUpdates);
            }
        });

        // Handle activity log view
        TextView tvActivity = findViewById(R.id.tvActivity);
        tvActivity.setMovementMethod(new ScrollingMovementMethod());
        tvActivity.setVisibility(isAuthorized ? View.VISIBLE : View.GONE);
        updateActivityLog();

        // Load settings to the UI
        loadUpdateInterval();
        loadLocalRadius();

        final TableLayout tableLayout = findViewById(R.id.tableLayout);
        tableLayout.setVisibility(isAuthorized ? View.VISIBLE : View.GONE);

        final Button btnApply = findViewById(R.id.btnApply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText txtUpdateInterval = findViewById(R.id.txtUpdateInterval);
                String updateIntervalString = txtUpdateInterval.getText().toString();

                float currentUpdateInterval = Preferences.getUpdateInterval(MainActivity.this);
                float updateInterval = currentUpdateInterval;
                try {
                    updateInterval = Float.parseFloat(updateIntervalString);
                } catch (NumberFormatException e) {
                    Log.e(MainActivity.class, MainActivity.this, String.format(Locale.US, "Invalid number format: %s", updateIntervalString), false);
                }

                final EditText txtLocalRadius = findViewById(R.id.txtLocalRadius);
                String localRadiusString = txtLocalRadius.getText().toString();

                float currentLocalRadius = Preferences.getLocalRadius(MainActivity.this);
                float localRadius = currentLocalRadius;
                try {
                    localRadius = Float.parseFloat(localRadiusString);
                } catch (NumberFormatException e) {
                    Log.e(MainActivity.class, MainActivity.this, String.format(Locale.US, "Invalid number format: %s", localRadiusString), false);
                }

                boolean updated = false;

                if (currentUpdateInterval != updateInterval) {
                    updateInterval = Preferences.setUpdateInterval(MainActivity.this, updateInterval);
                    loadUpdateInterval();
                    updated = true;

                    Log.i(MainActivity.class, MainActivity.this, String.format(Locale.US, "Set update interval to %.2f hours", updateInterval), false);
                }

                if (currentLocalRadius != localRadius) {
                    localRadius = Preferences.setLocalRadius(MainActivity.this, localRadius);
                    loadLocalRadius();
                    updated = true;

                    Log.i(MainActivity.class, MainActivity.this, String.format(Locale.US, "Set local radius to %.2f km", localRadius), false);
                }

                if (updated) {
                    // Start or stop the check-in service
                    startStopCheckInService(token);
                }
            }
        });
    }

    private void loadUpdateInterval() {
        final EditText txtUpdateInterval = findViewById(R.id.txtUpdateInterval);
        txtUpdateInterval.setText(String.format(Locale.US, "%.2f", Preferences.getUpdateInterval(this)));
    }

    private void loadLocalRadius() {
        final EditText txtLocalRadius = findViewById(R.id.txtLocalRadius);
        txtLocalRadius.setText(String.format(Locale.US, "%.2f", Preferences.getLocalRadius(this)));
    }

    private void startStopCheckInService(String token) {
        // Start or stop the check-in service
        if (Preferences.areUpdatesPaused(this)) {
            CheckInService.get().stop(this);
        } else {
            CheckInService.get().start(this, token);
        }
    }

    private void updateActivityLog() {
        String activityLog = Log.getActivityLog(MainActivity.this);

        TextView tvActivity = findViewById(R.id.tvActivity);
        tvActivity.setText(activityLog);
    }

    private void setPauseResumeUpdatesButtonText(Button btnPauseResumeUpdates) {
        btnPauseResumeUpdates.setText(Preferences.areUpdatesPaused(this) ? R.string.resume_updates_button_text : R.string.pause_updates_button_text);
    }

    private void onCompleteConnect(int resultCode, Intent data) {
        AuthCodeResponse codeResponse = FoursquareOAuth.getAuthCodeFromResult(resultCode, data);
        Exception exception = codeResponse.getException();

        if (exception == null) {
            // Success.
            String code = codeResponse.getCode();
            performTokenExchange(code);

        } else {
            if (exception instanceof FoursquareCancelException) {
                // Canceled
                Log.e(MainActivity.class, this, "Authentication canceled", true, exception);
            } else if (exception instanceof FoursquareDenyException) {
                // Access denied
                Log.e(MainActivity.class, this, "Access denied", true, exception);
            } else if (exception instanceof FoursquareOAuthException) {
                // OAuth error
                Log.e(MainActivity.class, this, String.format("Authentication exception: %s, %s", ((FoursquareOAuthException) exception).getErrorCode(), exception.getMessage()), true, exception);
            } else if (exception instanceof FoursquareUnsupportedVersionException) {
                // Unsupported Fourquare app version on the device
                Log.e(MainActivity.class, this, exception.getMessage(), true, exception);
            } else if (exception instanceof FoursquareInvalidRequestException) {
                // Invalid request
                Log.e(MainActivity.class, this, exception.getMessage(), true, exception);
            } else {
                // Unknown error
                Log.e(MainActivity.class, this, exception.getMessage(), true, exception);
            }
        }
    }

    private void onCompleteTokenExchange(int resultCode, Intent data) {
        AccessTokenResponse tokenResponse = FoursquareOAuth.getTokenFromResult(resultCode, data);
        Exception exception = tokenResponse.getException();

        if (exception == null) {
            String accessToken = tokenResponse.getAccessToken();

            Log.i(MainActivity.class, this, "Foursquare access token obtained", true);

            // Persist the token for later use. In this example, we save it to shared prefs
            FileTokenStore.get(getApplicationContext()).setToken(accessToken);

            // Refresh UI
            ensureUi();

        } else {
            if (exception instanceof FoursquareOAuthException) {
                // OAuth error
                Log.e(MainActivity.class, this, String.format("Authentication exception: %s, %s", ((FoursquareOAuthException) exception).getErrorCode(), exception.getMessage()), true, exception);

            } else {
                // Other exception type
                Log.e(MainActivity.class, this, exception.getMessage(), true, exception);
            }
        }
    }

    /**
     * Exchange a code for an OAuth Token. Note that we do not recommend you
     * do this in your app, rather do the exchange on your server. Added here
     * for demo purposes.
     *
     * @param code The auth code returned from the native auth flow.
     */
    private void performTokenExchange(String code) {
        Intent intent = FoursquareOAuth.getTokenExchangeIntent(this, clientId, clientSecret, code);
        startActivityForResult(intent, REQUEST_CODE_FSQ_TOKEN_EXCHANGE);
    }

}
