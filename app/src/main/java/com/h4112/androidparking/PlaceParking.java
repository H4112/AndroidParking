package com.h4112.androidparking;

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
    private long tempsDebutOccupation;
    private long tempsFinOccupation;

    private BitmapDescriptor icone;

    public PlaceParking(int id, Etat etat, float latitude, float longitude, int idRue){
        this.id=id;
        this.etat=etat;
        this.latitude=latitude;
        this.longitude=longitude;
        this.idRue=idRue;

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

    public void setLibre(){
        this.etat=Etat.LIBRE;
        this.tempsFinOccupation=System.currentTimeMillis();
        updateIcone();
    }

    public void setOccupee(){
        this.etat=Etat.OCCUPEE;
        this.tempsDebutOccupation=System.currentTimeMillis();
        updateIcone();
    }

    public void setEnMouvement(){
        this.etat=Etat.EN_MOUVEMENT;
        updateIcone();
    }

    public long getTempsLibre(){
        return (System.currentTimeMillis()-this.tempsFinOccupation);
    }

    public long getTempsOccupee(){
        return (System.currentTimeMillis()-this.tempsDebutOccupation);
    }

    public boolean isInsideCircle(LatLng p, float radius){
        if( (p.latitude-this.latitude)*(p.latitude-this.latitude)+(p.longitude-this.latitude)*(p.longitude-this.latitude) <= radius*radius ){
            return true;
        }
        return false;
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

    public String getEtatString(){
        if(this.etat == Etat.LIBRE)
            return "Libre";
        else if(this.etat == Etat.OCCUPEE)
            return "Occupée";
        else if(this.etat == Etat.EN_MOUVEMENT)
            return "Sur le point de se libérer";
        else
            return "Etat indéterminé";
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

    @Override
    public String toString() {
        return "PlaceParking{" +
                "id=" + id +
                ", idRue=" + idRue +
                ", etat=" + etat +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", tempsDebutOccupation=" + tempsDebutOccupation +
                ", tempsFinOccupation=" + tempsFinOccupation +
                ", icone=" + icone +
                '}';
    }
}
