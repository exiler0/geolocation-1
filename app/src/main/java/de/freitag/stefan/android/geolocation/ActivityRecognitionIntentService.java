package de.freitag.stefan.android.geolocation;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Processes received intents and forwards information about the detected activities via broadcast.
 */
public final class ActivityRecognitionIntentService extends IntentService {

    /**
     * The log tag.
     */
    private static final String TAG = ActivityRecognitionIntentService.class.getSimpleName();

    /**
     * Create a new {@link ActivityRecognitionIntentService}.
     */
    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            final ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            final DetectedActivity detectedActivity = result.getMostProbableActivity();
            final int confidence = detectedActivity.getConfidence();
            final String mostProbableName = getActivityName(detectedActivity.getType());
            final Intent i = new Intent("ActivityRecognition");
            i.putExtra("activity", mostProbableName);
            i.putExtra("confidence", confidence);
            this.sendBroadcast(i);
            return;
        }
        Log.d(TAG, "Intent had no data returned");
    }

    /**
     * Maps the detected activity to a string.
     *
     * @param type An integer representing the detected activity.
     * @return A localized string for the activity.
     * @throws IllegalArgumentException if {@code type} contains an unsupported value.
     */
    private String getActivityName(final int type) {
        switch (type) {
            case DetectedActivity.IN_VEHICLE:
                return "In Vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "On Bicycle";
            case DetectedActivity.ON_FOOT:
                return "On Foot";
            case DetectedActivity.WALKING:
                return "Walking";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.RUNNING:
                return "Running";
            case DetectedActivity.UNKNOWN:
                return "Unknown";
        }
        throw new IllegalArgumentException("Type with value " + type + " is not supported.");
    }
}