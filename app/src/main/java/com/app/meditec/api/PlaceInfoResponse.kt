package com.app.meditec.api

import com.app.meditec.models.PlaceInfo

data class PlaceInfoResponse(
        var results : List<PlaceInfo>
)