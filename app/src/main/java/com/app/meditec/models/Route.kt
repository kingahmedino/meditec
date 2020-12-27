package com.app.meditec.models

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

data class Route(
        val bounds: Bounds,
        val copyrights: String?,
        val warnings: List<String>?,
        @SerializedName("overview_polyline") private val _overviewPolyLine: PolyLine,
        val legs: List<Leg>
){
    val polyLines: List<LatLng>
        get() = decodePolyLine(_overviewPolyLine.points)!!

    private fun decodePolyLine(poly: String): List<LatLng>? {
        val len = poly.length
        var index = 0
        val decoded: MutableList<LatLng> = ArrayList()
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = poly[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dLat
            shift = 0
            result = 0
            do {
                b = poly[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dLng
            decoded.add(LatLng(
                    lat / 100000.0, lng / 100000.0
            ))
        }
        return decoded
    }
}

data class Leg(
        val distance: Distance,
        val duration: Duration,
        @SerializedName("end_address") val endAddress: String,
        @SerializedName("end_location") private val _endLocation: LocalLocation,
        @SerializedName("start_address") val startAddress: String,
        @SerializedName("start_location") private val _startLocation: LocalLocation,
        val steps: List<Step>
){
    val endLocation: LatLng
        get() = LatLng(_endLocation.lat, _endLocation.lng)

    val startLocation: LatLng
        get() = LatLng(_startLocation.lat, _startLocation.lng)
}

data class Step(
        val distance: Distance,
        val duration: Duration,
        @SerializedName("end_location") private val _endLocation: LocalLocation,
        @SerializedName("html_instructions") val instructions: String,
        val maneuver: String?,
        @SerializedName("start_location") private val _startLocation: LocalLocation
){
    val endLocation: LatLng
        get() = LatLng(_endLocation.lat, _endLocation.lng)

    val startLocation: LatLng
        get() = LatLng(_startLocation.lat, _startLocation.lng)
}

data class PolyLine(
        val points: String
)

data class Bounds(
        private val _northeast: LocalLocation,
        private val _southwest: LocalLocation
){
    val northeast: LatLng
        get() = LatLng(_northeast.lat, _northeast.lng)

    val southwest: LatLng
        get() = LatLng(_southwest.lat, _southwest.lng)
}

data class Distance(
        val text: String,
        val value: Int
)

data class Duration(
        val text: String,
        val value: Int
)

data class LocalLocation(
        val lat: Double,
        val lng: Double
)