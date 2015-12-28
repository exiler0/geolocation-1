package de.freitag.stefan.android.geolocation;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
        this.mLocationRequest =  this.createLocationRequest();
    }

    private void bindTextViews() {
        this.viewLatitude = (TextView) findViewById(R.id.TextView_Latitude);
        this.viewLongitude = (TextView) findViewById(R.id.TextView_Longitude);
        this.viewProvider = (TextView) findViewById(R.id.TextView_Provider);
        this.viewAccuracy = (TextView) findViewById(R.id.TextView_Accuracy);
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

