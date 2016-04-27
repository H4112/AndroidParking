package com.h4112.androidparking;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Permet de récupérer la liste des places de parking dans un rayon donné.
 */
public class FetchParkingSpotsTask extends AsyncTask<FetchParkingSpotsTask.Params,
        Void, List<PlaceParking>> {
    private MainActivity activity;

    public FetchParkingSpotsTask(MainActivity activity) {
        super();
        this.activity = activity;
    }

    @Override
    protected List<PlaceParking> doInBackground(Params... params) {
        if(params.length != 1) {
            throw new IllegalArgumentException("This task takes only one Params object");
        }

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

        List<PlaceParking> placesParking = new ArrayList<>();
        try {
            JSONArray sensors = new JSONArray(json);

            for(int i = 0; i < sensors.length(); i++) {
                JSONObject sensor = sensors.getJSONObject(i);

                PlaceParking thisSpot = new PlaceParking(
                        Integer.parseInt(sensor.getString("id")),
                        getState(sensor.getString("etat")),
                        Float.parseFloat(sensor.getString("latitude")),
                        Float.parseFloat(sensor.getString("longitude")),
                        Integer.parseInt(sensor.getString("idRue")));

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
    protected void onPostExecute(List<PlaceParking> places) {
        if(places == null) {
            activity.listePlacesFailure();
        } else {
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

