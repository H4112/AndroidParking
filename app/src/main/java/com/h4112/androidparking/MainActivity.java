package com.h4112.androidparking;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener  {
    private static final int UPDATE_INTERVAL = 10000;
    private static final int FASTEST_UPDATE_INTERVAL = 5000;

    private static final int ITEM_PARAMETRES = 0;
    private static final int ITEM_GARE = 1;
    private static final int ITEM_COMPTE = 2;

    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;

    private boolean mapViewInitialized = false;
    private LatLng myLocation = null;
    private ArrayList<PlaceParking> listePlaces = new ArrayList<>();
    private PlaceParking selectedPark = null;

    private Handler handler = new Handler();
    private Runnable update = null;

    private static final String SAVE_MAP_INIT = "mapViewInitialized";
    private static final String SAVE_FIRST_SPOT_LIST = "gotFirstSpotList";
    private static final String SAVE_SPOT_LIST = "listePlaces";
    private static final String SAVE_MY_LOCATION = "myLocation";
    private static final String SAVE_SELECTED_PARK = "selectedPark";

    private ActionBarDrawerToggle toggle;
    private SearchView searchview;

    private String[] MENU_OPTIONS;
    private boolean gotFirstSpotList = false;

    private TextView adresse;
    private TextView etat;
    private TextView tempsLibreOccupee;
    private TextView tempsLibreOccupeeTexte;
    private TextView distance;
    private SlidingUpPanelLayout panelLayout;
    private DrawerLayout drawer;
    private FloatingActionButton itinerary;
    ClusterManager<PlaceParking> mClusterManager;

    private Marker markerSelectedPark;

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
            gotFirstSpotList = savedInstanceState.getBoolean(SAVE_FIRST_SPOT_LIST);
            listePlaces = savedInstanceState.getParcelableArrayList(SAVE_SPOT_LIST);
            myLocation = savedInstanceState.getParcelable(SAVE_MY_LOCATION);
            selectedPark = savedInstanceState.getParcelable(SAVE_SELECTED_PARK);
        }

        initPlayServices();
        initDrawer();
        initDrawerList();
        initSlidingUpPanel();

        searchview = (SearchView) findViewById(R.id.search);
        if (searchview != null) {
            searchview.setVisibility(View.GONE);
            searchview.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    searchview.setVisibility(View.GONE);
                    return false;
                }
            });
        }

        itinerary = (FloatingActionButton) findViewById(R.id.boutonItineraire);
        if (itinerary != null) {
            itinerary.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationButtonClicked();
                }
            });
        }

        adresse = (TextView)findViewById(R.id.adresse);
        etat = (TextView)findViewById(R.id.etat);
        tempsLibreOccupee = (TextView)findViewById(R.id.tempsLibreOccupee);
        tempsLibreOccupeeTexte = (TextView)findViewById(R.id.texteTempsLibreOccupee);
        distance = (TextView)findViewById(R.id.distance);

        setScrollablePanelInvisible();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_MAP_INIT, mapViewInitialized);
        outState.putBoolean(SAVE_FIRST_SPOT_LIST, gotFirstSpotList);
        outState.putParcelableArrayList(SAVE_SPOT_LIST, listePlaces);
        outState.putParcelable(SAVE_MY_LOCATION, myLocation);
        outState.putParcelable(SAVE_SELECTED_PARK, selectedPark);
    }

    private void navigationButtonClicked() {
        Log.d("MainActivity", "From " + Double.toString(myLocation.latitude) + ", " + Double.toString(myLocation.longitude) + " to " +
                Float.toString(selectedPark.getLatitude()) + ", " + Float.toString(selectedPark.getLongitude()));

        final AlertDialog popup = new AlertDialog.Builder(this)
                .setMessage("Voulez-vous lancer la navigation ?")
                .setPositiveButton("Oui (5)", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchNavigation();
                    }
                })
                .setNegativeButton("Non", null)
                .setTitle("Votre place a été réservée")
                .show();

        new CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                popup.getButton(DialogInterface.BUTTON_POSITIVE).setText("Oui ("+(millisUntilFinished/1000)+")");
            }

            @Override
            public void onFinish() {
                if(popup.isShowing()) {
                    launchNavigation();
                    popup.dismiss();
                }
            }
        }.start();

    }

    public void launchNavigation(){
        //TODO
        Log.d("MainActivity", "--- Lancement de la navigation ---");

        String   mode = "&mode=c";
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse(String.format("google.navigation:ll=%s,%s%s",
                        markerSelectedPark.getPosition().latitude, markerSelectedPark.getPosition().longitude, mode)));
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void setListePlaces(ArrayList<PlaceParking> parkingList){
        listePlaces = parkingList;

        displayAllParkingSpots(listePlaces);

        gotFirstSpotList = true;
    }

    public void listePlacesFailure() {
        if(!gotFirstSpotList) {
            stopUpdateTimer();

            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.error_getting_parks))
                    .setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startUpdateTimer();
                        }
                    })
                    .setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private PlaceParking findBestPlace(){
        //Version naïve: Chemin le plus court à vol d'oiseau

        PlaceParking bestPlace = null;
        double minDistance = findFirstDistanceFreePlace();
        for(PlaceParking place : listePlaces){
            if(place.getDistanceFromPoint(myLocation) <= minDistance && place.getEtat()!=PlaceParking.Etat.OCCUPEE) {
                bestPlace = place;
                minDistance = place.getDistanceFromPoint(myLocation);
            }
        }
        return bestPlace;
    }

    private double findFirstDistanceFreePlace(){
        for(PlaceParking place : listePlaces){
            if(place.getEtat()!=PlaceParking.Etat.OCCUPEE) {
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
                    new String[]{"name"}, new int[]{R.id.text1}));

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

    private void initSlidingUpPanel() {
        panelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        if(panelLayout != null) {
            panelLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
                @Override
                public void onPanelSlide(View view, float v) {

                }

                @Override
                public void onPanelCollapsed(View view) {
                    adresse.setSingleLine(true);
                }

                @Override
                public void onPanelExpanded(View view) {
                    adresse.setSingleLine(false);
                }

                @Override
                public void onPanelAnchored(View view) {
                    adresse.setSingleLine(false);
                }

                @Override
                public void onPanelHidden(View view) {

                }

                @Override
                public void onPanelHiddenExecuted(View view, Interpolator interpolator, int i) {

                }

                @Override
                public void onPanelShownExecuted(View view, Interpolator interpolator, int i) {

                }

                @Override
                public void onPanelExpandedStateY(View view, boolean b) {

                }

                @Override
                public void onPanelCollapsedStateY(View view, boolean b) {

                }

                @Override
                public void onPanelLayout(View view, SlidingUpPanelLayout.PanelState panelState) {

                }
            });
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

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.nav_drawer_open, R.string.nav_drawer_closed);
        if (drawer != null) {
            drawer.addDrawerListener(toggle);
        }
        toggle.syncState();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();

        startUpdateTimer();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();

        stopUpdateTimer();
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(panelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED
                ||panelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) {
            panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else if(panelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
            resetParkingData();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                searchview.setIconified(false);
                if(searchview.getVisibility() == View.VISIBLE) {
                    searchview.setVisibility(View.GONE);
                    searchview.setQuery("", false);
                } else {
                    searchview.setVisibility(View.VISIBLE);
                }

                return true;

            case R.id.action_find_place:
                if(markerSelectedPark!=null){
                    markerSelectedPark.remove();
                }
                selectedPark = findBestPlace();
                MarkerOptions markerOptionsSelectedPark = new MarkerOptions()
                        .position(selectedPark.getCoord());
                markerSelectedPark = googleMap.addMarker(markerOptionsSelectedPark);
                updateParkingData();
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

        boolean myLocationWasNull = (myLocation == null);

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        myLocation = new LatLng(latitude, longitude);

        if(myLocationWasNull) {
            startUpdateTimer();
        }

        if(googleMap != null && !mapViewInitialized) {
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

    public void onMapReady(GoogleMap map) {
        googleMap = map;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);

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

        mClusterManager = new ClusterManager<>(this, googleMap);
        mClusterManager.setRenderer(new PlaceParkingClusterRenderer(this, googleMap, mClusterManager));

        googleMap.setOnCameraChangeListener(mClusterManager);
        googleMap.setOnMarkerClickListener(mClusterManager);

        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<PlaceParking>() {
            @Override
            public boolean onClusterItemClick(PlaceParking placeParking) {
                actionMarkerClick(placeParking);
                return true;
            }
        });

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                resetParkingData();
            }
        });

        displayAllParkingSpots(listePlaces);
    }

    public void actionMarkerClick(PlaceParking placeParking){
        if(markerSelectedPark!=null){
            markerSelectedPark.remove();
        }
        MarkerOptions markerOptionsSelectedPark = new MarkerOptions()
                .position(placeParking.getPosition());
        markerSelectedPark = googleMap.addMarker(markerOptionsSelectedPark);
        selectedPark = placeParking;

        updateParkingData();

        boolean occupe = (selectedPark != null ? selectedPark.getEtat() : null) == PlaceParking.Etat.OCCUPEE;
        itinerary.setVisibility(occupe ? View.INVISIBLE : View.VISIBLE);
    }

    private void updateParkingData(){
        setScrollablePanelVisible();
        if(selectedPark != null){
            adresse.setText(selectedPark.getAddress());
            etat.setText(selectedPark.getEtatString());
            if(selectedPark.getEtat() != PlaceParking.Etat.OCCUPEE) {
                tempsLibreOccupeeTexte.setText(R.string.free_since);
            }
            else if(selectedPark.getEtat() == PlaceParking.Etat.OCCUPEE) {
                tempsLibreOccupeeTexte.setText(R.string.busy_since);
            }
            tempsLibreOccupee.setText(selectedPark.getDurationString(this));

            double dist = (int) selectedPark.getDistanceFromPoint(new LatLng(myLocation.latitude, myLocation.longitude));

            if(dist > 1000) {
                distance.setText(getString(R.string.kilometers, new DecimalFormat("0.0").format(dist/1000)));
            } else {
                distance.setText(getString(R.string.meters, (int) (Math.round(dist / 10) * 10)));
            }
        }
        else{
            Log.d("MainActivity", "Aucune place selectionnée");
        }
    }

    private void resetParkingData(){
        selectedPark = null;
        if (markerSelectedPark != null) {
            markerSelectedPark.remove();
        }
        setScrollablePanelInvisible();
    }

    private void setScrollablePanelVisible(){
        itinerary.setVisibility(View.VISIBLE);
        if(panelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN)
            panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    private void setScrollablePanelInvisible(){
        itinerary.setVisibility(View.INVISIBLE);
        panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    private void displayAllParkingSpots(List<PlaceParking> listPark){
        googleMap.clear();
        mClusterManager.clearItems();

        for(PlaceParking place : listPark){
            displayPark(place);
        }
        mClusterManager.cluster();

        if(selectedPark != null) {
            boolean found = false;

            for(PlaceParking p : listePlaces) {
                if(p.getId() == selectedPark.getId()) {
                    Log.v("MainActivity", "Auto-Selected parking spot");
                    actionMarkerClick(p);
                    found = true;
                    break;
                }
            }

            if(!found) {
                Log.i("MainActivity", "Selected park not found: Unselecting");
                resetParkingData();
            }
        }
    }

    private void displayPark(PlaceParking place){
        if(place != null){
            mClusterManager.addItem(place);
        }
    }

    private void startUpdateTimer() {
        startUpdateTimer(0);
    }

    private void startUpdateTimer(int delay) {
        Log.d("MainActivity", "Update timer STARTED! Delay "+delay);

            update = new Runnable() {
                @Override
                public void run() {
                    if (myLocation != null) {
                        runUpdatePlaces();
                        startUpdateTimer(10000);
                    } else {
                        Log.d("MainActivity", "myLocation = null, timer stopped");
                    }
                }
            };

            handler.postDelayed(update, delay);
    }

    private void stopUpdateTimer() {
        Log.d("MainActivity", "Update timer STOPPED!");

        if(update != null) {
            handler.removeCallbacks(update);
        }
    }

    private void runUpdatePlaces() {
        FetchParkingSpotsTask task = new FetchParkingSpotsTask(this);
        task.execute(new FetchParkingSpotsTask.Params(myLocation.latitude, myLocation.longitude, 100));
    }
}