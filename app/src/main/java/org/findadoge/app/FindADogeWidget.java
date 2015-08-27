package org.findadoge.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class FindADogeWidget extends AppWidgetProvider {
    private static final String TAG = "FindADogeWidget";

    private static final String ENABLE_BUTTON_CLICKED = "org.findadoge.app.ENABLE_BUTTON_CLICKED";

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        ILocationUpdaterService service = getService(context);
        String action = intent.getAction();
        if (ILocationUpdaterService.ENABLE_CHANGE_BROADCAST.equals(action)) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.find_adoge_widget);
            if (service != null && service.isEnabled()) {
                views.setImageViewResource(R.id.enable_button, R.mipmap.doge_icon_white);
            } else {
                views.setImageViewResource(R.id.enable_button, R.mipmap.doge_icon_grey);
            }

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetComponent = new ComponentName(context, FindADogeWidget.class);
            appWidgetManager.updateAppWidget(widgetComponent, views);
        } else if (ENABLE_BUTTON_CLICKED.equals(action)) {
            if (service != null && service.isEnabled()) {
                service.disableTracking();
            } else if (service != null) {
                service.enableTracking();
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(TAG, "onUpdate");
        ILocationUpdaterService service = getService(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.find_adoge_widget);
        views.setOnClickPendingIntent(R.id.enable_button, getPendingSelfIntent(context, ENABLE_BUTTON_CLICKED));
        if (service != null && service.isEnabled()) {
            views.setImageViewResource(R.id.enable_button, R.mipmap.doge_icon_white);
        } else {
            views.setImageViewResource(R.id.enable_button, R.mipmap.doge_icon_grey);
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    private PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private ILocationUpdaterService getService(Context context) {
        Intent intent = new Intent(context, LocationUpdaterService.class);
        ILocationUpdaterService.ILocationUpdaterBinder binder = (ILocationUpdaterService.ILocationUpdaterBinder) peekService(context, intent);
        ILocationUpdaterService service = null;
        if (binder != null) {
            service = binder.getService();
        }
        return service;
    }
}

