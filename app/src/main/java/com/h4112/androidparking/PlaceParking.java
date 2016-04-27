package com.h4112.androidparking;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

public class PlaceParking implements Parcelable {
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

    public enum Etat { LIBRE, OCCUPEE, EN_MOUVEMENT }

    private int id;
    private int idRue;
    private Etat etat;
    private float latitude;
    private float longitude;
    private long derniereMaj;
    private String address;

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
        //TODO utiliser l'api google
        return ( (point.longitude-this.longitude)*(point.longitude-this.longitude)+(point.latitude-this.latitude)*(point.latitude-this.latitude) );
    }

    public Etat getEtat(){
        return this.etat;
    }

    public String getEtatString(Context c) {
        int hours = this.getDureeEtatActuelMin() / 60;
        int mins = this.getDureeEtatActuelMin() % 60;

        String infos;
        if(this.getEtat()==PlaceParking.Etat.OCCUPEE){
            if(hours == 0) {
                infos = c.getString(R.string.place_occupee_min, mins + "");
            } else {
                infos = c.getString(R.string.place_occupee_h, hours + "", mins + "");
            }
        } else if(this.getEtat()== PlaceParking.Etat.LIBRE){
            if(hours == 0) {
                infos = c.getString(R.string.place_libre_min, mins + "");
            } else {
                infos = c.getString(R.string.place_libre_h, hours + "", mins + "");
            }
        } else if(getDureeEtatActuelMin() < 5) {
            infos = c.getString(R.string.place_en_mouvement);
        } else {
            infos = c.getString(R.string.place_en_mouvement_depuis_longtemps);
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
