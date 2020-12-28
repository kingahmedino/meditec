package com.app.meditec.ui.maps

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.meditec.BuildConfig
import com.app.meditec.R
import com.app.meditec.adapters.SearchPlacesAutoCompleteAdapter
import com.app.meditec.databinding.ActivityMapBinding
import com.app.meditec.databinding.PlaceInfoBottomSheetBinding
import com.app.meditec.models.PlaceInfo
import com.app.meditec.utils.PermissionUtils
import com.app.meditec.utils.PermissionUtilsListener
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, PermissionUtilsListener {
    private var isCameraMovedOnAppStart = false
    private var mLocationPermissionGranted = false
    private var mLocationCallback: LocationCallback? = null
    private var mGoogleMap: GoogleMap? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var mCurrentLocation: Location? = null
    private var mPlaceInfoList: List<PlaceInfo>? = null
    private var mPlacesClient: PlacesClient? = null
    private var mToken: AutocompleteSessionToken? = null
    private var mMapView: View? = null
    private var mCurrentlySelectedPlace: PlaceInfo? = null
    private var mCurrentPolyLine: Polyline? = null
    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var mMapsViewModel: MapsViewModel
    private lateinit var mBinding: ActivityMapBinding
    private lateinit var mBottomSheetBinding: PlaceInfoBottomSheetBinding
    private lateinit var mAutoCompleteListener: AdapterView.OnItemClickListener
    private lateinit var mSearchPlacesAutoCompleteAdapter: SearchPlacesAutoCompleteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_map)
        PermissionUtils.setListener(this)
        mMapsViewModel = ViewModelProvider(this).get(MapsViewModel::class.java)
        mBottomSheetBinding = mBinding.bottomSheet
        mLocationPermissionGranted = PermissionUtils.getLocationPermission(this)
        if (mLocationPermissionGranted) initializeMap()
        createLocationCallback()
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        mPlacesClient = Places.createClient(this)
        mToken = AutocompleteSessionToken.newInstance()
        mSearchPlacesAutoCompleteAdapter = SearchPlacesAutoCompleteAdapter(this, mPlacesClient!!, mToken!!)
        mPlaceInfoList = ArrayList()
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetBinding.bottomSheet)
        initViews()
        bottomSheetBehaviourCallback()
    }

    override fun onStart() {
        super.onStart()
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (mLocationPermissionGranted) PermissionUtils.checkIfGPSIsEnabled(this)
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationUpdatesListener() {
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 10000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationProviderClient!!.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper())
    }

    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                if (locationResult != null) {
                    mCurrentLocation = locationResult.lastLocation
                    mMapsViewModel.getPlaces(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
                    if (!isCameraMovedOnAppStart) {
                        isCameraMovedOnAppStart = true
                        moveCamera(LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mMapsViewModel.placesLiveData.observe(this, Observer { placeInfos ->
            mPlaceInfoList = placeInfos
            for ((name, _, _, _, geometry) in mPlaceInfoList!!) {
                val latLng = LatLng(geometry.location.lat, geometry.location.lng)
                val markerOptions = MarkerOptions()
                        .title(name)
                        .position(latLng)
                mGoogleMap!!.addMarker(markerOptions)
            }
        })
        mMapsViewModel.routeLiveData.observe(this, Observer { routes ->
            for (route in routes){
                val polylineOptions = PolylineOptions()
                polylineOptions.color(resources.getColor(R.color.colorAccent))
                polylineOptions.width(7f)
                polylineOptions.addAll(route.polyLines)
                mCurrentPolyLine = mGoogleMap!!.addPolyline(polylineOptions)
            }
        })
        mMapsViewModel.responseStatus.observe(this, Observer { placeResponseStatus ->
            showToast(placeResponseStatus)
        })
    }

    override fun GPSIsEnabled() {
        if (mLocationPermissionGranted) setupLocationUpdatesListener()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        showToast("Map is ready")
        mGoogleMap = googleMap
        mGoogleMap!!.isMyLocationEnabled = true
        mGoogleMap!!.uiSettings.isMyLocationButtonEnabled = true
        moveLocationButtonLower()
        mGoogleMap!!.setOnMarkerClickListener { marker: Marker ->
            showPlaceDetails(marker.position)
            false
        }
    }

    override fun onStop() {
        super.onStop()
        if (mFusedLocationProviderClient != null) mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
    }

    private fun showPlaceDetails(position: LatLng) {
        for (placeInfo in mPlaceInfoList!!) {
            val place = LatLng(placeInfo.geometry.location.lat,
                    placeInfo.geometry.location.lng)
            if (place == position) {
                mBottomSheetBinding.placeInfo = placeInfo
                mCurrentlySelectedPlace = placeInfo
                if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                    mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                break
            }
        }
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mMapView = mapFragment!!.requireView()
        mapFragment.getMapAsync(this)
    }

    private fun moveLocationButtonLower() {
        if (mMapView != null && mMapView!!.findViewById<View>("1".toInt()) != null) {
            val locationButton = (mMapView!!.findViewById<View>("1".toInt()).parent as View)
                    .findViewById<View>("2".toInt())
            val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            layoutParams.setMargins(0, 140, 40, 0)
        }
    }

    private fun moveCamera(latLng: LatLng) {
        mGoogleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
    }

    private fun moveCameraAndAddMarker(latLng: LatLng, title: String) {
        mGoogleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
        val options = MarkerOptions()
                .position(latLng)
                .title(title)
        mGoogleMap!!.addMarker(options)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (mLocationPermissionGranted) setupLocationUpdatesListener()
            } else {
                Log.d(TAG, "user ignored GPS alert")
                showToast("Keep your GPS enabled")
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        mLocationPermissionGranted = false
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                for (grantResult in grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        showToast("Location permissions needed to show maps")
                        mLocationPermissionGranted = false
                        finish()
                        return
                    }
                }
                mLocationPermissionGranted = true
                initializeMap()
                PermissionUtils.checkIfGPSIsEnabled(this)
            }
        }
    }

    private fun bottomSheetBehaviourCallback() {
        mBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                mBottomSheetBinding.headerArrow.rotation = slideOffset * 180
            }
        })
    }

    private fun initViews() {
        mBottomSheetBinding.headerArrow.setOnClickListener {
            if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            else
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        }
        mBinding.autoCompleteTextView.setAdapter(mSearchPlacesAutoCompleteAdapter)

        mAutoCompleteListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val item = mSearchPlacesAutoCompleteAdapter.getItem(position)
            val placeId = item?.placeId
            getPlaceWith(placeId)
        }

        mBinding.autoCompleteTextView.onItemClickListener = mAutoCompleteListener

        mBottomSheetBinding.button.setOnClickListener {
            mCurrentPolyLine?.remove()
            val latLng = LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
            mMapsViewModel.getDirections(latLng, mCurrentlySelectedPlace!!.place_id)
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun getPlaceWith(placeId: String?) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.builder(placeId!!, placeFields).build()

        mPlacesClient!!.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            moveCameraAndAddMarker(place.latLng!!, place.name!!)
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                showToast("An error occurred, place not found")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "MapsActivity"
        const val LOCATION_PERMISSION_REQUEST_CODE = 102
        const val DEFAULT_ZOOM = 15f
        const val GPS_REQUEST_CODE = 189
    }
}