package org.findadoge.app;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

public class LocationUpdaterService extends Service
        implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener,
        ILocationUpdaterService {
    public static final String TAG = "LocationUpdaterService";

    public static final long UPDATE_INTERVAL_WHEN_FOCUSED_IN_MILLISECONDS = 10 * 1000; // 10 secondes
    public static final long UPDATE_INTERVAL_WHEN_UNFOCUSED_IN_MILLISECONDS = 10 * 60 * 1000; // 10 minutes

    private GoogleApiClient googleApiClient;
    private Location currentLocation;
    private boolean isFocused = false;
    private boolean isEnable = false;

    private final IBinder locationUpdaterBinder = new LocationUpdaterBinder();

    public LocationUpdaterService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    public void onDestroy() {
        disableTracking();
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (currentLocation == null) {
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }

        this.updateStatus();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;

        Intent intent = new Intent(ILocationUpdaterService.POSITION_UPDATE_BROADCAST);
        intent.putExtra(ILocationUpdaterService.POSITION_UPDATE_LOCATION_FIELD, location);
        sendBroadcast(intent);

        ParseUser currentUser = ParseUser.getCurrentUser();
        ParseGeoPoint parseLocation = new ParseGeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
        currentUser.put("currentPosition", parseLocation);
        currentUser.saveInBackground();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        this.stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return locationUpdaterBinder;
    }

    @Override
    public void setFocus() {
        isFocused = true;
        if (googleApiClient != null && googleApiClient.isConnected()) {
            updateStatus();
        }
    }

    @Override
    public void setUnfocus() {
        isFocused = false;
        if (googleApiClient != null && googleApiClient.isConnected()) {
            updateStatus();
        }
    }

    @Override
    public void enableTracking() {
        changeEnabling(true);
    }

    @Override
    public void disableTracking() {
        changeEnabling(false);
    }

    private void changeEnabling(boolean enable) {
        Log.v(TAG, "enable = " + enable);
        isEnable = enable;
        updateStatus();

        Intent intent = new Intent(ILocationUpdaterService.ENABLE_CHANGE_BROADCAST);
        sendBroadcast(intent);
    }

    @Override
    public boolean isEnabled() {
        return isEnable;
    }

    private void updateStatus() {
        if (googleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);

            if (isEnable) {
                LocationRequest locationRequest = new LocationRequest();
                if (isFocused) {
                    locationRequest.setInterval(UPDATE_INTERVAL_WHEN_FOCUSED_IN_MILLISECONDS);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    //locationRequest.setSmallestDisplacement(10);
                } else {
                    locationRequest.setInterval(UPDATE_INTERVAL_WHEN_UNFOCUSED_IN_MILLISECONDS);
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    //locationRequest.setSmallestDisplacement(10);
                }
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            }
        }
    }

    public class LocationUpdaterBinder extends Binder implements ILocationUpdaterBinder {
        public ILocationUpdaterService getService() {
            return LocationUpdaterService.this;
        }
    }
}
