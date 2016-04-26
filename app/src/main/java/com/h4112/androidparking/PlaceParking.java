package com.h4112.androidparking;

import com.example.googlemaps.R;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

public class PlaceParking {
    public static int LIBRE = 0;
    public static int OCCUPEE = 1;
    public static int EN_MOUVEMENT = 2;

    private String id;
    private int etat;
    private float latitude;
    private float longitude;
    private long tempsDebutOccupation;
    private long tempsFinOccupation;

    private BitmapDescriptor icone;

    public PlaceParking(String id, int etat, float latitude, float longitude){
        this.id=id;
        this.etat=etat;
        this.latitude=latitude;
        this.longitude=longitude;

        updateIcone();
    }

    public void updateIcone(){
        if(this.etat == LIBRE){
            icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_libre);
        }
        else if(this.etat == OCCUPEE){
            icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_occupee);
        }
        else{
            icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_en_mouvement);
        }
    }

    public void setLibre(){
        this.etat=LIBRE;
        this.tempsFinOccupation=System.currentTimeMillis();
        updateIcone();
    }

    public void setOccupee(){
        this.etat=OCCUPEE;
        this.tempsDebutOccupation=System.currentTimeMillis();
        updateIcone();
    }

    public void setEnMouvement(){
        this.etat=EN_MOUVEMENT;
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

    public int getEtat(){
        return this.etat;
    }

    public String getEtatString(){
        if(this.etat == LIBRE)
            return "Libre";
        else if(this.etat == OCCUPEE)
            return "Occupée";
        else if(this.etat == EN_MOUVEMENT)
            return "Sur le point de se libérer";
        else
            return "Etat indéterminé";
    }

    public BitmapDescriptor getIcone(){
        return this.icone;
    }
}
