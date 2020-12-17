package com.app.meditec.helpers

import com.app.meditec.models.PlaceInfo
import com.google.gson.annotations.SerializedName

data class PlaceInfoResponse(
        @SerializedName("results")
        var results : List<PlaceInfo>
)