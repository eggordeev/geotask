package com.example.GeoTask.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.AsyncTask;
import com.example.GeoTask.ResultsActivity;
import com.google.android.gms.maps.model.LatLng;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by egordeev on 15.09.14.
 */
public class LoadDirectionFromGoogleDirectionAsyncTask extends AsyncTask<Void, Void, Void> {
    private double origin_lat, origin_lng;
    private double dest_lat, dest_lng;

    private String coordinates_origin_place;
    private String coordinates_dest_place;

    private List<LatLng> coordinatesList;
    private ArrayList<Address> coordinates;

    private String start_place_name;

    private Context context;


    public LoadDirectionFromGoogleDirectionAsyncTask(double origin_lat, double origin_lng,
                                                     double dest_lat, double dest_lng){
        this.origin_lat = origin_lat;
        this.origin_lng = origin_lng;
        this.dest_lat = dest_lat;
        this.dest_lng = dest_lng;
    }

    public LoadDirectionFromGoogleDirectionAsyncTask(String start_place_name, String coordinates_origin_place,
                                                     String coordinates_dest_place,
                                                     ArrayList<Address> coordinates,
                                                     Context context){
        this.start_place_name = start_place_name;
        this.coordinates_origin_place = coordinates_origin_place;
        this.coordinates_dest_place = coordinates_dest_place;
        this.coordinates = coordinates;
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {

        // нужно сделать запрос к google directions и получить координаты маршрута
        String url = "http://maps.googleapis.com/maps/api/directions/json?origin="+this.coordinates_origin_place;
        String dest_addr = "&destination="+this.coordinates_dest_place;
        String sensor = "&sensor=false";
        url = url+dest_addr+sensor;

        HttpClient httpclient = new DefaultHttpClient();

        HttpGet request = new HttpGet();
        URI website = null;
        try {
            website = new URI(url);

            request.setURI(website);
            HttpResponse http_response = null;

            http_response = httpclient.execute(request);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    http_response.getEntity().getContent()));

            if (http_response.getStatusLine().getStatusCode() == 200) {
                StringBuilder server_response = new StringBuilder();
                String tmp = "";
                while ((tmp = in.readLine()) != null){
                    server_response.append(tmp);
                }

                // обрабатываем json-ответ сервера
                try {
                    JSONObject response = new JSONObject(server_response.toString());
                    String status = response.getString("status");
                    if (status.equals("OK")){
                        JSONArray routes = response.getJSONArray("routes");
                        JSONObject obj = routes.getJSONObject(0);
                        JSONArray legs = obj.getJSONArray("legs");
                        coordinatesList = new ArrayList<LatLng>();
                        obj = legs.getJSONObject(0);
                        JSONObject start_location = obj.getJSONObject("start_location");
                        coordinatesList.add(new LatLng(start_location.getDouble("lat"),
                                start_location.getDouble("lng")));
                        JSONArray steps = obj.getJSONArray("steps");
                        JSONObject end_location;

                        for (int i=0;i < steps.length(); i++){
                            obj = steps.getJSONObject(i);
                            end_location = obj.getJSONObject("end_location");

                            coordinatesList.add(new LatLng(end_location.getDouble("lat"),
                                    end_location.getDouble("lng")));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
         catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void p){
        Intent intent = new Intent(this.context, ResultsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (this.coordinatesList != null) {
            for(LatLng item:this.coordinatesList){
                Address address = new Address(new Locale("en"));
                address.setLatitude(item.latitude);
                address.setLongitude(item.longitude);
                this.coordinates.add(address);
            }
            // вызвать форму результатов и передать список координат маршрута
            intent.putParcelableArrayListExtra("Coordinates", coordinates);
            intent.putExtra("start_place_name",start_place_name);
            intent.putExtra("status","Found");
            this.context.startActivity(intent);
        }
        else{
            intent.putExtra("status","Unknown");
            this.context.startActivity(intent);
        }

    }

}
