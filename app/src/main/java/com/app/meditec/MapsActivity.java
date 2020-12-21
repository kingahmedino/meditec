package com.app.meditec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
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
import com.google.android.gms.maps.model.MarkerOptions;
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

        createLocationCallback();
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
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (mLocationPermissionGranted)
            PermissionUtils.INSTANCE.checkIfGPSIsEnabled(this);
    }

    @SuppressLint("MissingPermission")
    private void setupLocationUpdatesListener() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
    }

    private void createLocationCallback() {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapsViewModel.getPlacesLiveData().observe(this, placeInfos -> {
            mPlaceInfoList = placeInfos;
            for (PlaceInfo placeInfo : mPlaceInfoList) {
                LatLng latLng = new LatLng(placeInfo.getGeometry().getLocation().getLat(),
                        placeInfo.getGeometry().getLocation().getLng());
                MarkerOptions markerOptions = new MarkerOptions()
                        .title(placeInfo.getName())
                        .position(latLng);
                mGoogleMap.addMarker(markerOptions);
            }
        });

        mMapsViewModel.getPlacesResponseStatus().observe(this, placeResponseStatus -> {
            Toast.makeText(this, placeResponseStatus, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void GPSIsEnabled() {
        if (mLocationPermissionGranted)
            setupLocationUpdatesListener();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        mGoogleMap = googleMap;

        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

        mGoogleMap.setOnMarkerClickListener(marker -> {
            Log.d(TAG, "Marker is clicked: " + marker.getTitle());
            Log.d(TAG, "on Marker Click: " + mPlaceInfoList.size());
            boolean isPlaceInList = getPlaceDetails(marker.getPosition());
            if (isPlaceInList) {
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            return false;
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
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
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }

    private void moveCamera(LatLng latLng) {
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, MapsActivity.DEFAULT_ZOOM));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (mLocationPermissionGranted)
                    setupLocationUpdatesListener();
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
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
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
                PermissionUtils.INSTANCE.checkIfGPSIsEnabled(this);
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
        mHeaderArrow.setOnClickListener(v -> {
            if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            else
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

}