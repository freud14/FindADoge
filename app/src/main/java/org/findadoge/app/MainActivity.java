package org.findadoge.app;


import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.findadoge.app.ILocationUpdaterService.ILocationUpdaterBinder;
import org.findadoge.app.util.UIUpdater;
import org.findadoge.app.util.Util;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    private static final String TAG = "MainActivity";

    private GoogleMap map;
    private ClusterManager<UserMarker> userClusterManager;

    private boolean isInitializedCameraPosition = false;
    private Location lastLocation;

    private ILocationUpdaterService service;
    private boolean bound = false;

    private MenuItem enableItem;
    private boolean enablingAsked = false;

    private UIUpdater mapUpdateScheduler = new UIUpdater(new Runnable() {
        @Override
        public void run() {
            updateMap();
        }
    }, 5000);

    private BroadcastReceiver locationUpdaterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ILocationUpdaterService.POSITION_UPDATE_BROADCAST.equals(action)) {
                MainActivity.this.onLocationUpdate((Location) intent.getParcelableExtra(ILocationUpdaterService.POSITION_UPDATE_LOCATION_FIELD));
            } else if (ILocationUpdaterService.ENABLE_CHANGE_BROADCAST.equals(action)) {
                setEnableButtonStatus();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v(TAG, "there");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent intent = new Intent(this, LocationUpdaterService.class);
        startService(intent);

        handleIntent(getIntent());

        enableTrackingDialog();
    }

    private void enableTrackingDialog() {
        if (bound && !service.isEnabled() && !enablingAsked) {
            enablingAsked = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.enable_tracking_question)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (bound) {
                                service.enableTracking();
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            findUser(query);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String query = intent.getDataString();
            findUser(query);
        }
    }

    private void findUser(String usernameQuery) {
        ParseUser.getQuery().whereEqualTo("username", usernameQuery).getFirstInBackground(new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser object, ParseException e) {
                if (e != null) {
                    Toast.makeText(MainActivity.this, R.string.search_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                ParseGeoPoint point = object.getParseGeoPoint("currentPosition");
                if (point == null) {
                    Toast.makeText(MainActivity.this, R.string.search_no_location_found, Toast.LENGTH_SHORT).show();
                    return;
                }

                LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());

                map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                map.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, LocationUpdaterService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bound) {
            service.setFocus();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ILocationUpdaterService.POSITION_UPDATE_BROADCAST);
        filter.addAction(ILocationUpdaterService.ENABLE_CHANGE_BROADCAST);
        registerReceiver(locationUpdaterReceiver, filter);

        mapUpdateScheduler.startUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bound) {
            service.setUnfocus();
        }

        unregisterReceiver(locationUpdaterReceiver);

        mapUpdateScheduler.stopUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ILocationUpdaterBinder binder = (ILocationUpdaterBinder) service;
            MainActivity.this.service = binder.getService();
            MainActivity.this.service.setFocus();
            bound = true;

            setEnableButtonStatus();
            enableTrackingDialog();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            MainActivity.this.service.setUnfocus();
            bound = false;

            setEnableButtonStatus();
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        enableItem = menu.findItem(R.id.action_enable);
        setEnableButtonStatus();

        return true;
    }

    private void setEnableButtonStatus() {
        if (bound && enableItem != null) {
            enableItem.setChecked(MainActivity.this.service.isEnabled());
        } else if (enableItem != null) {
            enableItem.setChecked(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_logout) {
            service.disableTracking();
            Util.logout(MainActivity.this);
            return true;
        } else if (id == R.id.action_enable) {
            if (bound) {
                item.setChecked(!item.isChecked());
                if (item.isChecked()) {
                    service.enableTracking();
                } else {
                    service.disableTracking();
                }
            } else if (item.isChecked()) {
                item.setChecked(false);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap gmap) {
        map = gmap;

        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);

        userClusterManager = new ClusterManager<>(this, map);
        userClusterManager.setRenderer(new UserRenderer(this, map, userClusterManager));

        map.setOnCameraChangeListener(userClusterManager);
        map.setOnMarkerClickListener(userClusterManager);

        updateMap();
    }

    private void setCameraOnCurrentPosition() {
        double latitude = lastLocation.getLatitude();
        double longitude = lastLocation.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);

        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        map.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    private void onLocationUpdate(Location location) {
        lastLocation = location;
        if (!isInitializedCameraPosition && map != null) {
            setCameraOnCurrentPosition();
            isInitializedCameraPosition = true;
        }

        Log.v(TAG, "new position: " + lastLocation);
    }

    private void updateMap() {
        if (map != null) {
            ParseQuery<ParseUser> query = ParseUser.getQuery();

            query.whereNotEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> objects, ParseException e) {
                    if (e != null || objects == null) {
                        return;
                    }

                    userClusterManager.clearItems();
                    for (ParseUser obj : objects) {
                        if (obj.getParseGeoPoint("currentPosition") != null) {
                            userClusterManager.addItem(new UserMarker(obj));
                        }
                    }
                    userClusterManager.cluster();
                    Log.v(TAG, "map update: " + objects.size());
                }
            });
        }
    }

    public static class UserMarker implements ClusterItem {
        private final ParseUser user;

        public UserMarker(ParseUser user) {
            this.user = user;
        }

        @Override
        public LatLng getPosition() {
            ParseGeoPoint point = user.getParseGeoPoint("currentPosition");
            return new LatLng(point.getLatitude(), point.getLongitude());
        }

        public String getTitle() {
            return user.getUsername();
        }
    }

    public static class UserRenderer extends DefaultClusterRenderer<UserMarker> {

        public UserRenderer(Context context, GoogleMap map, ClusterManager<UserMarker> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(UserMarker userMarker, MarkerOptions markerOptions) {
            markerOptions.title(userMarker.getTitle());
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            return cluster.getSize() > 1;
        }
    }
}
