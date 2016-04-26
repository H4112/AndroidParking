package com.h4112.androidparking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.example.googlemaps.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener  {
    private static final int UPDATE_INTERVAL = 10000;
    private static final int FASTEST_UPDATE_INTERVAL = 5000;

    private final int HAUTEUR_DP_BOUTON_LOCATION=48;

    private final int ITEM_PARAMETRES = 0;
    private final int ITEM_GARE = 1;
    private final int ITEM_COMPTE = 2;

    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    private boolean mapViewInitialized = false;
    private LatLng myLocation;
    private List<PlaceParking> listePlaces;

    private static final String SAVE_MAP_INIT = "mapViewInitialized";

    private ActionBarDrawerToggle toggle;

    private String[] MENU_OPTIONS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MENU_OPTIONS = new String[]{
                getString(R.string.option_location_settings),
                getString(R.string.option_location_gare),
                getString(R.string.option_location_account)
        };

        setContentView(R.layout.activity_maps);

        if(savedInstanceState != null) {
            mapViewInitialized = savedInstanceState.getBoolean(SAVE_MAP_INIT);
        }

        initParkingList();
        initPlayServices();
        initDrawer();
        initDrawerList();

        FloatingActionButton navigate = (FloatingActionButton) findViewById(R.id.boutonItineraire);
        if (navigate != null) {
            navigate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationButtonClicked();
                }
            });
        }
    }

    private void navigationButtonClicked() {
        Log.i("MainActivity", "TODO: navigate!");
    }

    public void initParkingList(){
        listePlaces = new ArrayList<>();

        //TODO: Récupérer les places
        PlaceParking place1 = new PlaceParking("id", PlaceParking.OCCUPEE, 45.768067f,4.8677357f);
        PlaceParking place2 = new PlaceParking("id", PlaceParking.LIBRE, 45.768080f,4.8677340f);

        listePlaces.add(place1);
        listePlaces.add(place2);

        //displayParkInsideRadius(liste, 10000000f);
    }

    public PlaceParking findBestPlace(){
        //Version naïve: Chemin le plus court à vol d'oiseau

        PlaceParking bestPlace = null;
        double minDistance = findFirstDistanceFreePlace();
        for(PlaceParking place : listePlaces){
            if(place.getDistanceFromPoint(myLocation) <= minDistance && place.getEtat()!=PlaceParking.OCCUPEE) {
                bestPlace = place;
            }
        }
        return bestPlace;
    }

    public double findFirstDistanceFreePlace(){
        for(PlaceParking place : listePlaces){
            if(place.getEtat()!=PlaceParking.OCCUPEE) {
                return place.getDistanceFromPoint(myLocation);
            }
        }
        return 0;
    }

    /**
     * Configure la ListView présente dans le tiroir de navigation.
     */
    private void initDrawerList() {
        List<Map<String, String>> options = new ArrayList<>();
        for(String name : MENU_OPTIONS) {
            options.add(getNameOnlyMap(name));
        }

        ListView optionsList = (ListView) findViewById(R.id.optionsListView);
        if (optionsList != null) {
            optionsList.setAdapter(new SimpleAdapter(this, options, R.layout.maps_drawer_list_item,
                    new String[] { "name" }, new int[] { R.id.text1 }));

            optionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onDrawerItemSelected(position);
                }
            });
        }
    }

    /**
     * Renvoie une nouvelle Map avec seulement un champ "name" renseigné.
     * @param name Le nom à renseigner
     * @return La nouvelle map créée
     */
    private Map<String, String> getNameOnlyMap(String name) {
        HashMap<String, String> map = new HashMap<>();
        map.put("name", name);
        return map;
    }

    /**
     * Lance les services Google Play (localisation et carte).
     */
    private void initPlayServices() {
        try {
            if (googleMap == null) {
                ((MapFragment) getFragmentManager().
                        findFragmentById(R.id.map)).getMapAsync(this);
            }

            //initialisation du Google API Client permettant de récupérer la géolocalisation
            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    /**
     * Met en place le toggle du tiroir de navigation.
     */
    private void initDrawer() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.nav_drawer_open, R.string.nav_drawer_closed);
        if (drawer != null) {
            drawer.addDrawerListener(toggle);
        }
        toggle.syncState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_MAP_INIT, mapViewInitialized);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void onMapReady(GoogleMap map) {
        googleMap = map;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setPadding(0, dpToPx(HAUTEUR_DP_BOUTON_LOCATION), 0, 0);

        // Généré automatiquement
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(true);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_search:
                return true;

            case R.id.action_find_place:
                displayPark(findBestPlace());
                return true;

            default:
                return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
        }
    }

    private void onDrawerItemSelected(int position) {
        Log.d("MainActivity", "Chose position " + position);
        switch(position){
            case ITEM_PARAMETRES:
                Intent activitySettings = new Intent(MainActivity.this, Settings.class);
                startActivity(activitySettings);
                break;

            case ITEM_GARE:
                Toast.makeText(this, R.string.location_vehicule, Toast.LENGTH_LONG).show();
                break;

            case ITEM_COMPTE:
                //TODO
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("MainActivity", "Connexion à Google API établie");

        //vérifier si sous Android 6 l'autorisation d'accès à la position est accordée
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.need_rights, Toast.LENGTH_LONG).show();

            return;
        }
        //obtenir une première position
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            onLocationChanged(mLastLocation);
        } else {
            Log.w("MainActivity", "Dernière localisation = null");
        }

        //demander des mises à jour régulières
        LocationRequest req = new LocationRequest();
        req.setInterval(UPDATE_INTERVAL);
        req.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, req, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w("MainActivity", "Connexion à Google API suspendue");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v("MainActivity", "Got a new location: " + location.getLatitude() + " " + location.getLongitude());

        if(googleMap != null && !mapViewInitialized) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            myLocation = new LatLng(latitude, longitude);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(myLocation, 15);
            googleMap.animateCamera(yourLocation);

            mapViewInitialized = true;
        } else if(googleMap == null) {
            Log.w("MainActivity", "googleMap = null");
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, R.string.unable_to_locate, Toast.LENGTH_LONG).show();
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public void displayParkInsideRadius(List<PlaceParking> listPark, float radius){
        for(PlaceParking place : listPark){
            if(place.isInsideCircle(myLocation, radius)){
                displayPark(place);
            }
        }
    }

    private void displayPark(PlaceParking place){
        String infos;
        if(place != null){
            if(place.getEtat()==PlaceParking.OCCUPEE || place.getEtat()==PlaceParking.EN_MOUVEMENT){
                infos = "Place occupée depuis "+place.getTempsOccupee();
            }
            else{
                infos = "Place libre depuis "+place.getTempsLibre();
            }
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(place.getCoord())
                    .title(place.getEtatString())
                    .snippet("Place")
                    .icon(place.getIcone()));
        }
    }
}