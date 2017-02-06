package com.corebaseit.geolocation;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.corebaseit.geolocation.location.LocationManagerInterface;
import com.corebaseit.geolocation.location.LocationManager;
import com.google.android.gms.location.LocationRequest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Vincent Bevia on 2/02/2017.
 */

public class GeoLocation extends AppCompatActivity implements
        LocationManagerInterface {

    private static String latitudeForHeader;
    private static String longitudeForHeader;
    public LocationManager locationManager;
    private static final int REQUEST_FINE_LOCATION = 1;
    private Activity currentActivity;
    private TextView lat, lon;
    private TextView CityTextView2;
    private TextView CodeTextView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        lat = (TextView)findViewById(R.id.latTextView2);
        lon = (TextView)findViewById(R.id.lonTextView2);

        CityTextView2 = (TextView)findViewById(R.id.cityTextView2);
        CodeTextView2 = (TextView)findViewById(R.id.codeTextView2);
    }

    @Override
    public void locationFetched(Location mLocation, Location oldLocation, String time, String locationProvider) {
        // storing it on application level
        MainApplication.currentLocation = mLocation;
        MainApplication.oldLocation = oldLocation;
        MainApplication.locationProvider = locationProvider;
        MainApplication.locationTime = time;

        Log.d("LOCALIZATION", "Lat : " + mLocation.getLatitude() + " Lng : " + mLocation.getLongitude());
        latitudeForHeader =  String.valueOf(mLocation.getLatitude());
        longitudeForHeader =  String.valueOf(mLocation.getLongitude());

        lat.setText(Double.toString(mLocation.getLatitude()));
        lon.setText(Double.toString(mLocation.getLongitude()));

        Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> address = geocoder.getFromLocation( mLocation.getLatitude(), mLocation.getLongitude(), 1 );
            Log.d("LOCALIZATION", "address 1: " 
                    + address.get(0).getAddressLine(1).toString()
                    + " address 2 " 
                    + address.get(0).getAddressLine(2).toString());

            CityTextView2.setText(address.get(0).getAddressLine(1).toString());
            CodeTextView2.setText(address.get(0).getAddressLine(2).toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getLat() {

        return latitudeForHeader;
    }

    public static String getLong() {

        return longitudeForHeader;
    }

    public void initLocationFetching(Activity mActivity) {
        currentActivity = mActivity;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showLocationPermission();
        } else {
            locationManager = new LocationManager(getApplicationContext(),
                    mActivity,
                    this, LocationManager.ALL_PROVIDERS,
                    LocationRequest.PRIORITY_HIGH_ACCURACY,
                    10 * 1000, 1 * 1000,
                    LocationManager.LOCATION_PROVIDER_RESTRICTION_NONE); // init location manager
        }
    }

    protected void onStart() {
        super.onStart();
        if (locationManager != null)
            locationManager.startLocationFetching();
    }

    protected void onStop() {
        super.onStop();
        if (locationManager != null)
            locationManager.abortLocationFetching();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null)
            locationManager.pauseLocationFetching();
    }

    /**
     * PERMISSIONS HERE: ------> start...
     */
    private void showLocationPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(currentActivity, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(currentActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showExplanation("Permission Needed", "Rationale", Manifest.permission.READ_PHONE_STATE, REQUEST_FINE_LOCATION);
            } else {
                requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_FINE_LOCATION);
            }
        } else {
            Toast.makeText(currentActivity, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
            locationManager = new LocationManager(getApplicationContext(),
                    GeoLocation.this, this, LocationManager.ALL_PROVIDERS,
                    LocationRequest.PRIORITY_HIGH_ACCURACY, 10 * 1000, 1 * 1000,
                    LocationManager.LOCATION_PROVIDER_RESTRICTION_NONE); // init location manager
        }
    }

    private void showExplanation(String title, String message, final String permission, final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(currentActivity, new String[]{permissionName}, permissionRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationManager = new LocationManager(getApplicationContext(), GeoLocation.this,
                            this, LocationManager.ALL_PROVIDERS,
                            LocationRequest.PRIORITY_HIGH_ACCURACY,
                            10 * 1000, 1 * 1000, LocationManager.LOCATION_PROVIDER_RESTRICTION_NONE); // init location manager
                    locationManager.startLocationFetching();
                    Toast.makeText(GeoLocation.this, "Permission Granted!", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(GeoLocation.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    /**
     * PERMISSIONS HERE: ------> end...
     */


    /**
     * Service on/off listeners...
     * @param view
     */
    public void startService(View view) {
        initLocationFetching(this);
    }

    public void stopService(View view) {
        if (locationManager != null)
            locationManager.abortLocationFetching();

        Toast.makeText(currentActivity, "Tracking service stooped!", Toast.LENGTH_SHORT).show();

        Log.d("LOCALIZATION", "STOPPED");

    }

    public void startHeaders(View view) {
        Intent intent = new Intent(this, Headers.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (locationManager != null)
            locationManager.abortLocationFetching();

    }
}