package com.h4112.androidparking;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

/**
 * Permet de récupérer des coordonnées à partir d'une recherche de l'utilisateur.
 */
public class GeocoderTask extends AsyncTask<String, Void, GeocoderTask.Result> {
    private MainActivity activity;

    public GeocoderTask(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Result doInBackground(String... params) {
        if(params.length != 1) {
            throw new IllegalArgumentException("Task does take only 1 parameter");
        }

        Log.d("GeocoderTask", "Task began");

        Geocoder coder = new Geocoder(activity);
        try {
            List<Address> addresses = coder.getFromLocationName(params[0], 1);

            if(!addresses.isEmpty()) {
                Address address = addresses.get(0);
                return new Result(true, new LatLng(address.getLatitude(), address.getLongitude()));
            } else {
                //la requête a abouti, mais sans résultat
                return new Result(true, null);
            }
        } catch (IOException e) {
            Log.w("GeocoderTask", "I/O error while getting location name!", e);
            return new Result(false, null);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        Log.d("GeocoderTask", "Task ended with "+result);

        if(result.requestResult != null) {
            activity.geocodingFinished(result.requestResult);
        } else {
            activity.geocodingFailed(result.requestEnded);
        }
    }

    @Override
    protected void onCancelled(Result r) {
        Log.i("GeocoderTask", "Was cancelled");
    }

    protected static class Result {
        public boolean requestEnded;
        public LatLng requestResult;

        public Result(boolean requestEnded, LatLng requestResult) {
            this.requestEnded = requestEnded;
            this.requestResult = requestResult;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "requestEnded=" + requestEnded +
                    ", requestResult=" + requestResult +
                    '}';
        }
    }
}
