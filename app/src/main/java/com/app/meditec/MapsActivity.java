package com.app.meditec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.meditec.helpers.DownloadPlacesFromUrl;
import com.app.meditec.helpers.PlaceInfoResponse;
import com.app.meditec.models.PlaceInfo;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.Gson;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MapsActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 102;
    public static final float DEFAULT_ZOOM = 15f;
    public static final int GPS_REQUEST_CODE = 189;
    public static final String NEAR_BY_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    private boolean mLocationPermissionGranted = false;
    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mCurrentLocation;
    private View mMapView;
    private MaterialSearchBar mSearchBar;
    private AutocompleteSessionToken mToken;
    private PlacesClient mPlacesClient;
    private List<AutocompletePrediction> mPredictionList;
    private static List<PlaceInfo> mPlaceInfoList;
    private ConstraintLayout mBottomSheetLayout;
    private BottomSheetBehavior mBottomSheetBehavior;
    private ImageView mHeaderArrow;
    private TextView mPlaceName;
    private TextView mPlaceAddress;
    private TextView mBusinessStatus;
    private TextView mIsOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getLocationPermission();

        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        mPlacesClient = Places.createClient(this);
        mToken = AutocompleteSessionToken.newInstance();
        mSearchBar = findViewById(R.id.search_bar);
        searchBarActionListener();
        searchBarTextChangeListener();
        searchBarSuggestionClick();
        mPlaceInfoList = new ArrayList<>();
        mBottomSheetLayout = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetLayout);
        mHeaderArrow = findViewById(R.id.header_arrow);
        headerImageClickListener();
        bottomSheetBehaviourCallback();
        mPlaceName = findViewById(R.id.place_name_tv);
        mPlaceAddress = findViewById(R.id.place_address_tv);
        mBusinessStatus = findViewById(R.id.business_status_tv);
        mIsOpen = findViewById(R.id.is_open_tv);
    }

    private void searchBarActionListener() {
        mSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                startSearch(text.toString(), true, null, true);
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                if (buttonCode == MaterialSearchBar.BUTTON_NAVIGATION) {
                    // implement navigation drawer or something
                } else if (buttonCode == MaterialSearchBar.BUTTON_BACK) {
                    mSearchBar.clearSuggestions();
                    mSearchBar.closeSearch();
                }
            }
        });
    }

    private void searchBarTextChangeListener() {
        mSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                        .setCountries("ng", "gh", "bj")
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setSessionToken(mToken)
                        .setQuery(s.toString())
                        .build();
                mPlacesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(
                        new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                            @Override
                            public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                                if (task.isSuccessful()) {
                                    FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                                    if (predictionsResponse != null) {
                                        mPredictionList = predictionsResponse.getAutocompletePredictions();
                                        List<String> suggestionList = new ArrayList<>();
                                        for (int i = 0; i < mPredictionList.size(); i++) {
                                            AutocompletePrediction prediction = mPredictionList.get(i);
                                            suggestionList.add(prediction.getFullText(null).toString());
                                        }
                                        mSearchBar.updateLastSuggestions(suggestionList);
                                        if (!mSearchBar.isSuggestionsVisible()) {
                                            mSearchBar.showSuggestionsList();
                                        }
                                    }
                                } else {
                                    Log.d(TAG, "prediction fetching unsuccessfull");
                                }
                            }
                        });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void searchBarSuggestionClick() {
        mSearchBar.setSuggestionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
            @Override
            public void OnItemClickListener(int position, View v) {
                if (position >= mPredictionList.size())
                    return;
                AutocompletePrediction selectedPrediction = mPredictionList.get(position);
                String suggestion = mSearchBar.getLastSuggestions().get(position).toString();
                mSearchBar.setText(suggestion);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSearchBar.clearSuggestions();
                    }
                }, 2000);
                closeSoftKeyBoardForSearchBar();
                String placeId = selectedPrediction.getPlaceId();
                List<Place.Field> fieldList = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG);
                FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeId, fieldList).build();
                mPlacesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                        Place place = fetchPlaceResponse.getPlace();
                        Log.d(TAG, "Place Found: " + place.getName());
                        moveCamera(new LatLng(place.getLatLng().latitude, place.getLatLng().longitude),
                                DEFAULT_ZOOM, place.getName());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            apiException.printStackTrace();
                            int statusCode = apiException.getStatusCode();
                            Log.d(TAG, "Place Not Found " + e.getMessage());
                            Log.d(TAG, "Place Not Found " + statusCode);
                        }
                    }
                });
            }

            @Override
            public void OnItemDeleteListener(int position, View v) {

            }
        });
    }

    private void closeSoftKeyBoardForSearchBar() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(mSearchBar.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(MapsActivity.this, "Map is ready", Toast.LENGTH_SHORT).show();
        mGoogleMap = googleMap;
        if (mLocationPermissionGranted) {
            checkIfGPSIsEnabled();
            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
            moveLocationButtonLower();
            mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    if (mSearchBar.isSuggestionsVisible()) {
                        mSearchBar.clearSuggestions();
                        mSearchBar.closeSearch();
                    }
                    if (mSearchBar.isSearchOpened())
                        mSearchBar.closeSearch();
                }
            });
            mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    getDeviceLocation();
                    if (mSearchBar.isSuggestionsVisible()) {
                        mSearchBar.clearSuggestions();
                        mSearchBar.closeSearch();
                    }
                    if (mSearchBar.isSearchOpened()) {
                        mSearchBar.closeSearch();
                    }
                    return false;
                }
            });
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
    }

    private boolean getPlaceDetails(LatLng position) {
        for (PlaceInfo placeInfo : mPlaceInfoList) {
            LatLng place = new LatLng(placeInfo.getGeometry().getLocation().getLat(),
                    placeInfo.getGeometry().getLocation().getLng());
            if (place.equals(position)) {
                mPlaceName.setText(placeInfo.getName());
                mPlaceAddress.setText(placeInfo.getVicinity());
                mBusinessStatus.setText(placeInfo.getBusiness_status());
                if (placeInfo.getOpeningHours().getOpen_now()) {
                    mIsOpen.setText(R.string.open);
                    mIsOpen.setTextColor(getResources().getColor(R.color.positiveGreen));
                } else {
                    mIsOpen.setText(R.string.closed);
                    mIsOpen.setTextColor(getResources().getColor(R.color.negativeRed));
                }
                return true;
            }
        }
        return false;
    }

    private void moveLocationButtonLower() {
        if (mMapView != null && mMapView.findViewById(Integer.parseInt("1")) != null) {
            View locationButton = ((View) mMapView.findViewById(Integer.parseInt("1")).getParent())
                    .findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.setMargins(0, 140, 40, 0);
        }
    }

    private void initializeMap() {
        Log.d(TAG, "initializeMap");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapView = mapFragment.getView();
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
                            Log.d(TAG, "onComplete: found location");
                            mCurrentLocation = (Location) task.getResult();
                            if (mCurrentLocation != null) {
                                moveCamera(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),
                                        DEFAULT_ZOOM, "My Position");
                                getNearByHospitals(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                                Log.d(TAG, "onComplete: " + "lat: " + mCurrentLocation.getLatitude() +
                                        " lng: " + mCurrentLocation.getLongitude());
                            } else {
                                requestNewLocation();
                            }
                        } else {
                            Log.d(TAG, "onComplete: location not found");
                            Toast.makeText(MapsActivity.this, "Unable to find location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "get device current location. SecurityException:" + e.getMessage());
        }
    }

    private void requestNewLocation() {
        final LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        final LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null)
                    return;
                mCurrentLocation = locationResult.getLastLocation();
                moveCamera(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), DEFAULT_ZOOM, "My Position");
                getNearByHospitals(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                Log.d(TAG, "requestNewLocation: " + mCurrentLocation.getLatitude() + " " +
                        mCurrentLocation.getLongitude());
            }
        };
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving camera to: " + latLng.latitude + " " + latLng.longitude);
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if (!title.equals("My Position")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mGoogleMap.addMarker(options);
        }
    }

    private void checkIfGPSIsEnabled() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addAllLocationRequests(Collections.singleton(locationRequest));

        SettingsClient settingsClient = LocationServices.getSettingsClient(MapsActivity.this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(MapsActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                getDeviceLocation();
            }
        });
        task.addOnFailureListener(MapsActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                    try {
                        resolvableApiException.startResolutionForResult(MapsActivity.this, GPS_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException ex) {
                        Log.d(TAG, "check GPS: " + ex.getMessage());
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                getDeviceLocation();
            } else {
                Log.d(TAG, "user ignored GPS alert");
                Toast.makeText(MapsActivity.this, "Keep your GPS enabled", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void getLocationPermission() {
        Log.d(TAG, "getting permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initializeMap();
            } else {
                Log.d(TAG, "getting first permissions failed");
                ActivityCompat.requestPermissions(this, permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            Log.d(TAG, "getting second permissions failed");
            ActivityCompat.requestPermissions(this, permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult called !");
        mLocationPermissionGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "onRequestPermissionsResult failed");
                            mLocationPermissionGranted = false;
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult granted");
                    mLocationPermissionGranted = true;
                    initializeMap();
                }
        }
    }

    /*-----------------------  get nearby places data -----------------------*/
    public static class GetNearByPlacesData extends AsyncTask<Object, String, String> {
        String placesData;
        GoogleMap gMap;
        String url;
        private LatLng mLatLng;
        private MarkerOptions mMarkerOptions;

        @Override
        protected String doInBackground(Object... objects) {
            Log.d(TAG, "doInBackground");
            gMap = (GoogleMap) objects[0];
            url = (String) objects[1];
            DownloadPlacesFromUrl downloadPlaces = new DownloadPlacesFromUrl();
            try {
                placesData = downloadPlaces.readUrl(url);
                Log.i(TAG, "doInBackground: " + placesData);
            } catch (IOException e) {
                Log.d(TAG, "doInBackground: " + e.getMessage());
            }
            return placesData;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d(TAG, "on Post Execute");
            Gson gson = new Gson();
            PlaceInfoResponse response = gson.fromJson(s, PlaceInfoResponse.class);
            Log.d(TAG, "on Post Execute " + response.getResults().size());
            mPlaceInfoList = response.getResults();
            for (int i = 0; i < mPlaceInfoList.size(); i++) {
                mLatLng = new LatLng(mPlaceInfoList.get(i).getGeometry().getLocation().getLat(),
                        mPlaceInfoList.get(i).getGeometry().getLocation().getLng());
                mMarkerOptions = new MarkerOptions()
                        .title(mPlaceInfoList.get(i).getName())
                        .position(mLatLng);
                gMap.addMarker(mMarkerOptions);
                Log.d(TAG, "on Post Execute " + mLatLng.toString());
            }
        }
    }

    private void getNearByHospitals(double latitude, double longitude) {
        Log.d(TAG, "getNearByHospitals");
        String url = NEAR_BY_SEARCH_URL + "location=" + latitude + "," + longitude +
                "&radius=1000" +
                "&type=hospital" +
                "&key=" + BuildConfig.MAPS_API_KEY;
        Log.d(TAG, "getNearByHospitals: " + url);
        Object[] data = new Object[2];
        data[0] = mGoogleMap;
        data[1] = url;

        GetNearByPlacesData getNearByPlacesData = new GetNearByPlacesData();
        getNearByPlacesData.execute(data);
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