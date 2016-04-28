package com.h4112.androidparking;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class PlaceParking implements Parcelable, ClusterItem {
    protected PlaceParking(Parcel in) {
        id = in.readInt();
        idRue = in.readInt();
        latitude = in.readFloat();
        longitude = in.readFloat();
        derniereMaj = in.readLong();
        address = in.readString();
    }

    public static final Creator<PlaceParking> CREATOR = new Creator<PlaceParking>() {
        @Override
        public PlaceParking createFromParcel(Parcel in) {
            return new PlaceParking(in);
        }

        @Override
        public PlaceParking[] newArray(int size) {
            return new PlaceParking[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(idRue);
        dest.writeFloat(latitude);
        dest.writeFloat(longitude);
        dest.writeLong(derniereMaj);
        dest.writeString(address);
    }

    @Override
    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }

    public enum Etat { LIBRE, OCCUPEE, EN_MOUVEMENT }

    private int id;
    private int idRue;
    private Etat etat;
    private float latitude;
    private float longitude;
    private long derniereMaj;
    private String address;
    private int color;

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

    private void updateIcone(){
        if(this.etat == Etat.LIBRE){
            icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_libre);
            color = Color.rgb(2, 224, 23);
        }
        else if(this.etat == Etat.OCCUPEE){
            icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_occupee);
            color = Color.rgb(255, 2, 2);
        }
        else{
            icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_en_mouvement);
            color = Color.rgb(236, 194, 2);
        }
    }

    public int getDureeEtatActuelMin(){
        return (int) (System.currentTimeMillis() - derniereMaj) / 60000;
    }

    public LatLng getCoord(){
        return new LatLng(this.latitude, this.longitude);
    }

    public double getDistanceFromPoint(LatLng point){
        Location me = new Location("generated");
        me.setLatitude(latitude);
        me.setLongitude(longitude);
        Location ref = new Location("generated");
        ref.setLatitude(point.latitude);
        ref.setLongitude(point.longitude);

        double dist = me.distanceTo(ref);
        Log.v("PlaceParking", "Distance = "+dist);

        return dist;
    }

    public Etat getEtat(){
        return this.etat;
    }

    public String getEtatString(){
        if(this.getEtat() == Etat.LIBRE){
            return "Place Libre";
        }
        else if(this.getEtat() == Etat.OCCUPEE){
            return "Place Occupée";
        }
        else if(this.getEtat() == Etat.EN_MOUVEMENT){
            return "Place Bientôt Libre";
        }
        else{
            return "Place Indéterminée";
        }
    }

    public String getDurationString(Context c) {
        int hours = this.getDureeEtatActuelMin() / 60;
        int mins = this.getDureeEtatActuelMin() % 60;

        String infos;
        if(mins == 0 && hours == 0) {
            infos = c.getString(R.string.time_now);
        } else if(hours == 0) {
            infos = c.getString(R.string.time_m, ""+mins);
        } else {
            infos = c.getString(R.string.time_h, ""+hours, ""+mins);
        }
        return infos;
    }

    public BitmapDescriptor getIcone(){
        return this.icone;
    }

    public int getColor() {
        return color;
    }

    public float getLatitude(){
        return this.latitude;
    }

    public float getLongitude(){
        return this.longitude;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "PlaceParking{" +
                "id=" + id +
                ", idRue=" + idRue +
                ", etat=" + etat +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", derniereMaj=" + derniereMaj +
                ", icone=" + icone +
                '}';
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
