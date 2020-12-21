package com.app.meditec.models

import com.google.gson.annotations.SerializedName

data class PlaceInfo(
        val name: String,
        val place_id: String,
        val vicinity: String,
        val business_status: String,
        val geometry: Geometry,
        @SerializedName("opening_hours")
        val opening_hours: OpeningHours
)

data class Geometry(
        val location : Location
)

data class Location(
        val lat : Double,
        val lng : Double
)

data class OpeningHours(
        val open_now: Boolean
)
