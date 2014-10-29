package com.example.GeoTask;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.example.GeoTask.asynctasks.LoadDirectionFromGoogleDirectionAsyncTask;
import com.example.GeoTask.asynctasks.LoadPlaceFromGoogleGeocoderAsyncTask;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by egordeev on 10.09.14.
 */
public class PlaceActivity extends ActionBarActivity {

    //private GoogleMap mFromMap;
    private GoogleMap mToMap;

    private String from;
    private String to;
    private String start_place_name;

    private ArrayAdapter<String> adapter_origin_places;
    private ArrayAdapter<String> adapter_dest_places;
    private List<Address> addresses_origin = null;
    private List<Address> addresses_dest = null;

    private static final int geocoder_max_results = 7;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place);

        // подключение карты
        //mFromMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_origin)).getMap();
        mToMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_dest)).getMap();
        if (/*mFromMap != null &&*/ mToMap != null) {
            //mFromMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mToMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        // список маркеров на случай удаления из карты на вкладке "Откуда"
        final List<Marker> marker_origin_map = new ArrayList<Marker>();
        // список маркеров на случай удаления из карты на вкладке "Куда"
        final List<Marker> marker_dest_map = new ArrayList<Marker>();

        // поле ввода места "Откуда"
        final EditText editText_type_origin_place_name = (EditText)findViewById(R.id.editText_type_origin_place_name);
        // список мест "Откуда"
        ListView listView_found_origin_places = (ListView)findViewById(R.id.listView_found_origin_places);
        // кнопка "Найти" на вкладке "Откуда"
        Button button_find_origin_place = (Button) findViewById(R.id.button_origin_place_find);
        // адаптер для listView_found_origin_places
        adapter_origin_places = new ArrayAdapter<String>(getApplicationContext(),
                R.layout.list_view_item);
        listView_found_origin_places.setAdapter(adapter_origin_places);

        // поле ввода места "Куда"
        final EditText editText_type_dest_place_name = (EditText)findViewById(R.id.editText_type_dest_place_name);
        // список мест "Куда"
        ListView listView_found_dest_places = (ListView)findViewById(R.id.listView_found_dest_places);
        // кнопка "Найти" на вкладке "Куда"
        Button button_find_dest_place = (Button) findViewById(R.id.button_dest_place_find);
        // адаптер для listView_found_dest_places
        adapter_dest_places = new ArrayAdapter<String>(getApplicationContext(),
                R.layout.list_view_item);
        listView_found_dest_places.setAdapter(adapter_dest_places);

        // инициализируем вкладки
        TabHost tabs = (TabHost) findViewById(R.id.tabHost);
        tabs.setup();

        TabHost.TabSpec spec = tabs.newTabSpec("tag1");

        spec.setContent(R.id.tab_origin);
        spec.setIndicator("Откуда");
        tabs.addTab(spec);

        spec = tabs.newTabSpec("tag2");
        spec.setContent(R.id.tab_dest);
        spec.setIndicator("Куда");
        tabs.addTab(spec);

        tabs.setCurrentTab(0);

        //----------------------------------во вкладке "Откуда"----------------------------------
        button_find_origin_place.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // нужно запросить у службы google directions место, которое ввел пользователь
                // и обратиться в отдельном потоке
                String place = editText_type_origin_place_name.getText().toString();
                /*new LoadPlaceFromGoogleGeocoderAsyncTask(place,
                        adapter_origin_places).execute();*/

                Geocoder geocoder = new Geocoder(getApplicationContext());
                try {
                    addresses_origin = geocoder.getFromLocationName(place, PlaceActivity.geocoder_max_results);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (addresses_origin != null){
                    adapter_origin_places.clear();
                    // добавить адреса в адаптер adapter_dest_places
                    for(Address address: addresses_origin){
                        String addr = address.getAddressLine(0);
                        if (address.getAddressLine(1)!=null){
                            addr +=", "+address.getAddressLine(1);
                        }
                        if (address.getAddressLine(2)!=null){
                            addr +=", "+address.getAddressLine(2);
                        }
                        if (address.getAddressLine(3)!=null){
                            addr +=", "+address.getAddressLine(3);
                        }
                        adapter_origin_places.add(addr);
                    }
                    adapter_origin_places.notifyDataSetChanged();
                }
            }
        });

        // список вариантов мест
        listView_found_origin_places.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // избавляемся от предыдущих маркеров
                for (Marker item: marker_origin_map){
                    item.remove();
                }

                String address = ((TextView)view.findViewById(R.id.textView_places_list_item)).getText().toString();

                // извлекаю из cтроки адреса широту, долготу, название адреса

                //String tmp = address.substring(address.lastIndexOf(" "));
                double lng = addresses_origin.get(position).getLongitude();//Double.valueOf(tmp.trim());

                //String tmp2 = address.substring(0, address.lastIndexOf(" "));
                double lat = addresses_origin.get(position).getLatitude();//Double.valueOf(tmp2.substring(tmp2.lastIndexOf(" ")).trim());

                start_place_name = from = address;//tmp2.substring(0, tmp2.lastIndexOf(" "));

                // нужно показать на карте выбранное из списка вариантов место
                LatLng place = new LatLng(lat,lng);
                marker_origin_map.add(/*mFromMap*/mToMap.addMarker(new MarkerOptions().position(place).title(from)));
                CameraPosition cameraPosition = new CameraPosition.Builder().target(place).zoom(4).build();
                /*mFromMap*/mToMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                from = String.valueOf(lat)+","+String.valueOf(lng);
            }
        });

        //------------------------во вкладке "Куда"----------------------------------
        // запрос вариантов мест
        button_find_dest_place.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // нужно запросить у службы google geocoder место, которое ввел пользователь
                // и обратиться в отдельном потоке
                String place = editText_type_dest_place_name.getText().toString();
                /*new LoadPlaceFromGoogleGeocoderAsyncTask(place,
                        adapter_dest_places).execute();
*/
                // вариант с использованием геокдера
                Geocoder geocoder = new Geocoder(getApplicationContext());

                try {
                    addresses_dest = geocoder.getFromLocationName(place, PlaceActivity.geocoder_max_results);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (addresses_dest != null){
                    adapter_dest_places.clear();
                    // добавить адреса в адаптер adapter_dest_places
                    for(Address address: addresses_dest){
                        String addr = address.getAddressLine(0);
                        if ((address.getAddressLine(1)!=null)){
                            addr +=", "+address.getAddressLine(1);
                        }

                        if (address.getAddressLine(2)!=null){
                            addr +=", "+address.getAddressLine(2);
                        }
                        if (address.getAddressLine(3)!=null){
                            addr +=", "+address.getAddressLine(3);
                        }
                        adapter_dest_places.add(addr);
                    }
                    adapter_dest_places.notifyDataSetChanged();
                }
            }
        });

        // список вариантов мест
        listView_found_dest_places.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // избавляемся от предыдущих маркеров
                for (Marker item: marker_dest_map){
                    item.remove();
                }

                String address = ((TextView)view.findViewById(R.id.textView_places_list_item)).getText().toString();

                // извлекаю из cтроки адреса широту, долготу, название адреса

                //String tmp = address.substring(address.lastIndexOf(" "));
                double lng = addresses_dest.get(position).getLongitude();//Double.valueOf(tmp.trim());

                //String tmp2 = address.substring(0, address.lastIndexOf(" "));
                double lat = addresses_dest.get(position).getLatitude();//Double.valueOf(tmp2.substring(tmp2.lastIndexOf(" ")).trim());

                to = address;//tmp2.substring(0, tmp2.lastIndexOf(" "));

                // нужно показать на карте выбранное из списка вариантов место
                LatLng place = new LatLng(lat,lng);
                marker_dest_map.add(mToMap.addMarker(new MarkerOptions().position(place).title(to)));
                CameraPosition cameraPosition = new CameraPosition.Builder().target(place).zoom(4).build();
                mToMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                to = String.valueOf(lat)+","+String.valueOf(lng);
            }

        });

        // работа с картой


            // нужно получить из выбранного элемента списка найденных мест

            //Marker izh = mMap.addMarker(new MarkerOptions().position(izhevsk).title("Ижевск"));
            //CameraPosition cameraPosition = new CameraPosition.Builder().target(izhevsk).zoom(14).build();
            //mMap.setMyLocationEnabled(true);
            //mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);



    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /*@Override
    public void onResume(){
        super.onResume();
        this.from = "";
        this.to = "";
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.place_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search_path:


            /* при нажатии на кнопку "Найти путь"
             нужно запросить у службы путь Откуда - Куда(в отдельном потоке),
             затем, получив координаты от службы, нужно запустить окно с результатами, передав в намерении координаты,
             чтобы показать на карте путь
             */
                ArrayList<Address> coordinates = new ArrayList<Address>();
                new LoadDirectionFromGoogleDirectionAsyncTask(start_place_name, this.from, this.to, coordinates,
                        getApplicationContext()).execute();


                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}