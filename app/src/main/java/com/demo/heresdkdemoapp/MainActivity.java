package com.demo.heresdkdemoapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.heresdkdemoapp.helper.SearchMapView;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapviewlite.MapScene;
import com.here.sdk.mapviewlite.MapStyle;
import com.here.sdk.mapviewlite.MapViewLite;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private EditText editSearchName;
    private Button btnSearch;
    private MapViewLite map;
    private final int LOCATION_PERMISSION = 101;
    private SearchMapView searchMapView;

    protected LocationListener locationListener;
    protected LocationManager locationManager;

    protected double currentLat, currentLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        initialPermission();


        editSearchName = (EditText) findViewById(R.id.txtPlace);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        map = (MapViewLite) findViewById(R.id.mapView);
        map.onCreate(savedInstanceState);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchVal = editSearchName.getText().toString().trim();
                if(searchVal != null){
                    //Search for particular type
                      // searchMapView.searchPlace(searchVal);

                    //Search by Category
                    searchMapView.searchPlaceCategory(searchVal);



                }else {
                    Toast.makeText(getApplicationContext(), "Please enter Place to search", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }


    private void initialPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            }, LOCATION_PERMISSION);
        } else {
            Toast.makeText(this, "Location permission Granted", Toast.LENGTH_SHORT).show();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            loadMapView();
        }
    }

    private void loadMapView(){
        //Load Map view

        map.getMapScene().loadScene(MapStyle.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapScene.ErrorCode errorCode) {
                if(errorCode == null){
                    searchMapView = new SearchMapView(MainActivity.this,map, currentLat, currentLong);
                }else {
                    Toast.makeText(getApplicationContext(),"Error on LoadScene "+ errorCode,Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\n Please grant")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                initialPermission();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MainActivity.this.finish();
                            }
                        }).show();
            }
        }
        if(requestCode == LOCATION_PERMISSION) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permisson Granted",Toast.LENGTH_LONG).show();
                loadMapView();

            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLat = location.getLatitude();
        currentLong = location.getLongitude();

    }
}