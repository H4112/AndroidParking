package com.h4112.androidparking;

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
 */
public class DensityBasedAlgorithm implements Algorithm<PlaceParking> {
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
        final double zoomSpecificSpan = 40000000 / Math.pow(2, zoom) / 30;
        System.out.println(zoomSpecificSpan);

        int lastId = 0;
        HashMap<PlaceParking, Integer> clusterIds = new HashMap<>();
        for(PlaceParking p: list) clusterIds.put(p, 0);

        for(PlaceParking p: list) {
            boolean tookId = false;
            boolean hasNeighbor = false;

            for(PlaceParking p2: list) {
                if(p.getDistanceFromPoint(p2.getCoord()) < zoomSpecificSpan) {
                    hasNeighbor = true;

                    if(clusterIds.get(p2) != 0) {
                        clusterIds.put(p, clusterIds.get(p2));
                        tookId = true;
                    }
                }
            }
            if(!tookId && hasNeighbor) clusterIds.put(p, ++lastId);
        }

        ArrayList<MeanCluster> parks = new ArrayList<>();
        for(int i = 0; i < lastId; i++) parks.add(new MeanCluster());

        for(Map.Entry<PlaceParking, Integer> entry : clusterIds.entrySet()) {
            if(entry.getValue() != 0) {
                parks.get(entry.getValue() - 1).list.add(entry.getKey());
            } else {
                MeanCluster monoCluster = new MeanCluster();
                monoCluster.list.add(entry.getKey());
                parks.add(monoCluster);
            }
        }

        Set<Cluster<PlaceParking>> clusters = new HashSet<>();
        for(MeanCluster c: parks) clusters.add(c);

        return clusters;
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
    }

    @Override
    public Collection<PlaceParking> getItems() {
        return list;
    }
}
