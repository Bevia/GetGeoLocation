package com.corebaseit.geolocation.location;

import android.location.Location;

/**
 * Created by Vincent Bevia on 2/02/2017.
 */
public interface LocationManagerInterface {
    void locationFetched(Location mLocation, Location oldLocation, String time, String locationProvider);

}
