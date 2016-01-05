package de.freitag.stefan.android.geolocation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import de.freitag.stefan.android.R;

public final class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    /**
     * Tag used for logging.
     */
    private static final String TAG = "geolocation";

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



    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                        //...
                        Log.i(TAG, "XXXXXXXXXXXXXXx");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        Log.i(TAG, "ZZZZZZZZZZZZZZZZZZZ");


                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    CommonStatusCodes.RESOLUTION_REQUIRED);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        Log.i(TAG, "YYYYYYYYYYYYYYYYYYYY");
                        break;
                }
            }
        });


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
        //                ...
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "Result canceled.");
                        // The user was asked to change settings, but chose not to
          //              ...
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
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    private synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                this.mGoogleApiClient, this.mLocationRequest, this);
    }


    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(this.mGoogleApiClient, this);
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
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (this.mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        this.mGoogleApiClient.disconnect();
        super.onStop();

    }


    /**
     * Sets up the location request.
     */
    protected LocationRequest createLocationRequest() {

        /**
         * The desired interval for location updates
         */
        final long UPDATE_INTERVAL_IN_MILLISECONDS = 5_000;

        /**
         * The fastest rate for active location updates.
         */
        final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
                UPDATE_INTERVAL_IN_MILLISECONDS / 2;

        final LocationRequest request = new LocationRequest();
        request.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        request.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return request;
    }

    @Override
    public void onLocationChanged(final Location location) {
        this.mCurrentLocation = location;

        final double lat = location.getLatitude();
        final double lng = location.getLongitude();

        this.viewLatitude.setText(String.valueOf(lat));
        this.viewLongitude.setText(String.valueOf(lng));
        this.viewAccuracy.setText(String.valueOf(location.getAccuracy()));
        this.viewProvider.setText(location.getProvider());

        Toast.makeText(this, getResources().getString(R.string.location_updated_message),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        this.startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(final int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        this.mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


}

