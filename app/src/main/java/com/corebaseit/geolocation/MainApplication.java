package com.corebaseit.geolocation;

import android.app.Application;
import android.content.Context;
import android.location.Location;

/**
 * Created by Vincent Bevia on 2/02/2017.
 */

public class MainApplication extends Application {

    public static Location currentLocation;
    public static String locationProvider;
    public static Location oldLocation;
    public static String locationTime;

    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        initApplication();
    }

    private void initApplication() {
        context = getApplicationContext();
    }
}
