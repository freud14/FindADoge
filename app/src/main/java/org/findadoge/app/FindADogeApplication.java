package org.findadoge.app;

import android.app.Application;

import com.parse.Parse;

public class FindADogeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //ParseObject.registerSubclass(Armor.class);
        Parse.initialize(this);
    }
}
