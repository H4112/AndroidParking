package com.h4112.androidparking;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener  {

    //constantes
    private static final int UPDATE_INTERVAL = 10000;
    private static final int FASTEST_UPDATE_INTERVAL = 5000;
    private static final int PARKING_UPDATE_INTERVAL = 5000;
    private static final int ITEM_PARAMETRES = 0;
    private static final int ITEM_GARE = 1;
    private static final int ITEM_COMPTE = 2;
    private String[] MENU_OPTIONS;

    //vues
    private GoogleMap googleMap;
    private TextView adresse;
    private TextView etat;
    private TextView tempsLibreOccupee;
    private TextView distance;
    private SlidingUpPanelLayout panelLayout;
    private DrawerLayout drawer;
    private FloatingActionButton itinerary;
    private ActionBarDrawerToggle toggle;
    private SearchView searchview;
    private Marker markerSelectedPark;
    private AlertDialog failDialog = null;
    private RelativeLayout progressBarLayout;

    //clusters
    private ClusterManager<PlaceParking> mClusterManager;
    private PlaceParkingClusterRenderer mClusterRenderer;

    //constantes de sauvegarde
    private static final String SAVE_MAP_INIT = "mapViewInitialized";
    private static final String SAVE_FIRST_SPOT_LIST = "gotFirstSpotList";
    private static final String SAVE_SPOT_LIST = "listePlaces";
    private static final String SAVE_MY_LOCATION = "myLocation";
    private static final String SAVE_SELECTED_PARK = "selectedPark";

    //variables sauvegardées
    private boolean mapViewInitialized = false;
    private LatLng myLocation = null;
    private ArrayList<PlaceParking> listePlaces = new ArrayList<>();
    private PlaceParking selectedPark = null;
    private boolean gotFirstSpotList = false;

    //variables non sauvegardées
    private PlaceParking reservedPark = null;
    private int radius;

    //autres variables
    private GoogleApiClient mGoogleApiClient;
    private Handler handler = new Handler();
    private Runnable update = null;
    private FetchParkingSpotsTask task;
    private GeocoderTask task2;
    private Runnable mapDragFollower = null;

    ////////////////////////// ACTIVITY LIFECYCLE //////////////////////////
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
        initSearchView();
        initFAB();

        adresse = (TextView)findViewById(R.id.adresse);
        etat = (TextView)findViewById(R.id.etat);
        tempsLibreOccupee = (TextView)findViewById(R.id.tempsLibreOccupee);
        distance = (TextView)findViewById(R.id.distance);
        progressBarLayout = (RelativeLayout) findViewById(R.id.progressBar);
        progressBarLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //on empêche l'utilisateur de cliquer
                return true;
            }
        });

        setScrollablePanelInvisible();

        if(getSupportActionBar() != null)
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.RED));
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        radius = preferences.getInt(getResources().getString(R.string.portee_cle), -1);

        if(radius == -1) {
            radius = 200;

            SharedPreferences.Editor edit = preferences.edit();
            edit.putInt(getResources().getString(R.string.portee_cle), radius);
            edit.apply();
        }

        Log.d("MainActivity", "RADIUS ---- " + radius);

        startUpdateTimer();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();

        stopUpdateTimer();
        if(failDialog != null) failDialog.dismiss();
        if(task2 != null) task2.cancel(true);
        if(mapDragFollower != null) handler.removeCallbacks(mapDragFollower);
    }

    ////////////////////////// LISTENERS //////////////////////////
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                //montrer / cacher la barre de recherche
                searchview.setIconified(false);
                if(searchview.getVisibility() == View.VISIBLE
                        && progressBarLayout.getVisibility() != View.VISIBLE) {
                    searchview.setVisibility(View.GONE);
                    searchview.setQuery("", false);
                } else {
                    searchview.setVisibility(View.VISIBLE);
                }

                return true;

            case R.id.action_find_place:
                //sélectionner la place la plus proche du centre de l'écran
                if(markerSelectedPark!=null){
                    markerSelectedPark.remove();
                }
                selectedPark = findBestPlace();
                if(selectedPark != null) {
                    MarkerOptions markerOptionsSelectedPark = new MarkerOptions()
                            .position(selectedPark.getPosition());
                    markerSelectedPark = googleMap.addMarker(markerOptionsSelectedPark);
                    updateParkingData();
                } else {
                    resetParkingData();
                }
                return true;

            default:
                return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
        }
    }

    ////////////////////////// METHODES GOOGLE API //////////////////////////

    @Override
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

        initClusterManager();

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                resetParkingData();
            }
        });

        displayAllParkingSpots(listePlaces);
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

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        myLocation = new LatLng(latitude, longitude);

        if(googleMap != null && !mapViewInitialized) {
            //zoomer sur la position si c'est la première fois
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


    ////////////////////////// INITIALISATION //////////////////////////

    /**
     * Met en place le bouton Naviguer et son écouteur.
     */
    private void initFAB() {
        itinerary = (FloatingActionButton) findViewById(R.id.boutonItineraire);
        if (itinerary != null) {
            itinerary.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationButtonClicked();
                }
            });
        }
    }

    /**
     * Met en place la barre de recherche et son écouteur (fermeture).
     */
    private void initSearchView() {
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
            searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    task2 = new GeocoderTask(MainActivity.this);
                    task2.execute(query);

                    progressBarLayout.setVisibility(View.VISIBLE);
                    searchview.clearFocus();

                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }
    }

    public void geocodingFinished(LatLng result) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(result, 15));

        progressBarLayout.setVisibility(View.GONE);

        searchview.setVisibility(View.GONE);
        searchview.setQuery("", false);
    }

    public void geocodingFailed(boolean requestEnded) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error))
                .setMessage(requestEnded ? getString(R.string.no_result) : getString(R.string.connect_error_geocoder))
                .setPositiveButton(getString(R.string.ok), null)
                .show();

        progressBarLayout.setVisibility(View.GONE);
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

    /**
     * Met en place le SlidingUpPanel et ses écouteurs.
     */
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


    ////////////////////////// CARTE //////////////////////////

    private ClusterManager.OnClusterItemClickListener<PlaceParking> clusterItemClickListener =
            new ClusterManager.OnClusterItemClickListener<PlaceParking>() {
                @Override
                public boolean onClusterItemClick(PlaceParking placeParking) {
                    actionMarkerClick(placeParking);
                    return true;
                }
            };

    private ClusterManager.OnClusterClickListener<PlaceParking> clusterClickListener =
            new ClusterManager.OnClusterClickListener<PlaceParking>() {
                @Override
                public boolean onClusterClick(Cluster<PlaceParking> cluster) {
                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

                    for(PlaceParking p : cluster.getItems()) {
                        if(minX > p.getLatitude()) minX = p.getLatitude();
                        if(minY > p.getLongitude()) minY = p.getLongitude();
                        if(maxX < p.getLatitude()) maxX = p.getLatitude();
                        if(maxY < p.getLongitude()) maxY = p.getLongitude();
                    }

                    LatLng upperleft = new LatLng(minX, minY);
                    LatLng lowerright = new LatLng(maxX, maxY);

                    int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            40, getResources().getDisplayMetrics());

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                            new LatLngBounds(upperleft, lowerright), px));

                    return true;
                }
            };

    /**
     * Met en place le ClusterManager/Renderer, ainsi que les écouteurs liés à des clics sur la carte.
     */
    private void initClusterManager() {
        mClusterManager = new ClusterManager<>(this, googleMap);
        mClusterRenderer = new PlaceParkingClusterRenderer(this, googleMap, mClusterManager);
        mClusterManager.setRenderer(mClusterRenderer);

        //mClusterManager.setAlgorithm(new RoadIdBasedAlgorithm());

        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mClusterManager.onCameraChange(cameraPosition);

                Log.v("MainActivity", "Map got dragged");

                if(mapDragFollower != null) handler.removeCallbacks(mapDragFollower);
                mapDragFollower = new Runnable() {
                    @Override
                    public void run() {
                        mapDragFollower = null;

                        Log.i("MainActivity", "Camera moved! I must refresh right now.");
                        stopUpdateTimer();
                        startUpdateTimer();
                    }
                };

                handler.postDelayed(mapDragFollower, 200);
            }
        });
        googleMap.setOnMarkerClickListener(mClusterManager);

        mClusterManager.setOnClusterItemClickListener(clusterItemClickListener);
        mClusterManager.setOnClusterClickListener(clusterClickListener);
    }

    /**
     * Appelé lorsque l'utilisateur appuie sur un marqueur.
     * @param placeParking Place de parking choisie
     */
    private void actionMarkerClick(PlaceParking placeParking){
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

    /**
     * Affiche toutes les places de parking données en paramètre.
     * @param listPark Liste des places à afficher
     */
    private void displayAllParkingSpots(List<PlaceParking> listPark){
        if(markerSelectedPark != null) markerSelectedPark.remove();

        mClusterManager.clearItems();
        for(PlaceParking place : listPark){
            displayPark(place);
        }
        for(Marker m : mClusterManager.getClusterMarkerCollection().getMarkers()) {
            Cluster<PlaceParking> cluster = mClusterRenderer.getCluster(m);
            if(cluster != null) {
                MarkerOptions options = new MarkerOptions();
                mClusterRenderer.onBeforeClusterRendered(cluster, options);
                m.setIcon(options.getIcon());
            }
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

    /**
     * Affiche une place de parking sur la carte.
     * @param place Place à afficher
     */
    private void displayPark(PlaceParking place){
        if(place != null){
            mClusterManager.addItem(place);

            Marker mark = mClusterRenderer.getMarker(place);
            if(mark != null) {
                mark.setIcon(place.getEtat() == PlaceParking.Etat.GRANDLYON ?
                        mClusterRenderer.getMarkerForGrandLyon(place) : place.getIcone());
            }
        }
    }

    ////////////////////////// RECUPERATION DES PLACES //////////////////////////

    /**
     * Lance immédiatement la méthode runUpdatePlaces, qui sera rappelée à intervalle régulier.
     */
    private void startUpdateTimer() {
        startUpdateTimer(0);
    }

    /**
     * Lance la méthode runUpdatePlaces avec le délai spécifié, qui sera rappelée à
     * intervalle régulier (en rappelant cette méthode).
     * @param delay Délai d'exécution de runUpdatePlaces
     */
    private void startUpdateTimer(int delay) {
        if(update != null) {
            handler.removeCallbacks(update);
        }

        Log.d("MainActivity", "Update timer STARTED! Delay "+delay);

        update = new Runnable() {
            @Override
            public void run() {
                runUpdatePlaces();
                startUpdateTimer(PARKING_UPDATE_INTERVAL);
            }
        };

        handler.postDelayed(update, delay);
    }

    /**
     * Annule l'exécution programmée de runUpdatePlaces.
     */
    private void stopUpdateTimer() {
        Log.d("MainActivity", "Update timer STOPPED!");

        if(update != null) {
            handler.removeCallbacks(update);
        }
        if(task != null) {
            task.cancel(true);
            task = null;
        }
    }

    /**
     * Lance la tâche de récupération des places de parking (FetchParkingSpotsTask).
     */
    private void runUpdatePlaces() {
        if(task != null) {
            Log.d("MainActivity", "Launch request -> Do not want");
            return;
        }

        Log.v("MainActivity", "RADIUS ---- "+radius);

        if(googleMap != null) {
            LatLng target = googleMap.getCameraPosition().target;

            task = new FetchParkingSpotsTask(this);
            task.execute(new FetchParkingSpotsTask.Params(target.latitude, target.longitude, radius));
        }
    }

    /**
     * Retour de FetchParkingSpotsTask : donne une nouvelle liste de parkings récupérée depuis
     * le serveur ou le cache.
     * @param parkingList Liste des places de parking
     */
    public void setListePlaces(ArrayList<PlaceParking> parkingList){
        task = null;

        listePlaces = parkingList;

        displayAllParkingSpots(listePlaces);

        gotFirstSpotList = true;
    }

    /**
     * Retour de FetchParkingSpotsTask : appelé lorsque la liste de parkings n'a pas pu être récupérée.
     */
    public void listePlacesFailure() {
        task = null;

        if(!gotFirstSpotList) {
            //aucune récupération n'a fonctionné depuis le lancement de l'application
            stopUpdateTimer();

            failDialog = new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.error_getting_parks))
                    .setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startUpdateTimer();
                        }
                    })
                    .setNegativeButton(getString(R.string.close), null)
                    .setCancelable(false)
                    .show();
        }
    }

    ////////////////////////// BOUTONS ACTION BAR //////////////////////////

    /**
     * Permet de trouver la meilleure place (la plus proche du centre de l'écran).
     * @return La place trouvée, ou null s'il n'y a pas de place.
     */
    private PlaceParking findBestPlace(){
        //Version naïve: Chemin le plus court à vol d'oiseau

        PlaceParking bestPlace = null;
        double minDistance = findFirstDistanceFreePlace();
        for(PlaceParking place : listePlaces){
            if(place.getDistanceFromPoint(googleMap.getCameraPosition().target) <= minDistance
                    && place.getEtat()!=PlaceParking.Etat.OCCUPEE
                    && place.getEtat()!=PlaceParking.Etat.INCONNU) {
                bestPlace = place;
                minDistance = place.getDistanceFromPoint(googleMap.getCameraPosition().target);
            }
        }
        return bestPlace;
    }

    /**
     * Trouve une distance par rapport à une place libre ou sur le point de l'être.
     * @return Une distance
     */
    private double findFirstDistanceFreePlace(){
        for(PlaceParking place : listePlaces){
            if(place.getEtat()!=PlaceParking.Etat.OCCUPEE && place.getEtat()!=PlaceParking.Etat.INCONNU) {
                return place.getDistanceFromPoint(googleMap.getCameraPosition().target);
            }
        }
        return 0;
    }


    ////////////////////////// TIROIR DE NAVIGATION //////////////////////////

    /**
     * Appelé lorsqu'un élément du tiroir de navigation est sélectionné.
     * @param position Position de l'item sélectionné
     */
    private void onDrawerItemSelected(int position) {
        Log.d("MainActivity", "Chose position " + position);
        switch(position){
            case ITEM_PARAMETRES:
                Intent activitySettings = new Intent(MainActivity.this, Settings.class);
                startActivity(activitySettings);
                break;

            case ITEM_GARE:
                if(reservedPark != null){
                    MarkerOptions markerOptionsSelectedPark = new MarkerOptions()
                            .position(reservedPark.getPosition());
                    markerSelectedPark = googleMap.addMarker(markerOptionsSelectedPark);
                }
                else {
                    Toast.makeText(this, R.string.location_vehicule, Toast.LENGTH_LONG).show();
                }
                break;

            case ITEM_COMPTE:
                //TODO
                break;
        }
    }



    ////////////////////////// SLIDING UP PANEL //////////////////////////

    /**
     * Affiche les informations sur la place de parking selectedPark dans le SlidingUpPanel.
     */
    private void updateParkingData(){
        setScrollablePanelVisible();
        if(selectedPark != null){
            adresse.setText(selectedPark.getAddress());
            etat.setText(selectedPark.getEtatString(this));
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

    /**
     * Désélectionne la place actuellement choisie, et cache les informations.
     */
    private void resetParkingData(){
        selectedPark = null;
        if (markerSelectedPark != null) {
            markerSelectedPark.remove();
        }
        setScrollablePanelInvisible();
    }

    /**
     * Permet de réserver la place et de lancer la navigation.
     */
    private void navigationButtonClicked() {
        Log.d("MainActivity", "From " + Double.toString(myLocation.latitude) + ", " + Double.toString(myLocation.longitude) + " to " +
                Float.toString(selectedPark.getLatitude()) + ", " + Float.toString(selectedPark.getLongitude()));

        //demander si on veut lancer la navigation
        final AlertDialog popup = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.run_navigation))
                .setPositiveButton(getString(R.string.yes_5), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchNavigation();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .setTitle(getString(R.string.reserved_spots))
                .show();

        //mettre en place le compte à rebours sélectionnant automatiquement "oui"
        new CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                popup.getButton(DialogInterface.BUTTON_POSITIVE).setText(getString(R.string.yes_countdown, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                if(popup.isShowing()) {
                    launchNavigation();
                    popup.dismiss();
                }
            }
        }.start();

        reservedPark = selectedPark;
    }

    /**
     * Lance la navigation Google Maps vers la destination (selectedPark).
     */
    private void launchNavigation(){
        Log.d("MainActivity", "--- Lancement de la navigation ---");

        String   mode = "&mode=c";
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse(String.format("google.navigation:ll=%s,%s%s",
                        markerSelectedPark.getPosition().latitude, markerSelectedPark.getPosition().longitude, mode)));
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    /**
     * Affiche le SlidingUpPanel.
     */
    private void setScrollablePanelVisible(){
        itinerary.setVisibility(View.VISIBLE);
        if(panelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN)
            panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    /**
     * Masque le SlidingUpPanel.
     */
    private void setScrollablePanelInvisible(){
        itinerary.setVisibility(View.INVISIBLE);
        panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }
}