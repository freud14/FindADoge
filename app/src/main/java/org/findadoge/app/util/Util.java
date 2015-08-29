package org.findadoge.app.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

import org.findadoge.app.DispatchActivity;
import org.findadoge.app.R;


public class Util {
    public static void logout(final Context packageContext) {
        final ProgressDialog dialog = new ProgressDialog(packageContext);
        dialog.setMessage(packageContext.getString(R.string.progress_logout));
        dialog.show();

        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                dialog.dismiss();

                Intent intent = new Intent(packageContext, DispatchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                packageContext.startActivity(intent);
            }
        });
    }
}
