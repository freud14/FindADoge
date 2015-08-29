package org.findadoge.app.model;

import android.content.Context;
import android.text.format.DateUtils;

import com.androidmapsextensions.Marker;
import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.findadoge.app.R;

import java.util.Date;

@ParseClassName("_User")
public class User extends ParseUser {
    public static final String POSITION_FIELD = "currentPosition";

    private Marker marker;

    public ParseGeoPoint getParsePosition() {
        return getParseGeoPoint(POSITION_FIELD);
    }

    public LatLng getPosition() {
        ParseGeoPoint point = getParsePosition();
        LatLng latLng = null;
        if (point != null) {
            latLng = new LatLng(point.getLatitude(), point.getLongitude());
        }
        return latLng;
    }

    public String getLastUpdateString(Context context) {
        Date lastUpdateTime = getUpdatedAt();
        long timeDiff = System.currentTimeMillis() - lastUpdateTime.getTime();
        if (timeDiff < 60 * 1000) {
            return context.getString(R.string.now);
        } else {
            return DateUtils.getRelativeDateTimeString(context,
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
        return getUsername();
    }

    public static ParseQuery<User> getUserQuery() {
        return ParseQuery.getQuery(User.class);
    }
}
