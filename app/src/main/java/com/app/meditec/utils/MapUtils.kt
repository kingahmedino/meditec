package com.app.meditec.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.app.meditec.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

object MapUtils {

//    private fun getCarBitmap(context: Context): BitmapDescriptor {
//        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_pin)
//        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
//        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
//    }

    fun createMarkerOptions(title: String, latLng: LatLng): MarkerOptions {
        return MarkerOptions()
                .title(title)
                .position(latLng)
    }
}