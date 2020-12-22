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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.meditec.BuildConfig
import com.app.meditec.R
import com.app.meditec.databinding.ActivityMapBinding
import com.app.meditec.databinding.PlaceInfoBottomSheetBinding
import com.app.meditec.models.PlaceInfo
import com.app.meditec.utils.PermissionUtils
import com.app.meditec.utils.PermissionUtilsListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, PermissionUtilsListener {
    private var mLocationPermissionGranted = false
    private var mLocationCallback: LocationCallback? = null
    private var mGoogleMap: GoogleMap? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var mCurrentLocation: Location? = null
    private var mPlaceInfoList: List<PlaceInfo>? = null
    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var mMapsViewModel: MapsViewModel
    private lateinit var mBinding: ActivityMapBinding
    private lateinit var mBottomSheetBinding: PlaceInfoBottomSheetBinding

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
        mPlaceInfoList = ArrayList()
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetBinding.bottomSheet)
        setClickListeners()
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
                    moveCamera(LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude))
                    mMapsViewModel.getPlaces(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
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
        mMapsViewModel.placesResponseStatus.observe(this, Observer { placeResponseStatus ->
            Toast.makeText(this, placeResponseStatus, Toast.LENGTH_SHORT).show()
        })
    }

    override fun GPSIsEnabled() {
        if (mLocationPermissionGranted) setupLocationUpdatesListener()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show()
        mGoogleMap = googleMap
        mGoogleMap!!.isMyLocationEnabled = true
        mGoogleMap!!.uiSettings.isMyLocationButtonEnabled = true
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
                if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                    mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                break
            }
        }
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun moveCamera(latLng: LatLng) {
        mGoogleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (mLocationPermissionGranted) setupLocationUpdatesListener()
            } else {
                Log.d(TAG, "user ignored GPS alert")
                Toast.makeText(this, "Keep your GPS enabled", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(this, "Location permissions needed to show maps", Toast.LENGTH_LONG).show()
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

    private fun setClickListeners() {
        mBottomSheetBinding.headerArrow.setOnClickListener {
            if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            else
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        }

    }

    companion object {
        private const val TAG = "MapsActivity"
        const val LOCATION_PERMISSION_REQUEST_CODE = 102
        const val DEFAULT_ZOOM = 15f
        const val GPS_REQUEST_CODE = 189
    }
}