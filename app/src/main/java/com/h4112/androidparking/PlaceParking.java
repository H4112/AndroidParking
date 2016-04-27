package com.h4112.androidparking;

import android.content.Context;

import com.example.googlemaps.R;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

public class PlaceParking {
    public enum Etat { LIBRE, OCCUPEE, EN_MOUVEMENT }

    private int id;
    private int idRue;
    private Etat etat;
    private float latitude;
    private float longitude;
    private long derniereMaj;

    private BitmapDescriptor icone;

    public PlaceParking(int id, Etat etat, float latitude, float longitude, int idRue, long derniereMaj){
        this.id=id;
        this.etat=etat;
        this.latitude=latitude;
        this.longitude=longitude;
        this.idRue=idRue;
        this.derniereMaj=derniereMaj;

        updateIcone();
    }

    public void updateIcone(){
        if(this.etat == Etat.LIBRE){
            icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_libre);
        }
        else if(this.etat == Etat.OCCUPEE){
            icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_occupee);
        }
        else{
            icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_en_mouvement);
        }
    }

    public int getDureeEtatActuelMin(){
        return (int) (System.currentTimeMillis() - derniereMaj) / 60000;
    }

    public LatLng getCoord(){
        return new LatLng(this.latitude, this.longitude);
    }

    public double getDistanceFromPoint(LatLng point){
        return ( (point.longitude-this.longitude)*(point.longitude-this.longitude)+(point.latitude-this.latitude)*(point.latitude-this.latitude) );
    }

    public Etat getEtat(){
        return this.etat;
    }

    public String getEtatString(Context c){
        String infos;
        if(this.getEtat()==PlaceParking.Etat.OCCUPEE){
            infos = c.getString(R.string.place_occupee, this.getDureeEtatActuelMin()+"");
        } else if(this.getEtat()== PlaceParking.Etat.LIBRE){
            infos = c.getString(R.string.place_libre, this.getDureeEtatActuelMin()+"");
        } else {
            infos = c.getString(R.string.place_en_mouvement);
        }

        return infos;
    }

    public BitmapDescriptor getIcone(){
        return this.icone;
    }

    public float getLatitude(){
        return this.latitude;
    }

    public float getLongitude(){
        return this.longitude;
    }

    public PlaceParking(int id, int idRue, Etat etat, float latitude, float longitude, long derniereMaj, BitmapDescriptor icone) {
        this.id = id;
        this.idRue = idRue;
        this.etat = etat;
        this.latitude = latitude;
        this.longitude = longitude;
        this.derniereMaj = derniereMaj;
        this.icone = icone;
    }
}
