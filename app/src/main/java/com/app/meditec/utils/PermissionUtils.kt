package com.app.meditec.utils

import android.Manifest
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.meditec.MapsActivity
import com.app.meditec.PermissionUtilsListener
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest

object PermissionUtils {
    private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    private const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION

    private var mPermissionUtilsListener: PermissionUtilsListener? = null

    fun setListener(permissionUtilsListener: PermissionUtilsListener){
        mPermissionUtilsListener = permissionUtilsListener
    }

    fun checkIfGPSIsEnabled(activity: AppCompatActivity) {
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
                .addAllLocationRequests(setOf(locationRequest))
        val settingsClient = LocationServices.getSettingsClient(activity)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener(activity) { response ->
            val states = response.locationSettingsStates
            if (states.isLocationPresent){
                mPermissionUtilsListener?.GPSIsEnabled()
            }
        }

        task.addOnFailureListener(activity) { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(activity, MapsActivity.GPS_REQUEST_CODE)
                } catch (ex: SendIntentException) {
                    Log.d("PermissionUtils", "check GPS: " + ex.message)
                }
            }
        }
    }

    fun getLocationPermission(activity: AppCompatActivity): Boolean {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
        if (ContextCompat.checkSelfPermission(activity.applicationContext,
                        FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(activity.applicationContext,
                            COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true
            } else {
                ActivityCompat.requestPermissions(activity, permissions,
                        MapsActivity.LOCATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(activity, permissions,
                    MapsActivity.LOCATION_PERMISSION_REQUEST_CODE)
        }
        return false
    }
}