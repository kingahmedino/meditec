package com.app.meditec.utils

import android.content.IntentSender.SendIntentException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.app.meditec.PermissionUtilsListener
import com.app.meditec.MapsActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest

object PermissionUtils {
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
                mPermissionUtilsListener?.locationGranted()
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
}