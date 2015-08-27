package org.findadoge.app.util;

import android.content.Context;
import android.content.Intent;

import com.parse.ParseUser;

import org.findadoge.app.DispatchActivity;


public class Util {
    public static void logout(Context packageContext) {
        ParseUser.logOut();

        Intent intent = new Intent(packageContext, DispatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        packageContext.startActivity(intent);
    }
}
