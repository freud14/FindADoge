package org.findadoge.app;

public interface ILocationUpdaterService {
    String POSITION_UPDATE_BROADCAST = "org.findadoge.app.POSITION_UPDATE";
    String POSITION_UPDATE_LOCATION_FIELD = "location";

    String ENABLE_CHANGE_BROADCAST = "org.findadoge.app.ENABLE_CHANGE";

    interface ILocationUpdaterBinder {
        ILocationUpdaterService getService();
    }

    void setFocus();
    void setUnfocus();

    void disableTracking();
    void enableTracking();
    boolean isEnabled();
}
