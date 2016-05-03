package com.h4112.androidparking;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Permet de récupérer la liste des places de parking dans un rayon donné.
 */
public class FetchParkingSpotsTask extends AsyncTask<FetchParkingSpotsTask.Params,
        Void, ArrayList<PlaceParking>> {
    private Callback callback;
    private Context context;
    private Params param;

    /**
     * Crée une nouvelle tâche de récupération des parkings.
     * A l'issue de cette tâche, activity.listePlacesFailure() ou activity.setListePlaces(list)
     * sera appelé.
     * @param context Contexte de l'application
     * @param callback Callback permettant de renvoyer le résultat à l'application
     */
    public FetchParkingSpotsTask(Context context, Callback callback) {
        super();
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected ArrayList<PlaceParking> doInBackground(Params... params) {
        if(params.length != 1) {
            throw new IllegalArgumentException("This task takes only one Params object");
        }

        Log.v("FetchParkingSpotsTask", "Beginning download of sensors from the server");

        param = params[0];

        ArrayList<PlaceParking> placesParking = null;

        if(param.mode == Params.Mode.AROUND_POSITION) {
            placesParking = getPlacesParkingAroundPosition(param);

            //mise en cache des parkings
            Log.v("FetchParkingSpotsTask", "Start caching");
            placesParking = loadOrSaveSpots(placesParking, param);
            Log.v("FetchParkingSpotsTask", "End caching");
        } else {
            PlaceParking place = getPlaceParkingById(param);

            if (place != null) {
                placesParking = new ArrayList<>(1);
                placesParking.add(place);
            }
        }

        if(placesParking != null) Log.d("FetchParkingSpotsTask", "Returning "+placesParking.size()+" items");

        return placesParking;
    }

    /**
     * Permet d'obtenir les places par position autour du rayon.
     * @param param Les paramètres de récupération
     * @return Les places de parking autour du point
     */
    @Nullable
    private ArrayList<PlaceParking> getPlacesParkingAroundPosition(Params param) {
        ArrayList<PlaceParking> placesParking = new ArrayList<>();

        try {
            //télécharger le JSON depuis le serveur
            String json = getJSONFromServer("https://parking.rsauget.fr:8080/sensors?latitude="+param.latitude
                    +"&longitude="+param.longitude+"&radius="+param.radius);

            try {
                //traiter ce JSON pour fabriquer des PlaceParking
                //tout d'abord les capteurs
                JSONObject fullResponse = new JSONObject(json);

                JSONArray sensors = fullResponse.getJSONArray("capteurs");

                for(int i = 0; i < sensors.length(); i++) {
                    JSONObject sensor = sensors.getJSONObject(i);

                    PlaceParking thisSpot = getPlaceParkingFromJSON(sensor);

                    if(thisSpot != null) placesParking.add(thisSpot);

                    Log.v("FetchParkingSpotsTask", "New spot: "+thisSpot);
                }

                //puis les parkings fournis par le Grand Lyon
                JSONArray parkings = fullResponse.getJSONArray("parkings");

                for(int i = 0; i < parkings.length(); i++) {
                    try {
                        JSONObject parking = parkings.getJSONObject(i);

                        JSONObject loc = parking.getJSONObject("loc");
                        JSONArray params = loc.getJSONArray("coordinates");

                        PlaceParking thisSpot = new PlaceParking(
                                -Integer.parseInt(parking.getString("_id")),
                                parking.getString("etat"),
                                (float) params.getDouble(1),
                                (float) params.getDouble(0),
                                -Integer.parseInt(parking.getString("_id")),
                                getLastUpdate(parking.getString("last_update")),
                                parking.getString("nom"));

                        placesParking.add(thisSpot);

                        Log.v("FetchParkingSpotsTask", "New spot: " + thisSpot);
                    } catch(JSONException | IllegalArgumentException e) {
                        Log.w("FetchParkingSpotsTask", "JSON invalid for single spot.", e);
                    }
                }
            } catch (JSONException | IllegalArgumentException e) {
                Log.e("FetchParkingSpotsTask", "JSON returned by server is invalid!", e);
                placesParking = null;
            }
        } catch (IOException e) {
            Log.w("FetchParkingSpotsTask", "Input-output error!", e);
            placesParking = null;
        }
        return placesParking;
    }

    /**
     * Obtient une place de parking par id
     * @param param Paramètres (contient l'id)
     * @return La place de parking
     */
    private PlaceParking getPlaceParkingById(Params param) {
        PlaceParking placeParking;
        try {
            try {
                String json = getJSONFromServer("https://parking.rsauget.fr:8080/sensors/"+param.id);

                JSONObject obj = new JSONObject(json);
                placeParking = getPlaceParkingFromJSON(obj);
            } catch (JSONException | IllegalArgumentException e) {
                Log.e("FetchParkingSpotsTask", "JSON returned by server is invalid!", e);
                placeParking = null;
            }
        } catch (IOException e) {
            Log.w("FetchParkingSpotsTask", "Input-output error!", e);
            placeParking = null;
        }
        return placeParking;
    }

    /**
     * Permet d'obtenir la réponse complète du serveur sous forme de String.
     * @param urlString URL à récupérer
     * @return Réponse du serveur
     * @throws IOException En cas d'erreur de connexion.
     */
    @NonNull
    private String getJSONFromServer(String urlString) throws IOException {
        Log.v("FetchParkingSpotsTask", "Retrieving from server url "+urlString);

        String json = "";
        URL url = new URL(urlString);

        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        String line = reader.readLine();
        while(line != null) {
            json += line;
            line = reader.readLine();
        }
        reader.close();

        Log.v("FetchParkingSpotsTask", "Retrieving from server finished");
        return json;
    }

    /**
     * Permet de convertir un JSONObject représentant une place de parking (par capteur), en objet PlaceParking.
     * @param sensor JSONObject représentant la place
     * @return L'objet PlaceParking
     */
    private PlaceParking getPlaceParkingFromJSON(JSONObject sensor) {
        try {
            JSONObject loc = sensor.getJSONObject("loc");
            JSONArray params = loc.getJSONArray("coordinates");

            return new PlaceParking(
                sensor.getInt("_id"),
                getState(sensor.getString("etat")),
                (float) params.getDouble(1),
                (float) params.getDouble(0),
                sensor.getInt("idRue"),
                sensor.getLong("derniereMaj"),
                sensor.getString("adresse"));

        } catch(JSONException | IllegalArgumentException e) {
            Log.w("FetchParkingSpotsTask", "JSON invalid for single spot.", e);

            return null;
        }
    }

    /**
     * Transforme une date du type 2016-01-05 10:59:28 en timestamp
     * (comparable à System.currentTimeMillis()).
     * @param lastUpdate Date sous forme de String
     * @return Date sous forme de timestamp
     */
    private long getLastUpdate(String lastUpdate) {
        if(lastUpdate.isEmpty()) return System.currentTimeMillis();

        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRENCH)
                    .parse(lastUpdate).getTime();
        } catch (ParseException e) {
            Log.e("FetchParkingSpotsTask", "Parse error! ",e);
            return System.currentTimeMillis();
        }
    }

    /**
     * Permet d'obtenir l'état à partir de la chaîne de caractères envoyée par le serveur.
     * @param etat Soit "libre", soit "depart", soit "occupe"
     * @return Le PlaceParking.Etat correspondant
     * @throws IllegalArgumentException Si l'état ne correspond à aucun des 3 valides énoncés ci-dessus
     */
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
            callback.listePlacesFailure(param);
        } else {
            callback.setListePlaces(places, param);
        }
    }

    @Override
    protected void onCancelled(ArrayList<PlaceParking> result) {
        Log.i("FetchParkingSpotsTask", "Task was canceled. Not calling back MainActivity!");
    }

    /**
     * Paramètres d'exécution de la tâche de récupération des places de parking.
     */
    protected static class Params {
        public enum Mode { AROUND_POSITION, BY_ID }
        private Mode mode;

        public double latitude;
        public double longitude;
        public int radius;

        private int id;

        /**
         * Permet de créer des paramètres, qui pourront être passés à la tâche.
         * @param latitude Latitude de référence
         * @param longitude Longitude de référence
         * @param radius Rayon (en mètres) par rapport à la position de référence,
         *               dans laquelle il faut trouver des places de parking
         */
        public Params(Mode mode, double latitude, double longitude, int radius, int id) {
            this.mode = mode;
            this.latitude = latitude;
            this.longitude = longitude;
            this.radius = radius;
            this.id = id;
        }
    }

    /**
     * Si spots = null, charge des places de parking depuis le cache et les renvoie.
     * Si spots != null, met à jour le cache et renvoie spots non modifié.
     * @param spots Places renvoyées par le serveur
     * @return Places à afficher
     */
    private ArrayList<PlaceParking> loadOrSaveSpots(final ArrayList<PlaceParking> spots, Params params) {
        String path = context.getCacheDir().getPath() + File.separator + "parkingspots.txt";

        List<PlaceParking> cached = new LinkedList<>();

        //lire tout le cache depuis le fichier
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

        //filtrer tous ces résultats pour garder ceux qui sont dans le rayon
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

    public interface Callback {
        void listePlacesFailure(Params params);
        void setListePlaces(ArrayList<PlaceParking> parks, Params params);
    }
}

