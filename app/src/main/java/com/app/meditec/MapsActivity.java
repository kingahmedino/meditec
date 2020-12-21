package com.app.meditec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.app.meditec.databinding.ActivityMapBinding;
import com.app.meditec.databinding.PlaceInfoBottomSheetBinding;
import com.app.meditec.models.PlaceInfo;
import com.app.meditec.ui.MapsViewModel;
import com.app.meditec.utils.PermissionUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionUtilsListener {
    private static final String TAG = "MapsActivity";
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 102;
    public static final float DEFAULT_ZOOM = 15f;
    public static final int GPS_REQUEST_CODE = 189;
    private boolean mLocationPermissionGranted = false;
    private LocationCallback mLocationCallback;
    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mCurrentLocation;
    private static List<PlaceInfo> mPlaceInfoList;
    private BottomSheetBehavior mBottomSheetBehavior;
    private ImageView mHeaderArrow;
    private MapsViewModel mMapsViewModel;
    private ActivityMapBinding mBinding;
    private PlaceInfoBottomSheetBinding mBottomSheetBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_map);
        PermissionUtils.INSTANCE.setListener(this);
        mMapsViewModel = new ViewModelProvider(this).get(MapsViewModel.class);
        mBottomSheetBinding = mBinding.bottomSheet;
        mLocationPermissionGranted = PermissionUtils.INSTANCE.getLocationPermission(this);
        if (mLocationPermissionGranted)
            initializeMap();

        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        mPlaceInfoList = new ArrayList<>();
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetBinding.bottomSheet);
        mHeaderArrow = findViewById(R.id.header_arrow);
        headerImageClickListener();
        bottomSheetBehaviourCallback();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mLocationPermissionGranted)
            PermissionUtils.INSTANCE.checkIfGPSIsEnabled(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapsViewModel.getPlacesLiveData().observe(this, new Observer<List<PlaceInfo>>() {
            @Override
            public void onChanged(List<PlaceInfo> placeInfos) {
                mPlaceInfoList = placeInfos;
                for (int i = 0; i < mPlaceInfoList.size(); i++) {
                    LatLng latLng = new LatLng(mPlaceInfoList.get(i).getGeometry().getLocation().getLat(),
                            mPlaceInfoList.get(i).getGeometry().getLocation().getLng());
                    MarkerOptions mMarkerOptions = new MarkerOptions()
                            .title(mPlaceInfoList.get(i).getName())
                            .position(latLng);
                    mGoogleMap.addMarker(mMarkerOptions);
                }
            }
        });
    }

    @Override
    public void locationGranted() {
        getDeviceLocation();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        mGoogleMap = googleMap;

        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d(TAG, "Marker is clicked: " + marker.getTitle());
                Log.d(TAG, "on Marker Click: " + mPlaceInfoList.size());
                boolean isPlaceInList = getPlaceDetails(marker.getPosition());
                if (isPlaceInList) {
                    if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                return false;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFusedLocationProviderClient != null)
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    private boolean getPlaceDetails(LatLng position) {
        for (PlaceInfo placeInfo : mPlaceInfoList) {
            LatLng place = new LatLng(placeInfo.getGeometry().getLocation().getLat(),
                    placeInfo.getGeometry().getLocation().getLng());
            if (place.equals(position)) {
                mBottomSheetBinding.setPlaceInfo(placeInfo);
                return true;
            }
        }
        return false;
    }

    private void initializeMap() {
        Log.d(TAG, "initializeMap");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getting device current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionGranted) {
                Task locationTask = mFusedLocationProviderClient.getLastLocation();
                locationTask.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            mCurrentLocation = (Location) task.getResult();
                            if (mCurrentLocation != null) {
                                moveCamera(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                                mMapsViewModel.getPlaces(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                            } else {
                                requestNewLocation();
                            }
                        } else {
                            Toast.makeText(MapsActivity.this, "Unable to find location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "get device current location. SecurityException:" + e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocation() {
        final LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    mCurrentLocation = locationResult.getLastLocation();
                    moveCamera(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                    mMapsViewModel.getPlaces(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                }
            }
        };
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
    }

    private void moveCamera(LatLng latLng) {
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, MapsActivity.DEFAULT_ZOOM));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                getDeviceLocation();
            } else {
                Log.d(TAG, "user ignored GPS alert");
                Toast.makeText(this, "Keep your GPS enabled", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult called !");
        mLocationPermissionGranted = false;

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "onRequestPermissionsResult failed");
                        Toast.makeText(this, "Location permissions needed to show maps", Toast.LENGTH_LONG).show();
                        mLocationPermissionGranted = false;
                        finish();
                        return;
                    }
                }
                Log.d(TAG, "onRequestPermissionsResult granted");
                mLocationPermissionGranted = true;
                initializeMap();
                getDeviceLocation();
            }
        }
    }

    /*-----------------------  bottom sheet -----------------------*/
    private void bottomSheetBehaviourCallback() {
        mBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mHeaderArrow.setRotation(slideOffset * 180);
            }
        });
    }

    private void headerImageClickListener() {
        mHeaderArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                else
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

}