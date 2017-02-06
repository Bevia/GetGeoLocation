package com.corebaseit.geolocation;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class Headers extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_headers);

        TextView textView = (TextView)findViewById(R.id.textView);

        String lat_long = String.format("%s, %s", GeoLocation.getLat(),  GeoLocation.getLong());
        textView.setText(lat_long);

    }
}
