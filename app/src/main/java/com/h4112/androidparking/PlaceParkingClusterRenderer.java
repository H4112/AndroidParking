package com.h4112.androidparking;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.google.maps.android.ui.SquareTextView;

/**
 * Objet permettant d'afficher les clusters de manière correcte dans l'application.
 * Provient en partie du code source de android-maps-utils : https://github.com/googlemaps/android-maps-utils
 */
public class PlaceParkingClusterRenderer extends DefaultClusterRenderer<PlaceParking> {
    private final IconGenerator mIconGenerator;
    private ShapeDrawable mColoredCircleBackground;
    private final float mDensity;

    public PlaceParkingClusterRenderer(Context context, GoogleMap map, ClusterManager<PlaceParking> clusterManager) {
        super(context, map, clusterManager);

        mDensity = context.getResources().getDisplayMetrics().density;

        mIconGenerator = new IconGenerator(context);
        mIconGenerator.setContentView(makeSquareTextView(context));
        mIconGenerator.setTextAppearance(com.google.maps.android.R.style.ClusterIcon_TextAppearance);
        mIconGenerator.setBackground(makeClusterBackground());
    }

    /**
     * Called before the marker for a ClusterItem is added to the map.
     */
    protected void onBeforeClusterItemRendered(PlaceParking item, MarkerOptions markerOptions) {
        markerOptions.icon(item.getIcone());
        markerOptions.anchor(0.5f, 0.5f);
    }

    /**
     * Called before the marker for a Cluster is added to the map.
     * The default implementation draws a circle with a rough count of the number of items.
     */
    protected void onBeforeClusterRendered(Cluster<PlaceParking> cluster, MarkerOptions markerOptions) {
        //calculer la couleur moyenne, en faisant la somme puis en divisant par le nombre d'éléments
        //pour chacune des 3 composantes
        int red = 0, green = 0, blue = 0;
        for(PlaceParking park : cluster.getItems()) {
            int color = park.getColor();
            red += Color.red(color);
            green += Color.green(color);
            blue += Color.blue(color);
        }
        int count = cluster.getSize();
        red /= count;
        green /= count;
        blue /= count;

        int meanColor = Color.rgb(red, green, blue);

        mColoredCircleBackground.getPaint().setColor(meanColor);
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(mIconGenerator.makeIcon(getClusterText(cluster)));

        markerOptions.icon(descriptor);
    }

    private String getClusterText(Cluster<PlaceParking> cluster) {
        int nbPlacesLibres = 0, nbPlacesDepart = 0, nbPlacesOccupees = 0, nbPlacesInconnues = 0;

        for(PlaceParking p : cluster.getItems()) {
            switch(p.getEtat()) {
                case LIBRE:
                    nbPlacesLibres++;
                    break;
                case OCCUPEE:
                    nbPlacesOccupees++;
                    break;
                case EN_MOUVEMENT:
                    nbPlacesDepart++;
                    break;
                case INCONNU:
                    nbPlacesInconnues++;
                    break;
            }
        }

        if(nbPlacesInconnues == cluster.getSize()) {
            return "?";
        } else if(nbPlacesDepart != 0) {
            return nbPlacesLibres + " (+" + nbPlacesDepart + ")";
        } else {
            return nbPlacesLibres+"";
        }
    }

    private LayerDrawable makeClusterBackground() {
        mColoredCircleBackground = new ShapeDrawable(new OvalShape());
        ShapeDrawable outline = new ShapeDrawable(new OvalShape());
        outline.getPaint().setColor(0x80ffffff); // Transparent white.
        LayerDrawable background = new LayerDrawable(new Drawable[]{outline, mColoredCircleBackground});
        int strokeWidth = (int) (mDensity * 3);
        background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);
        return background;
    }

    private SquareTextView makeSquareTextView(Context context) {
        SquareTextView squareTextView = new SquareTextView(context);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        squareTextView.setLayoutParams(layoutParams);
        squareTextView.setId(com.google.maps.android.R.id.text);
        int twelveDpi = (int) (12 * mDensity);
        squareTextView.setPadding(twelveDpi, twelveDpi, twelveDpi, twelveDpi);

        return squareTextView;
    }
}
