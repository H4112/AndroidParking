package com.h4112.androidparking;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Représente une place de parking figurant sur la carte.
 */
public class PlaceParking implements Parcelable, ClusterItem {
    ////////////////////////// PARCELABLE METHODS //////////////////////////
    protected PlaceParking(Parcel in) {
        id = in.readInt();
        idRue = in.readInt();
        latitude = in.readFloat();
        longitude = in.readFloat();
        derniereMaj = in.readLong();
        address = in.readString();
        etatString = in.readString();
        etat = Etat.valueOf(in.readString());

        updateIcone();
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
        dest.writeString(etatString);
        dest.writeString(etat.name());
    }

    ////////////////////////// END PARCELABLE METHODS //////////////////////////

    public enum Etat { LIBRE, OCCUPEE, EN_MOUVEMENT, INCONNU, GRANDLYON }

    private int id;
    private int idRue;
    private Etat etat;
    private float latitude;
    private float longitude;
    private long derniereMaj;
    private String address;
    private String etatString;

    private int color;
    private BitmapDescriptor icone;

    /**
     * Permet de définir une place de parking <b>pour une place du parking du Grand Lyon</b>.
     * @param id Identifiant
     * @param etat État fourni par le Grand Lyon
     * @param latitude Latitude de la place
     * @param longitude Longitude de la place
     * @param idRue Identifiant de la rue (pour les clusters)
     * @param derniereMaj Dernière mise à jour de l'état
     * @param adresse Adresse de la place
     */
    public PlaceParking(int id, String etat, float latitude, float longitude, int idRue, long derniereMaj, String adresse){
        this(id, Etat.GRANDLYON, latitude, longitude, idRue, derniereMaj, adresse);
        etatString = etat;

        updateIcone();
    }

    /**
     * Permet de définir une place de parking <b>pour une place obtenue par capteur</b>.
     * @param id Identifiant
     * @param etat État fourni par le capteur (libre, occupé, en mouvement, état inconnu)
     * @param latitude Latitude de la place
     * @param longitude Longitude de la place
     * @param idRue Identifiant de la rue (pour les clusters)
     * @param derniereMaj Dernière mise à jour de l'état
     * @param adresse Adresse de la place
     */
    public PlaceParking(int id, Etat etat, float latitude, float longitude, int idRue, long derniereMaj, String adresse){
        this.id=id;
        this.etat=etat;
        this.latitude=latitude;
        this.longitude=longitude;
        this.idRue=idRue;
        this.derniereMaj=derniereMaj;
        this.address = adresse;
        this.etatString = null;

        updateIcone();
    }

    /**
     * Permet de changer l'icône et la couleur en fonction de l'état.
     */
    private void updateIcone(){
        try {
            if (this.etat == Etat.LIBRE) {
                icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_libre);
                color = Color.rgb(2, 224, 23);
            } else if (this.etat == Etat.OCCUPEE) {
                icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_occupee);
                color = Color.rgb(255, 2, 2);
            } else if (this.etat == Etat.EN_MOUVEMENT) {
                icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_en_mouvement);
                color = Color.rgb(236, 194, 2);
            } else if (this.etat == Etat.GRANDLYON && etatString != null) {
                color = Color.rgb(0, 0, 255);
            } else {
                icone = BitmapDescriptorFactory.fromResource(R.drawable.ic_inconnu);
                color = Color.rgb(64, 64, 64);
            }
        } catch(NullPointerException npe) {
            Log.v("PlaceParking", "Error: could not init icon");
            icone = null;
        }
    }

    /**
     * Permet d'obtenir le nombre de minutes écoulées depuis le dernier changement d'état.
     * @return Durée de l'état actuel en minutes
     */
    public int getDureeEtatActuelMin(){
        return (int) (System.currentTimeMillis() - derniereMaj) / 60000;
    }

    /**
     * Permet d'obtenir la distance de la place par rapport à un autre point.
     * @param point Point dont il faut calculer la distance
     * @return Distance au point donné
     */
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

    /**
     * Donne l'état actuel du capteur (Etat.GRANDLYON si c'est un parking du Grand Lyon).
     * @return État actuel du capteur
     */
    public Etat getEtat(){
        return this.etat;
    }

    /**
     * Donne la description de l'état.
     * Pour les parkings du Grand Lyon, cela inclut le nombre de places libres.
     * @param c Contexte permettant de récupérer les ressources
     * @return Description de l'état
     */
    public String getEtatString(Context c){
        if(this.getEtat() == Etat.LIBRE){
            return c.getString(R.string.free_spot);
        }
        else if(this.getEtat() == Etat.OCCUPEE){
            return c.getString(R.string.busy_spot);
        }
        else if(this.getEtat() == Etat.EN_MOUVEMENT){
            return c.getString(R.string.moving_spot);
        }
        else if(this.getEtat() == Etat.GRANDLYON){
            if(etatString == null) {
                etatString = "DONNEES INDISPONIBLES";
            }
            return etatString;
        }
        else{
            return c.getString(R.string.unknown_spot);
        }
    }

    /**
     * Donne la durée de l'état actuel sous la forme "X h et Y min".
     * @param c Contexte permettant de récupérer les ressources
     * @return Durée de l'état actuel
     */
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

    /**
     * Donne l'icône pour le marqueur.
     * @return Icône du marqueur
     */
    public BitmapDescriptor getIcone(){
        return this.icone;
    }

    /**
     * Donne la couleur du marqueur (pour les clusters).
     * @return Couleur du marqueur
     */
    public int getColor() {
        return color;
    }

    /**
     * Donne la latitude de la place de parking.
     * @return Latitude de la place
     */
    public float getLatitude(){
        return this.latitude;
    }

    /**
     * Donne la longitude de la place de parking.
     * @return Latitude de la place
     */
    public float getLongitude(){
        return this.longitude;
    }

    @Override
    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }

    /**
     * Donne l'identifiant de la place de parking.
     * @return Identifiant
     */
    public int getId() {
        return id;
    }

    /**
     * Donne l'identifiant de rue de la place de parking (pour les clusters).
     * @return Identifiant de rue
     */
    public int getIdRue() {
        return idRue;
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
                ", address='" + address + '\'' +
                ", color=" + color +
                ", icone=" + icone +
                '}';
    }

    /**
     * Permet de générer une entrée de cache pouvant être rechargée via fromCacheCSV.
     * @return Entrée de cache
     */
    public String toCacheCSV() {
        return id+";"+idRue+";"+latitude+";"+longitude+";"+address;
    }

    /**
     * Permet de construire une place de parking à partir d'une ligne de cache CSV.
     * Un ID négatif indique un Parking du Grand Lyon.
     * @param csv Ligne du cache
     * @return Place de parking reconstruite
     */
    public static PlaceParking fromCacheCSV(String csv) {
        String[] split = csv.split(";");

        return new PlaceParking(
                Integer.parseInt(split[0]),
                Integer.parseInt(split[0]) < 0 ? Etat.GRANDLYON : Etat.INCONNU,
                Float.parseFloat(split[2]),
                Float.parseFloat(split[3]),
                Integer.parseInt(split[1]),
                System.currentTimeMillis(),
                split[4]);
    }

    /**
     * Permet d'obtenir l'adresse du parking.
     * (Pour les parkings du Grand Lyon, il s'agit du nom du parking)
     * @return Adresse du parking
     */
    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlaceParking that = (PlaceParking) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

}
