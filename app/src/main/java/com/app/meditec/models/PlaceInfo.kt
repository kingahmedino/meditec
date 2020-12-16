package com.app.meditec.models

import com.google.android.gms.maps.model.LatLng

class PlaceInfo(var name: String, var placeId: String, var address: String, var latLng: LatLng,
                var businessStatus: String, var isOpenNow: Boolean)
