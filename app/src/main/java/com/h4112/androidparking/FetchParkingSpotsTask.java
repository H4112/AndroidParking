package com.h4112.androidparking;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

        Log.v("FetchParkingSpotsTask", "Beginning download of sensors from the server");

        Params param = params[0];

        ArrayList<PlaceParking> placesParking = new ArrayList<>();

        try {
            String json = "";
            URL url = new URL("https://parking.rsauget.fr:8080/sensors?latitude="+param.latitude
                    +"&longitude="+param.longitude+"&radius="+param.radius);

            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

            String line = reader.readLine();
            while(line != null) {
                json += line;
                line = reader.readLine();
            }
            reader.close();

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
                placesParking = null;
            }
        } catch (IOException e) {
            Log.w("FetchParkingSpotsTask", "Input-output error!", e);
            placesParking = null;
        }

        Log.v("FetchParkingSpotsTask", "Start caching");
        placesParking = loadOrSaveSpots(placesParking, param);
        Log.v("FetchParkingSpotsTask", "End caching");

        if(placesParking != null) Log.d("FetchParkingSpotsTask", "Returning "+placesParking.size()+" items");

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



    /**
     * Si spots = null, charge des places de parking depuis le cache et les renvoie.
     * Si spots != null, met à jour le cache et renvoie spots non modifié.
     * @param spots Places renvoyées par le serveur
     * @return Places à afficher
     */
    private ArrayList<PlaceParking> loadOrSaveSpots(ArrayList<PlaceParking> spots, Params params) {
        String path = activity.getCacheDir().getPath() + File.separator + "parkingspots.txt";

        List<PlaceParking> cached = new LinkedList<>();

        File f = new File(path);
        if(f.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));

                String s = br.readLine();
                while (s != null) {
                    cached.add(PlaceParking.fromCacheCSV(s));
                    s = br.readLine();
                }

                br.close();
            } catch(IOException | NumberFormatException e) {
                Log.e("FetchParkingSpotsTask", "Could not read from existing cache!", e);

                return spots;
            }
        }

        ArrayList<PlaceParking> possibleOnesFromCache = new ArrayList<>();
        for(PlaceParking p : cached) {
            if(p.getDistanceFromPoint(new LatLng(params.latitude, params.longitude)) < params.radius) {
                possibleOnesFromCache.add(p);
            }
        }

        if(spots == null) {
            //renvoyer ce qui a été trouvé dans le cache
            if(possibleOnesFromCache.isEmpty()) {
                return null; //une erreur sera affichée
            } else {
                return possibleOnesFromCache;
            }
        } else {
            //tenter de trouver les nouvelles places dans le cache existant et les mettre à jour
            for(PlaceParking p : spots) {
                if(possibleOnesFromCache.contains(p)) {
                    //place déjà présente
                    possibleOnesFromCache.remove(p);

                    //déplacer p en fin de liste en le mettant éventuellement à jour
                    cached.remove(p);
                    cached.add(p);
                } else {
                    //nouvelle place
                    cached.add(p);
                }
            }

            //supprimer du cache toutes les places qui y sont et qui n'ont pas été renvoyées par le serveur
            for(PlaceParking toRemove : possibleOnesFromCache) {
                cached.remove(toRemove);
            }

            //réduire le cache à une taille raisonnable (les premières places sont les plus anciennes)
            while(cached.size() > 1000) {
                cached.remove(0);
            }

            //enregistrer le cache
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));

                for(PlaceParking p : cached) {
                    bw.write(p.toCacheCSV()+"\n");
                }

                bw.close();
            } catch(IOException e) {
                Log.e("FetchParkingSpotsTask", "Could not write to cache!", e);
            }

            return spots;
        }
    }

}

