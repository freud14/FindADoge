package org.findadoge.app;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;

import org.findadoge.app.model.User;

public class FindADogeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ParseObject.registerSubclass(User.class);
        Parse.initialize(this);
    }
}
