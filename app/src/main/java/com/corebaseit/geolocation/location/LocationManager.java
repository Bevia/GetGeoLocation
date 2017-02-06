package com.corebaseit.geolocation.location;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

/**
 * Modified by Vincent Bevia on 2/02/2017.
 */
public class LocationManager implements
        GoogleApiClient.ConnectionCallbacks, 
        GoogleApiClient.OnConnectionFailedListener, 
        LocationListener {

    private static final String TAG = LocationManager.class.getSimpleName();

    //Get a reference of the interface that is implemented by the Geolocation Class.
    private LocationManagerInterface locationManagerInterface;

    private Context context;
    private Activity activity;

    private static final int X_MINUTES = 1000 * 60 * 2; //two minutes is this case!
    private static final int CONNECTION_FAILURE_TIMING_REQUEST = 9000;
    private String lastLocationUpdateTime;
    private String locationProvider;
    private Location lastLocationFetched;
    private Location locationFetched;
    private int locationPiority;
    private long locationFetchInterval;
    private long fastestLocationFetchInterval;
  
    private LocationRequest locationRequest;
    private GoogleApiClient mGoogleApiClient;
    private android.location.LocationManager locationManager;
    private android.location.LocationListener locationListener;
    boolean isGPSEnabled;
    boolean isNetworkEnabled;
    private int providerType;
    
    public static final int NETWORK_PROVIDER = 1;
    public static final int ALL_PROVIDERS = 0;
    public static final int GPS_PROVIDER = 2;
    public static final int LOCATION_PROVIDER_ALL_RESTICTION = 1;
    public static final int LOCATION_PROVIDER_RESTRICTION_NONE = 0;
    public static final int LOCATION_PROVIDER_GPS_ONLY_RESTICTION = 2;
    public static final int LOCATION_PROVIDER_NETWORK_ONLY_RESTICTION = 3;
    private int forceNetworkProviders = 0;

    public LocationManager(Context context,
                           Activity activity,
                           LocationManagerInterface locationInterface,
                           int providerType,
                           int locationPiority,
                           long locationFetchInterval,
                           long fastestLocationFetchInterval,
                           int forceNetworkProviders) {
        
        this.context = context;
        this.activity = activity;
        this.providerType = providerType;
        this.locationPiority = locationPiority;
        this.forceNetworkProviders = forceNetworkProviders;
        this.locationFetchInterval = locationFetchInterval;
        this.fastestLocationFetchInterval = fastestLocationFetchInterval;

        locationManagerInterface = locationInterface;

        initSmartLocationManager();
    }


    public void initSmartLocationManager() {

        /**
         1) ask for permission for Android 6 above to avoid crash
         2) check if gps is available
         3) get location using awesome strategy
         */

        checkNetworkProviderEnable();

        //init obj for google play service and start fetching location
        if (isGooglePlayServicesAvailable())
            initLocationObjts();

        //otherwise get location using Android API
        else
            getLocationUsingAndroidAPI();
    }

    private void initLocationObjts() {
        // Create the LocationRequest object
        locationRequest = LocationRequest.create()
                .setPriority(locationPiority)
                //set some time intervals...
                .setInterval(locationFetchInterval)
                .setFastestInterval(fastestLocationFetchInterval);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        //connect google play services to fetch location
        startLocationFetching(); 

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
            getLocationUsingAndroidAPI();
        } else {
            setNewLocation(getBetterLocation(location, locationFetched), locationFetched);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            getLastKnownLocation();
        } else {
            setNewLocation(getBetterLocation(location, locationFetched), locationFetched);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(activity, CONNECTION_FAILURE_TIMING_REQUEST); 
                // Start an Activity that tries to resolve the error
                getLocationUsingAndroidAPI();                                                                
                // try to get location using Android API locationManager
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    private void setNewLocation(Location location, Location oldLocation) {
        if (location != null) {
            lastLocationFetched = oldLocation;
            locationFetched = location;
            lastLocationUpdateTime = DateFormat.getTimeInstance().format(new Date());
            locationProvider = location.getProvider();
            locationManagerInterface.locationFetched(location, lastLocationFetched, lastLocationUpdateTime, location.getProvider());
        }
    }

    private void getLocationUsingAndroidAPI() {
        // Acquire a reference to the system Location Manager
        locationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        setLocationListner();
        captureLocation();
    }

    public void captureLocation() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                        != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) 
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            if (providerType == LocationManager.GPS_PROVIDER) {
                locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else if (providerType == LocationManager.NETWORK_PROVIDER) {
                locationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            } else {
                locationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void setLocationListner() {
        // Define a listener that responds to location updates
        locationListener = new android.location.LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                if (location == null) {
                    getLastKnownLocation();
                } else {
                    setNewLocation(getBetterLocation(location, locationFetched), locationFetched);
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    public void startLocationFetching() {
        mGoogleApiClient.connect();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    public void pauseLocationFetching() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    public void abortLocationFetching() {
        mGoogleApiClient.disconnect();

        // Remove the listener you previously added
        if (locationManager != null && locationListener != null) {
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            try {
                locationManager.removeUpdates(locationListener);
                locationManager = null;
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
    }

    public void checkNetworkProviderEnable() {
        locationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        isGPSEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);

        // getting network status
        if (!isGPSEnabled && !isNetworkEnabled && forceNetworkProviders == LOCATION_PROVIDER_ALL_RESTICTION) {
            Toast.makeText(context, "Location can't be fetched! Enable your location providers and relaunch the application.",
                    Toast.LENGTH_SHORT).show(); // show alert
            activity.finish();
        } else if (!isGPSEnabled && !isNetworkEnabled) {
            buildAlertMessageTurnOnLocationProviders("Your location providers seems to be disabled, please enable it!", "OK", "Cancel");
        } else if (!isGPSEnabled && forceNetworkProviders == LOCATION_PROVIDER_GPS_ONLY_RESTICTION) {
            buildAlertMessageTurnOnLocationProviders("Your GPS seems to be disabled, please enable it!", "OK", "Cancel");
        } else if (!isNetworkEnabled && forceNetworkProviders == LOCATION_PROVIDER_NETWORK_ONLY_RESTICTION) {
            buildAlertMessageTurnOnLocationProviders("Your Network location provider seems to be disabled, please enable it!", "OK", "Cancel");
        }

    }

    private void buildAlertMessageTurnOnLocationProviders(String message, String positiveButtonText, String negativeButtonText) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog,
                                            @SuppressWarnings("unused") final int id) {
                            Intent mIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(mIntent);
                        }
                    })
                    .setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            activity.finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }


    public Location getLastKnownLocation() {
        locationProvider = android.location.LocationManager.NETWORK_PROVIDER;
        Location lastKnownLocation = null;
        // Or use LocationManager.GPS_PROVIDER
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return lastKnownLocation;
        }
        try {
            lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            return lastKnownLocation;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return lastKnownLocation;
    }

    public boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        if (status == ConnectionResult.SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected Location getBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return location;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > X_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -X_MINUTES;
        boolean isNewer = timeDelta > 0;

        // search for new location every x minutes to keep tracking current to user's movement!
        if (isSignificantlyNewer) {
            return location;
        // If the new location is more than x minutes older, we don't need it any more!
        } else if (isSignificantlyOlder) {
            return currentBestLocation;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return location;
        } else if (isNewer && !isLessAccurate) {
            return location;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return location;
        }
        return currentBestLocation;
    }

    /**
     * Checks whether two providers are the same
     */

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}