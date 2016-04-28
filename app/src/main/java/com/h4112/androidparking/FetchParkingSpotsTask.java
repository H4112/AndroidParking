package com.h4112.androidparking;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Permet de récupérer la liste des places de parking dans un rayon donné.
 */
public class FetchParkingSpotsTask extends AsyncTask<FetchParkingSpotsTask.Params,
        Void, ArrayList<PlaceParking>> {
    private MainActivity activity;

    public FetchParkingSpotsTask(MainActivity activity) {
        super();
        this.activity = activity;
    }

    @Override
    protected ArrayList<PlaceParking> doInBackground(Params... params) {
        if(params.length != 1) {
            throw new IllegalArgumentException("This task takes only one Params object");
        }

        Log.d("FetchParkingSpotsTask", "Beginning download of sensors from the server");

        Params param = params[0];

        String json = "";
        try {
            URL url = new URL("https://parking.rsauget.fr:8080/sensors?latitude="+param.latitude
                    +"&longitude="+param.longitude+"&radius="+param.radius);

            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

            String line = reader.readLine();
            while(line != null) {
                json += line;
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ArrayList<PlaceParking> placesParking = new ArrayList<>();
        try {
            JSONArray sensors = new JSONArray(json);

            for(int i = 0; i < sensors.length(); i++) {
                JSONObject sensor = sensors.getJSONObject(i);

                PlaceParking thisSpot = new PlaceParking(
                        sensor.getInt("id"),
                        getState(sensor.getString("etat")),
                        (float) sensor.getDouble("latitude"),
                        (float) sensor.getDouble("longitude"),
                        sensor.getInt("idRue"),
                        sensor.getLong("derniereMaj"));

                placesParking.add(thisSpot);

                Log.v("FetchParkingSpotsTask", "New spot: "+thisSpot);
            }
        } catch (JSONException | IllegalArgumentException e) {
            Log.e("FetchParkingSpotsTask", "JSON returned by server is invalid!", e);
            return null;
        }

        return placesParking;
    }

    private PlaceParking.Etat getState(String etat) throws IllegalArgumentException {
        switch(etat) {
            case "depart": return PlaceParking.Etat.EN_MOUVEMENT;
            case "libre": return PlaceParking.Etat.LIBRE;
            case "occupe": return PlaceParking.Etat.OCCUPEE;
            default: throw new IllegalArgumentException("Invalid state value");
        }
    }

    @Override
    protected void onPostExecute(ArrayList<PlaceParking> places) {
        if(places == null) {
            activity.listePlacesFailure();
        } else {
            ArrayList<PlaceParking> toGeotag = new ArrayList<>(places);
            for(PlaceParking placeParking : places) {
                String cached = GeocodingTask.getAddressFromCache(placeParking);
                if(cached != null) {
                    placeParking.setAddress(cached);
                    toGeotag.remove(placeParking);
                } else {
                    placeParking.setAddress(activity.getString(R.string.parking_spot));
                }
            }

            if(!toGeotag.isEmpty()) {
                GeocodingTask task = new GeocodingTask(activity);
                PlaceParking[] parkArray = new PlaceParking[toGeotag.size()];
                toGeotag.toArray(parkArray);
                task.execute(parkArray);
            }

            activity.setListePlaces(places);
        }
    }

    protected static class Params {
        private double latitude;
        private double longitude;
        private int radius;

        public Params(double latitude, double longitude, int radius) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.radius = radius;
        }
    }

}

