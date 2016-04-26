package com.h4112.androidparking;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener  {
    private static final int UPDATE_INTERVAL = 10000;
    private static final int FASTEST_UPDATE_INTERVAL = 5000;

    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    private boolean mapViewInitialized = false;

    private static final String SAVE_MAP_INIT = "mapViewInitialized";

    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if(savedInstanceState != null) {
            mapViewInitialized = savedInstanceState.getBoolean(SAVE_MAP_INIT);
        }

        initPlayServices();
        initDrawer();
    }

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

            case R.id.action_settings:
                Intent activitySettings = new Intent(MainActivity.this, Settings.class);
                startActivity(activitySettings);
                return true;

            case R.id.action_quit:
                finish();
                return true;

            default:
                return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
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
        Log.v("MainActivity", "Got a new location: "+location.getLatitude()+" "+location.getLongitude());

        if(googleMap != null && !mapViewInitialized) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng myPosition = new LatLng(latitude, longitude);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(myPosition, 15);
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
}