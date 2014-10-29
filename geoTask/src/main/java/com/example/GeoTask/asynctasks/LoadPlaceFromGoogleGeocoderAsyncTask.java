package com.example.GeoTask.asynctasks;

import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by egordeev on 14.09.14.
 */
public class LoadPlaceFromGoogleGeocoderAsyncTask extends AsyncTask<Void, Void, List<String>> {
    private String place_to_find;
    private String url_place_to_find;
    private ArrayAdapter<String> adapter_with_possible_places;

    private static final int max_results_number = 7;

    public LoadPlaceFromGoogleGeocoderAsyncTask(String place_to_find,
                                                ArrayAdapter<String> possible_places){
        this.place_to_find = place_to_find;
        this.adapter_with_possible_places = possible_places;

        // обращение к google directions api: http://maps.googleapis.com/maps/api/geocode/json?address=
        String url = "http://maps.googleapis.com/maps/api/geocode/json?address=";
        String sensor = "&sensor=false";
        StringBuilder final_url = new StringBuilder();
        final_url.append(url);
        final_url.append(this.place_to_find);
        final_url.append(sensor);
        this.url_place_to_find = final_url.toString();
    }

    @Override
    protected void onPreExecute(){
        this.adapter_with_possible_places.clear();
        this.adapter_with_possible_places.notifyDataSetChanged();
    }

    @Override
    protected List<String> doInBackground(Void... params) {
        List<String> res = new ArrayList<String>();
        try {
            HttpClient httpclient = new DefaultHttpClient();

            HttpGet request = new HttpGet();
            URI website = new URI(url_place_to_find);
            request.setURI(website);
            HttpResponse response = httpclient.execute(request);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));
            if(response.getStatusLine().getStatusCode() == 200 ){
                StringBuilder server_response = new StringBuilder();
                String tmp = "";
                while ((tmp = in.readLine()) != null){
                    server_response.append(tmp);
                }

                // обрабатываем json-ответ сервера
                try {
                    JSONObject jsonResponse = new JSONObject(server_response.toString());
                    String status_code = jsonResponse.getString("status");
                    if (status_code.equals("OK")) {
                        JSONArray results_array = jsonResponse.getJSONArray("results");
                        // нужно проверить, сколько объектов находится в массиве results_array, потому что может быть > 1
                        int len = results_array.length();
                        if (len > max_results_number) {
                            len = max_results_number;
                        }
                        for (int i = 0; i < len; i++) {
                            JSONObject result_object = results_array.getJSONObject(i);
                            JSONObject geometry_object = result_object.getJSONObject("geometry");
                            JSONObject location_object = geometry_object.getJSONObject("location");
                            double lat = location_object.getDouble("lat");
                            double lng = location_object.getDouble("lng");
                            // формирую адрес для списка
                            StringBuilder address = new StringBuilder();
                            address.append(result_object.getString("formatted_address"));
                            address.append(" ");
                            address.append(lat);
                            address.append(" ");
                            address.append(lng);

                            res.add(address.toString());
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return res;
    }

    @Override
    protected void onPostExecute(List<String> res){
        for (String item:res){
            adapter_with_possible_places.add(item);
        }

        adapter_with_possible_places.notifyDataSetChanged();
    }
}
