package org.findadoge.app;


import android.app.Dialog;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.androidmapsextensions.ClusteringSettings;
import com.androidmapsextensions.GoogleMap;
import com.androidmapsextensions.Marker;
import com.androidmapsextensions.MarkerOptions;
import com.androidmapsextensions.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.findadoge.app.ILocationUpdaterService.ILocationUpdaterBinder;
import org.findadoge.app.util.UIUpdater;
import org.findadoge.app.util.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private GoogleMap map;
    private Map<String, UserMarker> userMarkerMap = new HashMap<>();

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
        Log.v(TAG, "onCreate");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        map = mapFragment.getExtendedMap();

        Intent intent = new Intent(this, LocationUpdaterService.class);
        startService(intent);

        handleIntent(getIntent());

        enableTrackingDialog();
    }


    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }


    public static class EnableTrackingDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.enable_tracking_question)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ((MainActivity) getActivity()).setEnableTracking(true);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    }).create();
        }
    }

    private void enableTrackingDialog() {
        if (bound && !service.isEnabled() && !enablingAsked) {
            enablingAsked = true;
//            DialogFragment newFragment = new EnableTrackingDialog();
//            newFragment.show(getFragmentManager(), "dialog");
        } else if (bound && service.isEnabled()) {
            enablingAsked = true;
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

        setUpMap();
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

        if (enableItem != null) {
            setMapLocationEnable(enableItem.isChecked());
        }
    }

    private void setEnableTracking(boolean enable) {
        if (bound) {
            if (enable) {
                service.enableTracking();
            } else {
                service.disableTracking();
            }
        }
        setMapLocationEnable(enable);
    }

    private void setMapLocationEnable(boolean enable) {
        if (map != null) {
            map.setMyLocationEnabled(enable);
            map.getUiSettings().setMyLocationButtonEnabled(enable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            service.disableTracking();
            Util.logout(MainActivity.this);
            return true;
        } else if (id == R.id.action_enable) {
            if (bound) {
                item.setChecked(!item.isChecked());
            } else if (item.isChecked()) {
                item.setChecked(false);
            }
            setEnableTracking(item.isChecked());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setUpMap() {
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);

        ClusteringSettings clusteringSettings = new ClusteringSettings();
        clusteringSettings.addMarkersDynamically(true);
        map.setClustering(clusteringSettings);

        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                if (marker.isCluster()) {
                    List<Marker> markers = marker.getMarkers();
                    final List<UserMarker> userMarkers = new ArrayList<>();
                    for (Marker mark : markers) {
                        userMarkers.add((UserMarker) mark.getData());
                    }

                    final PopupWindow popupWindow = new PopupWindow(MainActivity.this);
                    ListView list = new ListView(MainActivity.this);
                    list.setBackgroundColor(Color.WHITE);
                    popupWindow.setContentView(list);
                    popupWindow.setFocusable(true);
                    list.setAdapter(new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_dropdown_item_1line, userMarkers));
                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            findUser(userMarkers.get(position).getTitle());
                            popupWindow.dismiss();
                        }
                    });
                    popupWindow.showAtLocation(findViewById(R.id.map), Gravity.CENTER, 0, 0);
                    popupWindow.update(0, 0, 600, 600);
                }

                return null;
            }
        });

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
            query.whereExists("currentPosition");
            query.findInBackground(new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> objects, ParseException e) {
                    if (e != null || objects == null) {
                        return;
                    }

                    Map<String, ParseUser> userMap = new HashMap<>();
                    for (ParseUser obj : objects) {
                        userMap.put(obj.getUsername(), obj);
                    }
                    List<String> keyToDelete = new ArrayList<>();
                    for (Map.Entry<String, UserMarker> entry : userMarkerMap.entrySet()) {
                        String username = entry.getKey();
                        ParseUser user = userMap.get(username);
                        UserMarker userMarker = entry.getValue();
                        if (user != null) {
                            userMarker.setUser(user);
                            Marker marker = userMarker.getMarker();
                            marker.setTitle(userMarker.getTitle());
                            marker.setPosition(userMarker.getPosition());
                            marker.setSnippet(userMarker.getSnippet());
                            userMap.remove(username);
                        } else {
                            userMarker.getMarker().remove();
                            keyToDelete.add(username);
                            userMarkerMap.remove(username);
                        }
                    }
                    for (String key : keyToDelete) {
                        userMarkerMap.remove(key);
                    }
                    for (ParseUser obj : userMap.values()) {
                        UserMarker userMarker = new UserMarker(obj);
                        userMarkerMap.put(obj.getUsername(), userMarker);

                        userMarker.setMarker(map.addMarker(new MarkerOptions()
                                .title(userMarker.getTitle())
                                .position(userMarker.getPosition())
                                .snippet(userMarker.getSnippet())));
                        userMarker.getMarker().setData(userMarker);
                    }
                    Marker m = map.getMarkerShowingInfoWindow();
                    if (m != null && !m.isCluster()) {
                        m.showInfoWindow();
                    }
                    Log.v(TAG, "map update: " + objects.size());
                }
            });
        }
    }

    public class UserMarker {
        private ParseUser user;
        private Marker marker;

        public UserMarker(ParseUser user) {
            this.user = user;
        }

        public void setUser(ParseUser user) {
            this.user = user;
        }

        public LatLng getPosition() {
            ParseGeoPoint point = user.getParseGeoPoint("currentPosition");
            return new LatLng(point.getLatitude(), point.getLongitude());
        }

        public String getTitle() {
            return user.getUsername();
        }

        public String getSnippet() {
            Date lastUpdateTime = user.getUpdatedAt();
            long timeDiff = System.currentTimeMillis() - lastUpdateTime.getTime();
            if (timeDiff < 60 * 1000) {
                return getString(R.string.now);
            } else {
                return DateUtils.getRelativeDateTimeString(MainActivity.this,
                        lastUpdateTime.getTime(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.WEEK_IN_MILLIS, 0).toString();
            }
        }

        public Marker getMarker() {
            return marker;
        }

        public void setMarker(Marker marker) {
            this.marker = marker;
        }

        public String toString() {
            return getTitle();
        }
    }
}
