package de.freitag.stefan.android.geolocation;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import de.freitag.stefan.android.R;

public final class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * Tag used for logging.
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Provides the entry point to Google Play services.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    /**
     * For displaying information about the latitude.
     */
    private TextView viewLatitude;
    /**
     * For displaying information about the longitude.
     */
    private TextView viewLongitude;
    /**
     * For displaying information about the location information providers.
     */
    private TextView viewProvider;
    /**
     * For displaying information about the accuracy in meters.
     * (a 68% probability that the true location is inside the circle).
     */
    private TextView viewAccuracy;

    /**
     * For displaying the timestamp of the latest location update.
     */
    private TextView viewLatestUpdate;

    private TextView viewActivityType;

    private TextView viewActivityConfidence;

    private Listener locListener;

    private BroadcastReceiver receiver;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.locListener = new Listener();
        this.setContentView(R.layout.main);
        this.bindTextViews();
        this.buildGoogleApiClient();

        this.mLocationRequest = this.createLocationRequest();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates states = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    CommonStatusCodes.RESOLUTION_REQUIRED);
                        } catch (final IntentSender.SendIntentException exception) {
                            Log.e(TAG, exception.getMessage(), exception);
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });

        //Broadcast receiver
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Add current time
                int confidence = intent.getIntExtra("confidence", -1);
                Log.d(TAG, "Confidence: " + confidence);
                //TODO: Display NA if confidence is unknown.
                viewActivityConfidence.setText(String.valueOf(confidence));
                viewActivityType.setText(intent.getStringExtra("activity"));

            }
        };
        //Filter the Intent and register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("ActivityRecognition");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case CommonStatusCodes.RESOLUTION_REQUIRED:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "Result ok");
                        // All required changes were successfully made
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "Result canceled.");
                        // The user was asked to change settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    /**
     * Bind the resources to local fields.
     */
    private void bindTextViews() {
        this.viewLatitude = (TextView) findViewById(R.id.TextView_Latitude);
        assert this.viewLatitude != null;
        this.viewLongitude = (TextView) findViewById(R.id.TextView_Longitude);
        assert this.viewLongitude != null;
        this.viewProvider = (TextView) findViewById(R.id.TextView_Provider);
        assert this.viewProvider != null;
        this.viewAccuracy = (TextView) findViewById(R.id.TextView_Accuracy);
        assert this.viewAccuracy != null;
        this.viewLatestUpdate = (TextView) findViewById(R.id.TextView_LatestUpdate);
        assert this.viewLatestUpdate != null;
        this.viewActivityType = (TextView) findViewById(R.id.TextView_ActivityType);
        assert this.viewActivityType != null;
        this.viewActivityConfidence = (TextView) findViewById(R.id.TextView_ActivityConfidence);
        assert this.viewActivityConfidence != null;
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    private synchronized void buildGoogleApiClient() {
        this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }


    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                this.mGoogleApiClient, this.mLocationRequest, this.locListener);
    }


    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(this.mGoogleApiClient, this.locListener);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (this.mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        this.mGoogleApiClient.disconnect();
        unregisterReceiver(receiver);
        super.onStop();
    }


    /**
     * Sets up the location request.
     */
    protected LocationRequest createLocationRequest() {

         //The desired interval for location updates
        final long UPDATE_INTERVAL_IN_MILLISECONDS = 5_000;

         //The fastest rate for active location updates.
        final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
                UPDATE_INTERVAL_IN_MILLISECONDS / 2;

        final LocationRequest request = new LocationRequest();
        request.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        request.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return request;
    }


    @Override
    protected void onStart() {
        super.onStart();
        this.mGoogleApiClient.connect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        this.startLocationUpdates();

        Intent i = new Intent(this, ActivityRecognitionIntentService.class);
        PendingIntent mActivityRecongPendingIntent = PendingIntent
                .getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Log.d(TAG, "connected to ActivityRecognition");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, mActivityRecongPendingIntent);

    }

    @Override
    public void onConnectionSuspended(final int cause) {
        this.mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult result) {
        Log.w(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    /**
     * Update the view components.
     *
     * @param longitude
     * @param latitude
     * @param accuracy
     * @param provider
     */
    private void updateView(final String longitude, final String latitude, final String accuracy, final String provider) {
        assert longitude != null;
        assert latitude != null;
        assert accuracy != null;
        assert provider != null;
        this.viewLatitude.setText(latitude);
        this.viewLongitude.setText(longitude);
        this.viewAccuracy.setText(accuracy);
        this.viewProvider.setText(provider);

        final DateFormat dateTimeInstance = DateFormat.getDateTimeInstance();
        this.viewLatestUpdate.setText(dateTimeInstance.format(new Date()));

    }

    private class Listener implements  LocationListener{

        @Override
        public void onLocationChanged(final Location location) {
            final String longitude;
            final String latitude;
            final String accuracy;
            final String provider;

            if (location==null) {
                latitude = getResources().getText(R.string.not_available).toString();
                longitude = getResources().getText(R.string.not_available).toString();
                accuracy = getResources().getText(R.string.not_available).toString();
                provider = getResources().getText(R.string.not_available).toString();
            } else {
                final double dLongitude = location.getLongitude();
                longitude = String.valueOf(dLongitude);
                final double dLatitude = location.getLatitude();
                latitude = String.valueOf(dLatitude);
                final float fAccuracy = location.getAccuracy();
                accuracy = String.valueOf(fAccuracy);
                provider = location.getProvider();

            }
            MainActivity.this.mCurrentLocation = location;
            updateView(longitude, latitude,String.valueOf(accuracy),provider);
            Toast.makeText(MainActivity.this, getResources().getString(R.string.location_updated_message),
                    Toast.LENGTH_SHORT).show();
        }
    }
}

