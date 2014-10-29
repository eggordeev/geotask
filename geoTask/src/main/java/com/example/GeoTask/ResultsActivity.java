package com.example.GeoTask;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;

/**
 * Created by egordeev on 10.09.14.
 */
public class ResultsActivity extends ActionBarActivity {


    private GoogleMap map;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        TextView textView_find_direction_result = (TextView)findViewById(R.id.textView_find_path_result);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        String status = intent.getStringExtra("status");
        textView_find_direction_result.setText(status);
        if (status.equals("Found")){
            ArrayList<Address> coordinates = intent.getParcelableArrayListExtra("Coordinates");
            ArrayList<LatLng> coordinates_ = new ArrayList<LatLng>();
            for (Address item:coordinates){
                coordinates_.add(new LatLng(item.getLatitude(),item.getLongitude()));
            }

            map = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map_path)).getMap();
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            LatLng start_point = coordinates_.get(0);
            Marker start_pos= map.addMarker(new MarkerOptions().position(start_point).title(intent.getStringExtra("start_place_name")));
            start_pos.showInfoWindow();
            CameraPosition cameraPosition = new CameraPosition.Builder().target(start_point).zoom(4).build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            Polyline line = map.addPolyline(new PolylineOptions().
                    addAll(coordinates_)
                    .width(5).color(Color.BLACK));
        }

    }
}