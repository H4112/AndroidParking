package com.h4112.androidparking;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Permet de récupérer la liste des places de parking dans un rayon donné.
 */
public class GeocodingTask extends AsyncTask<PlaceParking, Void, Void> {
    private MainActivity activity;

    private static Map<LatLng, String > addressCache = new HashMap<>();

    public static String getAddressFromCache(PlaceParking placeParking) {
        if(addressCache.containsKey(placeParking.getCoord())) {
            return addressCache.get(placeParking.getCoord());
        } else {
            return null;
        }
    }


    public GeocodingTask(MainActivity activity) {
        super();
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(PlaceParking... params) {

        Log.d("GeocodingTask", "Beginning geocoding tasks");

        for(PlaceParking p : params) {
            String address = getCompleteAddressString(p.getCoord());
            p.setAddress(address);
        }

        return null;

    }

    private String getCompleteAddressString(LatLng pos) {
        if(addressCache.containsKey(pos)) {
            return addressCache.get(pos);
        }

        String strAdd;
        Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(pos.latitude, pos.longitude, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append(", ");
                }
                strAdd = strReturnedAddress.toString();
                strAdd = strAdd.substring(0, strAdd.length() - 2);
                Log.v("GeocodingTask", "Geotagged " + strAdd);

            } else {
                Log.v("GeocodingTask", "No Address returned!");
                return activity.getString(R.string.parking_spot);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.v("GeocodingTask", "Could not get Address!");
            return activity.getString(R.string.parking_spot);
        }

        addressCache.put(pos, strAdd);
        return strAdd;
    }
}

