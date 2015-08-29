package org.findadoge.app.util;

import android.content.Context;
import android.util.Log;

import com.androidmapsextensions.GoogleMap;
import com.androidmapsextensions.Marker;
import com.androidmapsextensions.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.findadoge.app.R;
import org.findadoge.app.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapUtil {
    private static final String TAG = "MapUtil";

    public static void updateMap(List<User> users,
                                 Map<String, Marker> userMarkerMap,
                                 GoogleMap map,
                                 Context context) {
        Map<String, User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getUsername(), user);
        }


        List<String> keyToDelete = new ArrayList<>();
        for (Map.Entry<String, Marker> entry : userMarkerMap.entrySet()) {
            String username = entry.getKey();
            User user = userMap.get(username);
            Marker marker = entry.getValue();
            if (user != null) {
                user.setMarker(marker);
                marker.setData(user);

                marker.setTitle(user.getUsername());
                marker.setPosition(user.getPosition());
                marker.setSnippet(user.getLastUpdateString(context));
                userMap.remove(username);
            } else {
                marker.remove();
                keyToDelete.add(username);
            }
        }
        for (String key : keyToDelete) {
            userMarkerMap.remove(key);
        }


        for (User user : userMap.values()) {
            Marker marker = map.addMarker(new MarkerOptions()
                    .title(user.getUsername())
                    .position(user.getPosition())
                    .snippet(user.getLastUpdateString(context))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.doge_icon)));
            userMarkerMap.put(user.getUsername(), marker);

            user.setMarker(marker);
            user.getMarker().setData(user);
        }


        Marker m = map.getMarkerShowingInfoWindow();
        if (m != null && !m.isCluster()) {
            m.showInfoWindow();
        }
        Log.v(TAG, "map update: " + users.size());
    }
}
