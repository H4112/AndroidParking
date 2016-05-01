package com.h4112.androidparking;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.algo.Algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Clustering algorithm based on item density
 * TODO USE ME OR DELETE ME!
 */
public class RoadIdBasedAlgorithm implements Algorithm<PlaceParking> {
    ArrayList<PlaceParking> list = new ArrayList<>();

    @Override
    public synchronized void addItem(PlaceParking placeParking) {
        list.add(placeParking);
    }

    @Override
    public synchronized void addItems(Collection<PlaceParking> collection) {
        list.addAll(collection);
    }

    @Override
    public synchronized void clearItems() {
        list.clear();
    }

    @Override
    public synchronized void removeItem(PlaceParking placeParking) {
        list.remove(placeParking);
    }

    @Override
    public synchronized Set<? extends Cluster<PlaceParking>> getClusters(double zoom) {
        Log.d("Algorithm", "Start clustering.");
        long time = System.currentTimeMillis();

        final double zoomSpecificSpan = 1000000 / Math.pow(2, zoom);
        Log.v("Algorithm", "Minimal distance is "+zoomSpecificSpan);

        //trier les places selon leur nom de rue
        HashMap<Integer, ArrayList<PlaceParking>> clusterIds = new HashMap<>();
        for(PlaceParking p: list) {
            if(clusterIds.containsKey(p.getIdRue())) {
                clusterIds.get(p.getIdRue()).add(p);
            } else {
                ArrayList<PlaceParking> newOne = new ArrayList<PlaceParking>();
                newOne.add(p);
                clusterIds.put(p.getIdRue(), newOne);
            }
        }

        //générer une liste de clusters
        ArrayList<MeanCluster> clusters = new ArrayList<>();
        for(Map.Entry<Integer, ArrayList<PlaceParking>> entry : clusterIds.entrySet()) {
            MeanCluster newOne = new MeanCluster();
            newOne.list = entry.getValue();
            clusters.add(newOne);
        }

        //peut-on fusionner deux clusters ?
        for(int i = 0; i < clusters.size(); i++) {
            for(int j = i + 1; j < clusters.size(); j++) {
                MeanCluster cluster1 = clusters.get(i);
                MeanCluster cluster2 = clusters.get(j);

                Location one = new Location("generated");
                one.setLatitude(cluster1.getPosition().latitude);
                one.setLongitude(cluster1.getPosition().longitude);
                Location two = new Location("generated");
                two.setLatitude(cluster2.getPosition().latitude);
                two.setLongitude(cluster2.getPosition().longitude);

                Log.v("Algorithm", "Distance inter cluster is "+one.distanceTo(two));

                if(one.distanceTo(two) < zoomSpecificSpan * 6) {
                    //fusionner
                    clusters.remove(cluster2);
                    cluster1.list.addAll(cluster2.getItems());

                    //revenir au début du regroupement
                    i = -1;
                    break;
                }
            }
        }

        //la transformer en set en explosant les clusters qui ne sont pas assez denses
        Set<Cluster<PlaceParking>> clusterSet = new HashSet<>();
        for(MeanCluster c: clusters) {
            if(c.isADistanceLowerThan(zoomSpecificSpan)) {
                clusterSet.add(c);
            } else {
                for(PlaceParking p: c.list) {
                    MeanCluster monoCluster = new MeanCluster();
                    monoCluster.list.add(p);
                    clusterSet.add(monoCluster);
                }
            }
        }

        Log.d("Algorithm", "End clustering. It took "+(System.currentTimeMillis() - time)+".");

        return clusterSet;
    }

    private static class MeanCluster implements Cluster<PlaceParking> {
        public ArrayList<PlaceParking> list = new ArrayList<>();

        @Override
        public LatLng getPosition() {
            double latitude = 0, longitude = 0;

            for(PlaceParking p: list) {
                latitude += p.getLatitude();
                longitude += p.getLongitude();
            }

            latitude /= list.size();
            longitude /= list.size();

            return new LatLng(latitude, longitude);
        }

        @Override
        public Collection<PlaceParking> getItems() {
            return list;
        }

        @Override
        public int getSize() {
            return list.size();
        }

        public boolean isADistanceLowerThan(double distance) {
            for(int i = 0; i < list.size(); i++) {
                for(int j = i+1; j < list.size(); j++) {
                    PlaceParking p1 = list.get(i);
                    PlaceParking p2 = list.get(j);

                    if(p1.getDistanceFromPoint(p2.getPosition()) < distance) return true;
                }
            }

            return false;
        }
    }

    @Override
    public Collection<PlaceParking> getItems() {
        return list;
    }
}
